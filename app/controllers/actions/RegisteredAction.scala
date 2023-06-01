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

package controllers.actions

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.SoftDrinksIndustryLevyConnector
import controllers.routes
import handlers.ErrorHandler
import models.requests.{IdentifierRequest, RegisteredRequest}
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}


class RegisteredActionImp @Inject()()
                                     (implicit val executionContext: ExecutionContext)
  extends RegisteredAction
  with ActionHelpers {
    override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, RegisteredRequest[A]]] = {

      request.optSubscription match {
        case None =>
          //ToDo redirect to identifier controller once implemented
          Future.successful(Left(Redirect(routes.IndexController.onPageLoad)))
        case Some(sub) =>
          Future.successful(Right(RegisteredRequest(request, request.internalId, request.enrolments, sub)))
      }
    }
}
trait RegisteredAction extends ActionRefiner[IdentifierRequest, RegisteredRequest]


