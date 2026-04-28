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

package Service

import com.google.inject.Inject
import connectors.SoftDrinksIndustryLevyConnector
import models.RetrievedSubscription
import uk.gov.hmrc.http.HeaderCarrier
import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class SdilSubscriptionService @Inject() (sdilConnector: SoftDrinksIndustryLevyConnector
                                        )(implicit ec: ExecutionContext){

  def isActive(sub: RetrievedSubscription): Boolean = {
    sub.deregDate.isEmpty ||
      sub.deregDate.exists(_.isAfter(LocalDate.now))
  }

  def resolveActiveSdilRef(sdilRefs: Seq[String], internalId: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    def loop(remaining: List[String]): Future[Option[String]] = remaining match {

      case head :: tail =>
        sdilConnector.retrieveSubscription(head,"sdil",internalId).value.flatMap {

          case Right(Some(sub)) if isActive(sub) =>
            Future.successful(Some(head))

          case Right(Some(_)) =>
            loop(tail) // expired -> skip

          case Right(None) =>
            Future.successful(Some(head)) // no subscription

          case Left(error) =>
            Future.failed(new Exception(s"SDIL subscription lookup failed: $error")) // error
        }
      case Nil =>
        Future.successful(None)
    }
    loop(sdilRefs.toList)
  }
}
