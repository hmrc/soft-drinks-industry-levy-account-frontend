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
import models.{NextUrl, SetupPayApiRequest}
import service.AccountResult
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utilities.GenericLogger
import uk.gov.hmrc.http.HttpReads.Implicits._

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class PayApiConnector @Inject()(val http: HttpClient,
                                config: FrontendAppConfig,
                                genericLogger: GenericLogger
                                   )(implicit ec: ExecutionContext) {



  def initJourney(sdilRef: String, balance: BigDecimal, dueDate: LocalDate, amount: BigDecimal)
                 (implicit hc: HeaderCarrier): AccountResult[NextUrl] = EitherT {
    println(Console.GREEN + "balance and due date are .............: " + Console.RESET)
    println(Console.BLUE + "Here they are...Balance " + balance + Console.RESET)
    println(Console.MAGENTA + "Here they are...due date " + dueDate + Console.RESET)
    println(Console.YELLOW + "Here they are...amount " + amount + Console.RESET)
    http.POST[SetupPayApiRequest, NextUrl](config.payApiUrl, generateRequestForPayApi(balance, sdilRef, dueDate, amount))
      .map(Right(_))
      .recover{
        case _ =>
          genericLogger.logger.error(s"[PayApiConnector][initJourney] - unexpected response")
          Left(UnexpectedResponseFromPayAPI)
      }
  }

  private def generateRequestForPayApi(balance: BigDecimal, sdilRef: String, dueDate: LocalDate, amount: BigDecimal): SetupPayApiRequest = {
    val balanceInPence = balance * 100
    val amountOwed = balanceInPence * -1
    val exactAmountOwed = amountOwed.toLongExact
    val dueDateToSendToApi = generateDueDate(dueDate, amount, balance)
    println(Console.GREEN + "theDueDate is  .............: " + dueDateToSendToApi + Console.RESET)



    SetupPayApiRequest(
      sdilRef,
      exactAmountOwed,
      dueDateToSendToApi,
      config.homePage,
      config.homePage
    )
  }

  private def generateDueDate(dueDate: LocalDate, amount: BigDecimal, balance: BigDecimal): Option[LocalDate] = {

    if (dueDate.isAfter(LocalDate.now().minusYears(6))) {
      if (balance - amount >= 0) {
        println(Console.GREEN + "balance and amount  .............: " + balance + amount + " the balance is " + theBalanceIs + Console.RESET)
        Some(dueDate)
      } else {
        None
      }
    } else {
      None
    }
  }


}
