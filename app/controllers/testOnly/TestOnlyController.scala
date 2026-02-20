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

package controllers.testOnly

import com.google.inject.Inject
import config.FrontendAppConfig
import models.NextUrl
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.Future

class TestOnlyController @Inject() (val controllerComponents: MessagesControllerComponents, config: FrontendAppConfig)
    extends FrontendBaseController with I18nSupport {

  def stubDirectDebitJourney() = Action.async { request =>
    Future.successful(Redirect(config.homePage))
  }

  def stubDirectDebitInitialise() = Action.async { request =>
    Future.successful(Ok(Json.toJson(NextUrl(routes.TestOnlyController.stubDirectDebitJourney().url))))
  }

  def stubPayApiJourney() = Action.async { request =>
    Future.successful(Redirect(config.homePage))
  }

  def stubPayApiInitialise() = Action.async { request =>
    Future.successful(Ok(Json.toJson(NextUrl(routes.TestOnlyController.stubPayApiJourney().url))))
  }

}
