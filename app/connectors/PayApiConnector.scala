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
import errors.UnexpectedResponseFromPayAPI
import models.{NextUrl, ReturnPeriod, SdilReturn, SetupPayApiRequest}
import play.api.libs.json.Json
import service.AccountResult
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utilities.GenericLogger
import play.api.libs.ws.writeableOf_JsValue

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class PayApiConnector @Inject()(val http: HttpClientV2,
                                config: FrontendAppConfig,
                                genericLogger: GenericLogger
                                   )(implicit ec: ExecutionContext) {



  def initJourney(sdilRef: String, balance: BigDecimal, optLastReturn: Option[SdilReturn], amount: BigDecimal, todaysDate: LocalDate = LocalDate.now())
                 (implicit hc: HeaderCarrier): AccountResult[NextUrl] = EitherT {
    val payApiRequest = generateRequestForPayApi(balance, sdilRef, optLastReturn, amount, todaysDate: LocalDate)
    http.post(url"${config.payApiUrl}")
      .withBody(Json.toJson(payApiRequest))
      .execute[NextUrl]
      .map(Right(_))
      .recover {
        case _ =>
          genericLogger.logger.error(s"[DirectDebitConnector][initJourney] - unexpected response")
          Left(UnexpectedResponseFromPayAPI)
      }
  }

  private def generateRequestForPayApi(balance: BigDecimal, sdilRef: String, optLastReturn: Option[SdilReturn],
                                       priorReturnAmount: BigDecimal, todaysDate: LocalDate): SetupPayApiRequest = {
    val balanceInPence = balance * 100
    val amountOwed = balanceInPence * -1
    val exactAmountOwed = amountOwed.toLongExact
    val dueDateToSendToApi = generateDueDate(optLastReturn, priorReturnAmount, balance, todaysDate)

    SetupPayApiRequest(
      sdilRef,
      exactAmountOwed,
      dueDateToSendToApi,
      config.homePage,
      config.homePage
    )
  }

  private[connectors] def generateDueDate(optLastReturn: Option[SdilReturn], priorReturnAmount: BigDecimal, balance: BigDecimal,
                                          todaysDate: LocalDate): Option[LocalDate] = {
    val lastReturnPeriod = ReturnPeriod(todaysDate).previous
    val dueDate = if (optLastReturn.nonEmpty) {
      genericLogger.logger.info(s"[PayApiConnector][generateDueDate] - optLastReturn is not empty, due date of ${lastReturnPeriod.deadline} generated on return period end")
      Some(lastReturnPeriod.deadline)
    } else {
      genericLogger.logger.info(s"[PayApiConnector][generateDueDate] - no LastReturn found based on the previous return period, presumed overdue")
      None
    }

    dueDate match {
      case Some(dueDate) => if (dueDate.isAfter(todaysDate) && balance - priorReturnAmount >= 0) {
        genericLogger.logger.info(s"[PayApiConnector][generateDueDate] - due date is after today and balance is less than the prior return amount, " +
          s"presumed not overdue and future payment date can be offered")
        Some(dueDate)
      } else {
        genericLogger.logger.info(s"[PayApiConnector][generateDueDate] - No due date passed to API, may be due to overdue returns or " +
          s"balance is greater than the prior return amount")
        None
      }
      case None => None
    }
  }

}
