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
import models.requests.AuthenticatedRequest
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ ExecutionContext, Future }

trait AuthenticatedAction
    extends ActionRefiner[Request, AuthenticatedRequest] with ActionBuilder[AuthenticatedRequest, AnyContent]

class AuthenticatedAuthenticatedAction @Inject() (
  override val authConnector: AuthConnector,
  val parser: BodyParsers.Default,
  sdilConnector: SoftDrinksIndustryLevyConnector,
  errorHandler: ErrorHandler
)(implicit ec: ExecutionContext, config: FrontendAppConfig)
    extends AuthenticatedAction with AuthorisedFunctions with ActionHelpers {

  override protected def executionContext: ExecutionContext = ec

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    val retrieval = allEnrolments and credentialRole and internalId and affinityGroup

    authorised(AuthProviders(GovernmentGateway))
      .retrieve(retrieval) { case enrolments ~ role ~ id ~ affinity =>
        id.fold[Future[Either[Result, AuthenticatedRequest[A]]]](
          errorHandler.internalServerErrorTemplate(using request).map(errorView => Left(InternalServerError(errorView)))
        ) { internalId =>
          val maybeUtr = getUtr(enrolments)
          val maybeSdil = getSdilEnrolment(enrolments)
          (maybeUtr, maybeSdil) match {
            case (Some(utr), _) =>
              sdilConnector
                .retrieveSubscription(utr, "utr", internalId)
                .value
                .flatMap {
                  case Right(optSubscription) =>
                    Future.successful(
                      Right(
                        AuthenticatedRequest(
                          request,
                          internalId,
                          enrolments,
                          optSubscription,
                          maybeUtr,
                          maybeSdil.map(_.value)
                        )
                      )
                    )
                  case Left(_) =>
                    errorHandler
                      .internalServerErrorTemplate(using request)
                      .map(errorView => Left(InternalServerError(errorView)))
                }
            case (_, Some(sdilEnrolment)) =>
              sdilConnector
                .retrieveSubscription(sdilEnrolment.value, "sdil", internalId)
                .value
                .flatMap {
                  case Right(optSubscription) =>
                    Future.successful(
                      Right(
                        AuthenticatedRequest(
                          request,
                          internalId,
                          enrolments,
                          optSubscription,
                          maybeUtr,
                          maybeSdil.map(_.value)
                        )
                      )
                    )
                  case Left(_) =>
                    errorHandler
                      .internalServerErrorTemplate(using request)
                      .map(errorView => Left(InternalServerError(errorView)))
                }
            case _ =>
              invalidRole(role).orElse(invalidAffinityGroup(affinity)) match {
                case Some(error) => Future.successful(Left(error))
                case None =>
                  Future.successful(Right(AuthenticatedRequest(request, internalId, enrolments, None, maybeUtr, None)))
              }
          }
        }
      }
      .recover {
        case _: NoActiveSession =>
          Left(Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl))))
        case _: AuthorisationException =>
          Left(Redirect(routes.UnauthorisedController.onPageLoad))
      }
  }
}
