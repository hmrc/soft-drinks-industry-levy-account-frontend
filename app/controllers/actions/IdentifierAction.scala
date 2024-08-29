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
import controllers.routes
import handlers.ErrorHandler
import models.requests.{AuthenticatedRequest, IdentificationRequest}
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}


class IdentificationActionImp @Inject()(errorHandler: ErrorHandler)
                                       (implicit val executionContext: ExecutionContext)
  extends IdentifierAction
  with ActionHelpers {
    override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, IdentificationRequest[A]]] = {
      (request.optUtr, request.optSubscription) match {
        case (Some(_), Some(sub)) if sub.deregDate.isEmpty =>
          Future.successful(Left(Redirect(routes.ServicePageController.onPageLoad)))
        case (Some(_), optSub) => Future.successful(Right(IdentificationRequest(request, request.internalId, optSub, request.optUtr)))
        case (None, Some(sub)) if sub.deregDate.nonEmpty => Future.successful(Right(IdentificationRequest(request, request.internalId, None, request.optUtr)))
        case _ if request.optSdilRef.isDefined => errorHandler.notFoundTemplate(request).map(errorView => Left(NotFound(errorView)))
        case _ => Future.successful(Right(IdentificationRequest(request, request.internalId, None, request.optUtr)))
      }
    }
}
trait IdentifierAction extends ActionRefiner[AuthenticatedRequest, IdentificationRequest]


