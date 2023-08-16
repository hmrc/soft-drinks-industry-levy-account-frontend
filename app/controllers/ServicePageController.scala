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
import controllers.actions.{AuthenticatedAction, RegisteredAction}
import errors.NoPendingReturns
import handlers.ErrorHandler
import models.{DeregisteredUserServicePageViewModel, RegisteredUserServicePageViewModel}
import orchestrators.RegisteredOrchestrator
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utilities.GenericLogger
import views.html.{ServiceView, DeregisteredUserServiceView}

import scala.concurrent.ExecutionContext

class ServicePageController @Inject()(
                                       val controllerComponents: MessagesControllerComponents,
                                       val genericLogger: GenericLogger,
                                       authenticated: AuthenticatedAction,
                                       registered: RegisteredAction,
                                       registeredOrchestrator: RegisteredOrchestrator,
                                       serviceView: ServiceView,
                                       deregServiceView: DeregisteredUserServiceView,
                                       errorHandler: ErrorHandler
                                    )(implicit config: FrontendAppConfig, ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (authenticated andThen registered).async { implicit request =>

    registeredOrchestrator.handleServicePageRequest.value.map{
      case Right(viewModel: RegisteredUserServicePageViewModel) =>
        Ok(serviceView(
          viewModel)
        (implicitly, implicitly, implicitly))
      case Right(viewModel: DeregisteredUserServicePageViewModel) =>
        Ok(deregServiceView(
          viewModel)
        (implicitly, implicitly, implicitly))
      case Left(_) => InternalServerError(errorHandler.internalServerErrorTemplate)
    }
  }

  def startAReturn(isNilReturn: Boolean) = (authenticated andThen registered).async {implicit request =>
    registeredOrchestrator.handleStartAReturn.value.map{
      case Right(returnPeriod) =>
        val url = config.startReturnUrl(returnPeriod.year, returnPeriod.quarter, isNilReturn)
        Redirect(url)
      case Left(NoPendingReturns) =>
        genericLogger.logger.warn("Unable to start return - no returns pending")
        Redirect(routes.ServicePageController.onPageLoad)
      case Left(_) => InternalServerError(errorHandler.internalServerErrorTemplate)
    }
  }

  def makeAChange = (authenticated andThen registered).async { implicit request =>
    registeredOrchestrator.emptyCache.map { _ =>
      Redirect(config.makeAChangeUrl)
    }
  }

  def correctAReturn = (authenticated andThen registered).async { implicit request =>
    registeredOrchestrator.emptyCache.map { _ =>
      Redirect(config.correctAReturnUrl)
    }
  }
}
