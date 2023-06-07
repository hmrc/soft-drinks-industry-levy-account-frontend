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

import com.google.inject.Inject
import connectors.DirectDebitConnector
import controllers.actions.{AuthenticatedAction, RegisteredAction}
import handlers.ErrorHandler
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utilities.GenericLogger

import scala.concurrent.ExecutionContext

class DirectDebitController @Inject()(
                                       val controllerComponents: MessagesControllerComponents,
                                       val genericLogger: GenericLogger,
                                       authenticated: AuthenticatedAction,
                                       registered: RegisteredAction,
                                       directDebitConnector: DirectDebitConnector,
                                       errorHandler: ErrorHandler
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def setup(): Action[AnyContent] = (authenticated andThen registered).async { implicit request =>
    directDebitConnector.initJourney().value.map{
      case Right(resp) =>
        Redirect(resp.nextUrl)
      case Left(_) => InternalServerError(errorHandler.internalServerErrorTemplate)
    }
  }
}
