/*
 * Copyright 2026 HM Revenue & Customs
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

package services

import com.google.inject.Inject
import connectors.SoftDrinksIndustryLevyConnector
import errors.AccountErrors
import models.RetrievedSubscription
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class SdilSubscriptionService @Inject() (sdilConnector: SoftDrinksIndustryLevyConnector)(implicit
  ec: ExecutionContext
) {

  def isActive(sub: RetrievedSubscription): Boolean =
    sub.deregDate.isEmpty ||
      sub.deregDate.exists(_.isAfter(LocalDate.now))

  def resolveActiveSdilRef(sdilRefs: Seq[String], internalId: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val distinctRefs = sdilRefs.distinct

    Future.traverse(distinctRefs) { ref =>
      sdilConnector.retrieveSubscriptionNoCache(ref, "sdil").value.map {
        case Right(Some(sub)) => Right(Some((ref, isActive(sub))))
        case Right(None)      => Right(None)
        case Left(error)      => Left(error)
      }
    }.flatMap { results =>
      results.collectFirst { case Right(Some((ref, true))) => ref } match {
        case activeRef @ Some(_) => Future.successful(activeRef)
        case None if distinctRefs.size == 1 =>
          results.collectFirst { case Right(Some((ref, false))) => ref } match {
            case inactiveRef @ Some(_) => Future.successful(inactiveRef)
            case None                  => handleLookupFailure(results)
          }
        case None => handleLookupFailure(results)
      }
    }
  }

  private def handleLookupFailure(
    results: Seq[Either[AccountErrors, Option[(String, Boolean)]]]
  ): Future[Option[String]] =
    if (results.exists(_.isLeft)) {
      Future.failed(new IllegalStateException("Unable to resolve active SDIL reference"))
    } else {
      Future.successful(None)
    }
}
