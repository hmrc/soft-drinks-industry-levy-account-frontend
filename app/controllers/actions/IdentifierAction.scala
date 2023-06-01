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
import models.requests.IdentifierRequest
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction extends ActionRefiner[Request, IdentifierRequest] with ActionBuilder[IdentifierRequest, AnyContent]

class AuthenticatedIdentifierAction @Inject()(override val authConnector: AuthConnector,
                                               val parser: BodyParsers.Default,
                                               sdilConnector: SoftDrinksIndustryLevyConnector,
                                               errorHandler: ErrorHandler
                                             )
                                             (implicit val executionContext: ExecutionContext, config: FrontendAppConfig)
  extends IdentifierAction
  with AuthorisedFunctions
  with ActionHelpers {

  override protected def refine[A](request: Request[A]): Future[Either[Result, IdentifierRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    val retrieval = allEnrolments and credentialRole and internalId and affinityGroup

    authorised(AuthProviders(GovernmentGateway)).retrieve(retrieval) {
      case enrolments ~ role ~ id ~ affinity =>
        id.fold[Future[Either[Result, IdentifierRequest[A]]]](
          Future.successful(
            Left(InternalServerError(errorHandler.internalServerErrorTemplate(request)))
          )) { internalId =>
          val maybeUtr = getUtr(enrolments)
          val maybeSdil = getSdilEnrolment(enrolments)
          (maybeUtr, maybeSdil) match {
            case (Some(utr), _) => sdilConnector.retrieveSubscription(utr, "utr", internalId).value
              .map {
                case Right(optSubscription) =>
                  Right(IdentifierRequest(request, internalId, enrolments, optSubscription))
                case Left(_) => Left(InternalServerError(errorHandler.internalServerErrorTemplate(request)))
              }
            case (_, Some(sdilEnrolment)) =>
              sdilConnector.retrieveSubscription(sdilEnrolment.value, "sdil", internalId).value
              .map {
                case Right(optSubscription) =>
                Right(IdentifierRequest(request, internalId, enrolments, optSubscription))
                case Left(_) => Left(InternalServerError(errorHandler.internalServerErrorTemplate(request)))
              }
            case _ => invalidRole(role).orElse(invalidAffinityGroup(affinity)) match {
              case Some(error) => Future.successful(Left(error))
              case None => Future.successful(Right(IdentifierRequest(request, internalId, enrolments, None)))
            }
          }
        }
    }.recover {
      case _: NoActiveSession =>
        Left(Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl))))
      case _: AuthorisationException =>
        Left(Redirect(routes.UnauthorisedController.onPageLoad))
    }
  }
}
