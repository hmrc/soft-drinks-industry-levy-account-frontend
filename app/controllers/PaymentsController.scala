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

package controllers

import cats.implicits._
import com.google.inject.Inject
import connectors.{PayApiConnector, SoftDrinksIndustryLevyConnector}
import controllers.actions.{AuthenticatedAction, RegisteredAction}
import handlers.ErrorHandler
import models.{FinancialLineItem, TransactionHistoryItem}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class PaymentsController @Inject()(
                                    val controllerComponents: MessagesControllerComponents,
                                    authenticated: AuthenticatedAction,
                                    registered: RegisteredAction,
                                    sdilConnector: SoftDrinksIndustryLevyConnector,
                                    paymentsConnector: PayApiConnector,
                                    errorHandler: ErrorHandler)
                                  (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def setup(): Action[AnyContent] = (authenticated andThen registered).async { implicit request =>
    val sdilRef = request.subscription.sdilRef
    val transactions: service.AccountResult[List[FinancialLineItem]] = sdilConnector.balanceHistory(sdilRef, true, request.internalId)
    val dueDate: Option[LocalDate] = lastReturnDueDate(sdilRef, transactions)
    val res = for {
      balance <- sdilConnector.balance(sdilRef, true, request.internalId)
      nextUrl <- paymentsConnector.initJourney(sdilRef, balance, dueDate).map(_.nextUrl)
    } yield nextUrl

    res.value.map{
      case Right(nextUrl) => Redirect(nextUrl)
      case Left(_) => InternalServerError(errorHandler.internalServerErrorTemplate)
    }
  }

  private def lastReturnDueDate(sdilRef: String, transactions: service.AccountResult[List[FinancialLineItem]]): Option[LocalDate] = {
    val currentDate: LocalDate = new LocalDate.now()
    val existingBalance: BigDecimal = transactions.head.balance
    val returnChargeTransaction: FinancialLineItem = transactions.filter(_.financialLineItem.messageKey == "returnCharge").head
    val returnChargeTotal: BigDecimal = returnChargeTransaction.amount

    if (returnChargeTransaction.financialLineItem.date.after(currentDate) && isCurrentBasedOnAmount(existingBalance, returnChargeTotal)) {
      returnChargeTransaction.financialLineItem.date
    } else {
      None
    }
  }

  private def isCurrentBasedOnAmount(originalBalance: BigDecimal, returnTotal: BigDecimal): Boolean =
    if (originalBalance - returnTotal <= 0) true else false


}
