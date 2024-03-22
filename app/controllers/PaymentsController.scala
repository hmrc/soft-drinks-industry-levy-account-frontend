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
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import service.AccountResult
import uk.gov.hmrc.http.HeaderCarrier
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
    val res = for {
      balance <- sdilConnector.balance(sdilRef, withAssessment = true, request.internalId)
      optLastDueDate <- getOptLastReturnDueDate(sdilRef, request.internalId, balance)
      nextUrl <- paymentsConnector.initJourney(sdilRef, balance, optLastDueDate).map(_.nextUrl)
    } yield nextUrl

    res.value.map{
      case Right(nextUrl) => Redirect(nextUrl)
      case Left(_) => InternalServerError(errorHandler.internalServerErrorTemplate)
    }
  }

  private def getOptLastReturnDueDate(sdilRef: String, internalId: String, balance: BigDecimal)
                                     (implicit headerCarrier: HeaderCarrier): AccountResult[Option[LocalDate]] =  {
    val lastReturnDueDate = sdilConnector.balanceHistory(sdilRef, withAssessment = true, internalId)
      .map(items =>
        items.filter(_.messageKey == "returnCharge").head.date
      )

    val lastReturnAmount = sdilConnector.balanceHistory(sdilRef, withAssessment = true, internalId)
        .map(items =>
          items.filter(_.messageKey == "returnCharge").head.amount
        )

    if (lastReturnDueDate.isAfter(LocalDate.now())) {
      lastReturnDueDate.map(date => Some(date))
    } else {
      lastReturnAmount.map(amount => if (balance - amount <= 0) Some(LocalDate.now()) else None)
    }
    val returnDueDateIsAfterCurrentDate = for {
      lastReturnDueDate <- lastReturnDueDate
      } yield lastReturnDueDate.isAfter(LocalDate.now())
      val balanceLessReturnAmountIsLessThanOrEqualToZero = for {
        lastReturnAmount <- lastReturnAmount
      } yield balance - lastReturnAmount <= 0
      for {
        notOverdueByDate <- returnDueDateIsAfterCurrentDate
        notOverdueByAmount <- balanceLessReturnAmountIsLessThanOrEqualToZero
      } yield (notOverdueByDate, notOverdueByAmount) match {
        case (true, true) => lastReturnDueDate
        case _ => LocalDate.now()
      }
      lastReturnDueDate.map(date => Some(date))
  }

}
