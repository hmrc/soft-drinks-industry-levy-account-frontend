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
import connectors.{ PayApiConnector, SoftDrinksIndustryLevyConnector }
import controllers.actions.{ AuthenticatedAction, RegisteredAction }
import handlers.ErrorHandler
import models.{ ReturnPeriod, SdilReturn }
import play.api.i18n.I18nSupport
import play.api.mvc.{ Action, AnyContent, MessagesControllerComponents }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import service.AccountResult

import java.time.LocalDate
import scala.concurrent.{ ExecutionContext, Future }

class PaymentsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authenticated: AuthenticatedAction,
  registered: RegisteredAction,
  sdilConnector: SoftDrinksIndustryLevyConnector,
  paymentsConnector: PayApiConnector,
  errorHandler: ErrorHandler
)(implicit ec: ExecutionContext)
    extends FrontendBaseController with I18nSupport {

  def setup(): Action[AnyContent] = (authenticated andThen registered).async { implicit request =>
    val sdilRef = request.subscription.sdilRef
    val utr = request.subscription.utr
    val res = for {
      balance         <- sdilConnector.balance(sdilRef, withAssessment = true, request.internalId)
      optLastReturn   <- getOptLastReturn(utr, request.internalId)
      optReturnAmount <- getOptLastReturnAmount(sdilRef, request.internalId)
      nextUrl         <- paymentsConnector.initJourney(sdilRef, balance, optLastReturn, optReturnAmount).map(_.nextUrl)
    } yield nextUrl

    res.value.flatMap {
      case Right(nextUrl) => Future.successful(Redirect(nextUrl))
      case Left(_)        => errorHandler.internalServerErrorTemplate.map(errorView => InternalServerError(errorView))
    }
  }

  private def getOptLastReturn(utr: String, internalId: String)(implicit
    headerCarrier: HeaderCarrier
  ): AccountResult[Option[SdilReturn]] = {
    val lastReturnPeriod = ReturnPeriod(LocalDate.now).previous
    val getOptLastReturn = sdilConnector.returns_get(utr, lastReturnPeriod, internalId)
    getOptLastReturn
  }

  private def getOptLastReturnAmount(sdilRef: String, internalId: String)(implicit
    headerCarrier: HeaderCarrier
  ): AccountResult[BigDecimal] = {

    val lastReturnAmount = sdilConnector
      .balanceHistory(sdilRef, withAssessment = true, internalId)
      .map(items =>
        items.collectFirst { case item if item.messageKey == "returnCharge" => item.amount }.getOrElse(BigDecimal(0))
      )
    lastReturnAmount
  }

}
