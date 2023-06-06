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

import models.RetrievedSubscription

import javax.inject.Inject
import models.requests.AuthenticatedRequest
import play.api.mvc._
import uk.gov.hmrc.auth.core.Enrolments

import scala.concurrent.{ExecutionContext, Future}

class FakeAuthenticatedAction @Inject()(subscription: Option[RetrievedSubscription],
                                        bodyParsers: BodyParser[AnyContent]) extends AuthenticatedAction {

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] =
    Future.successful(Right(AuthenticatedRequest(request, "id", Enrolments(Set.empty), subscription, subscription.map(_.utr), subscription.map(_.sdilRef))))

  override def parser: BodyParser[AnyContent] =
    bodyParsers

  override protected def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global
}
