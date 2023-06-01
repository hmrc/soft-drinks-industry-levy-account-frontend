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
import com.google.inject.{Inject, Singleton}
import connectors.SoftDrinksIndustryLevyConnector
import errors.NoPendingReturns
import models.requests.RegisteredRequest
import models.{ReturnPeriod, ServicePageViewModel}
import play.api.mvc.AnyContent
import repositories.SessionCache
import service.AccountResult
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegisteredOrchestrator @Inject()(sdilConnector: SoftDrinksIndustryLevyConnector,
                                       sessionCache: SessionCache) {

  def handleServicePageRequest(implicit request: RegisteredRequest[AnyContent],
                               hc: HeaderCarrier,
                               ec: ExecutionContext): AccountResult[ServicePageViewModel] = {
    val internalId = request.internalId
    val utr = request.subscription.utr
    val lastReturnPeriod = ReturnPeriod(LocalDate.now).previous
    val getPendingReturns = sdilConnector.returns_pending(internalId, utr)
    val getOptLastReturn = sdilConnector.returns_get(utr, lastReturnPeriod, internalId)
    for {
      returnsPending <- getPendingReturns
      optLastReturn <- getOptLastReturn
    } yield {
      val sortReturnsPending = returnsPending.sortBy(_.start)
      ServicePageViewModel(sortReturnsPending, request.subscription, optLastReturn)
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

}
