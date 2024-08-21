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

package connectors

import cats.data.EitherT
import config.FrontendAppConfig
import errors.UnexpectedResponseFromSDIL
import models.FinancialLineItem.formatter
import models._
import repositories.{SessionCache, SessionKeys}
import service.AccountResult
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utilities.GenericLogger

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SoftDrinksIndustryLevyConnector @Inject()(
                                                 val http: HttpClientV2,
                                                 frontendAppConfig: FrontendAppConfig,
                                                 sdilSessionCache: SessionCache,
                                                 genericLogger: GenericLogger
                                               )(implicit ec: ExecutionContext) {

  lazy val sdilUrl: String = frontendAppConfig.sdilBaseUrl

  private def getSubscriptionUrl(identifierValue: String, identifierType: String): String = s"$sdilUrl/subscription/$identifierType/$identifierValue"

  def retrieveSubscription(identifierValue: String, identifierType: String, internalId: String)
                          (implicit hc: HeaderCarrier): AccountResult[Option[RetrievedSubscription]] = EitherT {
    sdilSessionCache.fetchEntry[OptRetrievedSubscription](internalId, SessionKeys.SUBSCRIPTION).flatMap {
      case Some(optSubscription) => Future.successful(Right(optSubscription.optRetrievedSubscription))
      case None =>
        http.get(url"${getSubscriptionUrl(identifierValue: String, identifierType)}")
          .execute[Option[RetrievedSubscription]]
          .flatMap {
            optRetrievedSubscription =>
              sdilSessionCache.save(internalId, SessionKeys.SUBSCRIPTION, OptRetrievedSubscription(optRetrievedSubscription))
                .map { _ =>
                  Right(optRetrievedSubscription)
                }

          }.recover {
          case _ =>
            genericLogger.logger.error(s"[SoftDrinksIndustryLevyConnector][retrieveSubscription] - unexpected response for $internalId")
            Left(UnexpectedResponseFromSDIL)
        }
    }
  }

  def returns_pending(internalId: String,
                      utr: String)
                     (implicit hc: HeaderCarrier): AccountResult[List[ReturnPeriod]] = EitherT {
    sdilSessionCache.fetchEntry[List[ReturnPeriod]](internalId, SessionKeys.pendingReturn(utr)).flatMap {
      case Some(pendingReturns) => Future.successful(Right(pendingReturns))
      case None =>
        val pendingUrl = s"$sdilUrl/returns/$utr/pending"
        http.get(url"$pendingUrl")
          .execute[List[ReturnPeriod]]
          .flatMap { pendingReturns =>
            sdilSessionCache.save[List[ReturnPeriod]](internalId, SessionKeys.pendingReturn(utr), pendingReturns)
              .map(_ => Right(pendingReturns))
          }.recover {
          case _ => genericLogger.logger.error(s"[SoftDrinksIndustryLevyConnector][returns_pending] - unexpected response for $internalId")
            Left(UnexpectedResponseFromSDIL)
        }
    }
  }

  def returns_variable(internalId: String, utr: String)
                      (implicit hc: HeaderCarrier): AccountResult[List[ReturnPeriod]] = EitherT {
    sdilSessionCache.fetchEntry[List[ReturnPeriod]](internalId, SessionKeys.variableReturn(utr)).flatMap {
      case Some(pendingReturns) => Future.successful(Right(pendingReturns))
      case None =>
        val variableUrl = s"$sdilUrl/returns/$utr/variable"
        http.get(url"$variableUrl")
          .execute[List[ReturnPeriod]]
          .flatMap { pendingReturns =>
            sdilSessionCache.save[List[ReturnPeriod]](internalId, SessionKeys.variableReturn(utr), pendingReturns)
              .map(_ => Right(pendingReturns))
          }.recover {
          case _ => genericLogger.logger.error(s"[SoftDrinksIndustryLevyConnector][returns_variable] - unexpected response for $internalId")
            Left(UnexpectedResponseFromSDIL)
        }
    }
  }

  def returns_get(utr: String,
                   period: ReturnPeriod,
                   internalId: String
                 )(implicit hc: HeaderCarrier): AccountResult[Option[SdilReturn]] = EitherT {
    sdilSessionCache.fetchEntry[OptPreviousSubmittedReturn](internalId, SessionKeys.previousSubmittedReturn(utr, period)).flatMap {
      case Some(optPreviousReturn) => Future.successful(Right(optPreviousReturn.optReturn))
      case None =>
        val getReturnUrl = s"$sdilUrl/returns/$utr/year/${period.year}/quarter/${period.quarter}"
        http.get(url"$getReturnUrl")
          .execute[Option[SdilReturn]]
          .flatMap { optReturn =>
            sdilSessionCache.save[OptPreviousSubmittedReturn](internalId,
              SessionKeys.previousSubmittedReturn(utr, period), OptPreviousSubmittedReturn(optReturn))
              .map(_ => Right(optReturn))
          }.recover {
          case _ => genericLogger.logger.error(s"[SoftDrinksIndustryLevyConnector][returns_get] - unexpected response for $internalId")
            Left(UnexpectedResponseFromSDIL)
        }
    }
  }

  def balance(
               sdilRef: String,
               withAssessment: Boolean, internalId: String
             )(implicit hc: HeaderCarrier): AccountResult[BigDecimal] = EitherT {
    sdilSessionCache.fetchEntry[BigDecimal](internalId, SessionKeys.balance(withAssessment)).flatMap {
      case Some(b) =>
        Future.successful(Right(b))
      case None =>
        val balanceUrl = s"$sdilUrl/balance/$sdilRef/$withAssessment"
        http.get(url"$balanceUrl")
          .execute[BigDecimal]
          .flatMap { b =>
            sdilSessionCache.save[BigDecimal](internalId, SessionKeys.balance(withAssessment), b)
              .map(_ => Right(b))

          }.recover {
          case _ => genericLogger.logger.error(s"[SoftDrinksIndustryLevyConnector][balance] - unexpected response for $sdilRef")
            Left(UnexpectedResponseFromSDIL)
        }
    }
  }

  def balanceHistory(
                      sdilRef: String,
                      withAssessment: Boolean, internalId: String
                    )(implicit hc: HeaderCarrier): AccountResult[List[FinancialLineItem]] = EitherT {
    sdilSessionCache.fetchEntry[List[FinancialLineItem]](internalId, SessionKeys.balanceHistory(withAssessment)).flatMap {
      case Some(b) => Future.successful(Right(b))
      case None =>
        val balanceHistoryUrl = s"$sdilUrl/balance/$sdilRef/history/all/$withAssessment"
        http.get(url"$balanceHistoryUrl")
          .execute[List[FinancialLineItem]]
          .flatMap { bh =>
            sdilSessionCache.save[List[FinancialLineItem]](internalId, SessionKeys.balanceHistory(withAssessment), bh)
              .map(_ => Right(bh))

          }.recover {
          case _ => genericLogger.logger.error(s"[SoftDrinksIndustryLevyConnector][balanceHistory] - unexpected response for $sdilRef")
            Left(UnexpectedResponseFromSDIL)
        }
    }
  }

  def checkDirectDebitStatus(sdilRef: String)(implicit hc: HeaderCarrier): AccountResult[Boolean] = EitherT {
    val ddStatusUrl = s"$sdilUrl/check-direct-debit-status/$sdilRef"
    http.get(url"$ddStatusUrl")
      .execute[DisplayDirectDebitResponse]
      .map { result =>
        Right(result.directDebitMandateFound)
      }.recover {
      case _ => genericLogger.logger.error(s"[SoftDrinksIndustryLevyConnector][checkDirectDebitStatus] - unexpected response for $sdilRef")
        Left(UnexpectedResponseFromSDIL)
    }
  }
}
