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

package models

import play.api.libs.json._

case class RosmRegistration(
  safeId: String,
  organisation: Option[OrganisationDetails],
  individual: Option[IndividualDetails],
  address: UkAddress
) {

  lazy val organisationName: String =
    organisation.map(_.organisationName).orElse(individual.map(i => s"${i.firstName} ${i.lastName}")).getOrElse("")
}

object RosmRegistration {
  implicit val addressReads: Reads[UkAddress] =
    (json: JsValue) =>
      for {
        jsObject <- json.validate[JsObject]
        line1    <- (jsObject \ "addressLine1").validate[String]
        line2    <- (jsObject \ "addressLine2").validateOpt[String]
        line3    <- (jsObject \ "addressLine3").validateOpt[String]
        line4    <- (jsObject \ "addressLine4").validateOpt[String]
        postCode <- (jsObject \ "postalCode").validate[String]
      } yield {
        val optlines: List[String] = List(line2, line3, line4).collect { case Some(l) => l }
        UkAddress(List(line1) ++ optlines, postCode, None)
      }

  implicit val readsROSMReg: Reads[RosmRegistration] = Json.reads[RosmRegistration]
}

case class OrganisationDetails(organisationName: String)

object OrganisationDetails {
  implicit val readsOrgDetails: Reads[OrganisationDetails] = Json.reads[OrganisationDetails]
}

case class IndividualDetails(firstName: String, lastName: String)

object IndividualDetails {
  implicit val readsIndDetails: Reads[IndividualDetails] = Json.reads[IndividualDetails]
}
