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

import controllers.routes
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._

trait ActionHelpers {

  val TWO = 2
  val FOUR = 4

  val registrationRetrieval = allEnrolments and credentialRole and internalId and affinityGroup

  protected def getSdilEnrolment(enrolments: Enrolments): Option[EnrolmentIdentifier] = {
    val sdil = for {
      enrolment <- enrolments.enrolments if enrolment.key.equalsIgnoreCase("HMRC-OBTDS-ORG")
      sdil      <- enrolment.getIdentifier("EtmpRegistrationNumber") if sdil.value.slice(TWO, FOUR) == "SD"
    } yield sdil

    sdil.headOption
  }

  protected def getUtr(enrolments: Enrolments): Option[String] =
    enrolments
      .getEnrolment("IR-CT")
      .orElse(enrolments.getEnrolment("IR-SA"))
      .flatMap(_.getIdentifier("UTR").map(_.value))

  protected def invalidRole(credentialRole: Option[CredentialRole]): Option[Result] =
    credentialRole collect { case Assistant =>
      // Todo implement forbidden invalidRole page
      Redirect(routes.UnauthorisedController.onPageLoad)
    }

  protected def invalidAffinityGroup(affinityGroup: Option[AffinityGroup]): Option[Result] =
    affinityGroup match {
      case Some(Agent) | None =>
        // Todo implement forbidden invalidAffinity group page
        Some(Redirect(routes.UnauthorisedController.onPageLoad))
      case _ => None
    }
}
