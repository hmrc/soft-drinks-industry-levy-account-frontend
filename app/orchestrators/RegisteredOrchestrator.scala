/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package orchestrators

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import connectors.SoftDrinksIndustryLevyConnector
import errors.{AccountErrors, NoPendingReturns}
import models.requests.RegisteredRequest
import models._
import play.api.mvc.AnyContent
import repositories.SessionCache
import service.AccountResult
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegisteredOrchestrator @Inject()(sdilConnector: SoftDrinksIndustryLevyConnector,
                                       sessionCache: SessionCache,
                                       frontendAppConfig: FrontendAppConfig) {

  def handleServicePageRequest(implicit request: RegisteredRequest[AnyContent],
                               hc: HeaderCarrier,
                               ec: ExecutionContext): AccountResult[ServicePageViewModel] = {
    val subscription = request.subscription
    subscription.deregDate match {
      case Some(deregDate) => getServiceViewModelForDeregisteredUser(subscription, deregDate)
      case None => getServiceViewModelForRegisteredUser(subscription)
    }
  }

  def getTransactionHistoryForAllYears(implicit request: RegisteredRequest[AnyContent],
                                      hc: HeaderCarrier,
                                      ec: ExecutionContext): AccountResult[Map[Int, List[TransactionHistoryItem]]] = {
    sdilConnector.balanceHistory(request.subscription.sdilRef, true, request.internalId)
      .map {
        convertBalanceHistoryToTransactionHistory
      }
  }

  def handleStartAReturn(implicit request: RegisteredRequest[AnyContent],
                         hc: HeaderCarrier,
                         ec: ExecutionContext): AccountResult[ReturnPeriod] = EitherT {
    val internalId = request.internalId
    val utr = request.subscription.utr
    sdilConnector.returns_pending(internalId, utr).value.flatMap {
      case Right(pendingReturns) if pendingReturns.nonEmpty =>
        val sortedReturnsPending = pendingReturns.sortBy(_.start)
        sessionCache.removeRecord(internalId)
          .map(_ => Right(sortedReturnsPending.head))
      case Right(_) => Future.successful(Left(NoPendingReturns))
      case Left(error) => Future.successful(Left(error))
    }
  }

  def emptyCache(implicit request: RegisteredRequest[AnyContent]): Future[Boolean] = {
    val internalId = request.internalId
    sessionCache.removeRecord(internalId)
  }

  private def getServiceViewModelForRegisteredUser(subscription: RetrievedSubscription)
                                                  (implicit request: RegisteredRequest[AnyContent],
                                                   hc: HeaderCarrier,
                                                   ec: ExecutionContext): AccountResult[ServicePageViewModel] = {
    val internalId = request.internalId
    val utr = subscription.utr
    val lastReturnPeriod = ReturnPeriod(LocalDate.now).previous
    val getPendingReturns = sdilConnector.returns_pending(internalId, utr).map(_.sortBy(_.start))
    val getOptLastReturn = sdilConnector.returns_get(utr, lastReturnPeriod, internalId)
    val getBalance = sdilConnector.balance(subscription.sdilRef, true, internalId)
    val getInterest = getAndCalculateInterestIfReq(internalId)
    val optHasDDSetup = checkExistingDDIfEnabled

    for {
      returnsPending <- getPendingReturns
      optLastReturn <- getOptLastReturn
      balance <- getBalance
      interest <- getInterest
      hasExistingDD <- optHasDDSetup
    } yield {
      RegisteredUserServicePageViewModel(
        returnsPending,
        request.subscription,
        optLastReturn,
        balance,
        interest,
        hasExistingDD)
    }
  }

  private def getServiceViewModelForDeregisteredUser(subscription: RetrievedSubscription, deregDate: LocalDate)
                                                    (implicit request: RegisteredRequest[AnyContent],
                                                     hc: HeaderCarrier,
                                                     ec: ExecutionContext): AccountResult[ServicePageViewModel] = {
    val internalId = request.internalId
    val utr = subscription.utr
    val lastReturnPeriod = ReturnPeriod(LocalDate.now).previous
    val deregReturnPeriod = ReturnPeriod(deregDate)
    val checkIfHasVariableReturns = sdilConnector.returns_variable(internalId, utr).map(_.nonEmpty)
    val getOptLastReturn = sdilConnector.returns_get(utr, lastReturnPeriod, internalId)
    val getOptDeRegReturn = sdilConnector.returns_get(utr, deregReturnPeriod, internalId)
    val getBalance = sdilConnector.balance(subscription.sdilRef, true, internalId)

    for {
      optLastReturn <- getOptLastReturn
      hasVariableReturns <- checkIfHasVariableReturns
      balance <- getBalance
      optDeregReturn <- getOptDeRegReturn
    } yield {
      DeregisteredUserServicePageViewModel(
        subscription,
        deregDate,
        hasVariableReturns,
        optLastReturn,
        balance,
        optDeregReturn.isEmpty
      )
    }
  }

  private def getAndCalculateInterestIfReq(internalId: String)(implicit request: RegisteredRequest[AnyContent],
                                  hc: HeaderCarrier,
                                  ec: ExecutionContext): AccountResult[BigDecimal] =
    sdilConnector.balanceHistory(request.subscription.sdilRef, true, internalId)
    .map(items =>
      items.distinct.collect {
        case a: Interest => a.amount
      }.sum
    )

  private def checkExistingDDIfEnabled(implicit request: RegisteredRequest[AnyContent],
                                                               hc: HeaderCarrier,
                                                               ec: ExecutionContext): AccountResult[Option[Boolean]] = {
    if(frontendAppConfig.directDebitEnabled) {
      sdilConnector.checkDirectDebitStatus(request.subscription.sdilRef).map(Some(_))
    } else {
      EitherT.right[AccountErrors](Future.successful[Option[Boolean]](None))
    }
  }

  private def convertBalanceHistoryToTransactionHistory(balanceHistory: List[FinancialLineItem]): Map[Int, List[TransactionHistoryItem]] = {
    val transactionHistoryItem = balanceHistory.distinct.sortBy(_.date).foldLeft(List.empty[TransactionHistoryItem]){(transactionHistory, finicialListItem) =>
      List(new TransactionHistoryItem(finicialListItem, transactionHistory)) ++ transactionHistory
    }

    transactionHistoryItem.foldLeft(Map.empty[Int, List[TransactionHistoryItem]]){
      (transactionHistoryForYears, transactionHistoryItem) =>
        val transactionYear = transactionHistoryItem.financialLineItem.date.getYear
        val updatedTransactionItemsForYear = transactionHistoryForYears.get(transactionYear)
        .fold(List(transactionHistoryItem))(_ ++ List(transactionHistoryItem))
        transactionHistoryForYears ++ Map(transactionYear -> updatedTransactionItemsForYear)
    }
  }

}
