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

import controllers.actions.AuthenticatedAction
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionCache
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import controllers.oldRoute.routes
class KeepAliveController @Inject()(
                                     val controllerComponents: MessagesControllerComponents,
                                     identify: AuthenticatedAction,
                                     sessionCache: SessionCache
                                   )(implicit ec: ExecutionContext) extends FrontendBaseController {

  def keepAlive: Action[AnyContent] = (identify).async {
    implicit request =>
      sessionCache.extendSession(request.internalId).map(_ =>
        Redirect(routes.RedirectToNewServiceController.home)
      )
  }
}
