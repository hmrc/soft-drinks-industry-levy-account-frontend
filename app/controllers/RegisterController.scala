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
import config.FrontendAppConfig
import controllers.actions.{AuthenticatedAction, IdentifierAction}
import play.api.i18n.I18nSupport
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utilities.GenericLogger

import scala.concurrent.ExecutionContext

class RegisterController @Inject()(
                                    val controllerComponents: MessagesControllerComponents,
                                    val genericLogger: GenericLogger,
                                    authenticated: AuthenticatedAction,
                                    registrationAction: IdentifierAction
                                  )(implicit config: FrontendAppConfig, ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def start = (authenticated andThen registrationAction) { implicit r =>
    Redirect(config.startRegistrationUrl)
  }

}
