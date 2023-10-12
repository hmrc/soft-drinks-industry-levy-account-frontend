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
import com.google.inject.Inject
import config.FrontendAppConfig
import errors.UnexpectedResponseFromDirectDebit
import models.{NextUrl, SetupDirectDebitRequest}
import service.AccountResult
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utilities.GenericLogger
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.ExecutionContext

class DirectDebitConnector @Inject()(val http: HttpClient,
                                     config: FrontendAppConfig,
                                     genericLogger: GenericLogger
                                   )(implicit ec: ExecutionContext) {



  def initJourney()(implicit hc: HeaderCarrier): AccountResult[NextUrl] = EitherT {
    http.POST[SetupDirectDebitRequest, NextUrl](config.directDebitUrl, new SetupDirectDebitRequest(config))
      .map(Right(_))
      .recover{
        case _ =>
          genericLogger.logger.error(s"[DirectDebitConnector][initJourney] - unexpected response")
          Left(UnexpectedResponseFromDirectDebit)
      }
  }


}
