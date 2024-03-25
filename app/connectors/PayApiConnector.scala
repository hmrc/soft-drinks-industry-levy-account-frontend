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
    http.POST[SetupPayApiRequest, NextUrl](config.payApiUrl, generateRequestForPayApi(balance, sdilRef, dueDate))
      .map(Right(_))
      .recover{
        case _ =>
          genericLogger.logger.error(s"[PayApiConnector][initJourney] - unexpected response")
          Left(UnexpectedResponseFromPayAPI)
      }
  }

  private def generateRequestForPayApi(balance: BigDecimal, sdilRef: String, dueDate: LocalDate): SetupPayApiRequest = {
    val balanceInPence = balance * 100
    val amountOwed = balanceInPence * -1
    val exactAmountOwed = amountOwed.toLongExact
    println(Console.MAGENTA + "getting into geenrate requeest for pay api before attempting due date: " + Console.RESET)
    val theDueDate = dueDate
    println(Console.MAGENTA + "in generate requeest for pay api after attempting due date: " + Console.RESET)

    SetupPayApiRequest(
      sdilRef,
      exactAmountOwed,
      Some(theDueDate),
      config.homePage,
      config.homePage
    )
  }


}
