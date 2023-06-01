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

import base.SpecBase
import play.api.libs.json.Json

class ROSMRegistrationSpec extends SpecBase {

  val rosmRegistrationOrgJson =
    """{"safeId":"fvp41Gm51rswaeiysohztnrqjdfz7cOnael38omHvuH2ye519ncqiXruPqjBbwewiKdmthpsphun",
      |"isEditable":false,"isAnAgent":false,"isAnIndividual":false,"organisation":{"organisationName":"foo"},
      |"address":{"addressLine1":"50","addressLine2":"The Lane","addressLine3":"The Town",
      |"countryCode":"GB","postalCode":"SM32 5IA"},
      |"contactDetails":{"primaryPhoneNumber":"08926 167394","emailAddress":"qovmlk@rlkioorw.com"}}""".stripMargin

  val rosmRegistrationIndividualJson =
    """{"safeId":"fvp41Gm51rswaeiysohztnrqjdfz7cOnael38omHvuH2ye519ncqiXruPqjBbwewiKdmthpsphun",
      |"isEditable":false,"isAnAgent":false,"isAnIndividual":true,"individual":{"firstName":"name1","lastName":"name2"},
      |"address":{"addressLine1":"50","addressLine2":"The Lane","addressLine3":"The Town",
      |"countryCode":"GB","postalCode":"SM32 5IA"},
      |"contactDetails":{"primaryPhoneNumber":"08926 167394","emailAddress":"qovmlk@rlkioorw.com"}}""".stripMargin

  val ROSMRegistrationOrg = RosmRegistration(
     safeId = "fvp41Gm51rswaeiysohztnrqjdfz7cOnael38omHvuH2ye519ncqiXruPqjBbwewiKdmthpsphun",
     organisation = Some(OrganisationDetails("foo")),
     individual = None,
     address = UkAddress(
       lines = List("50", "The Lane", "The Town"),
       postCode = "SM32 5IA"
     )
  )

  val ROSMRegistrationIndividual = RosmRegistration(
    safeId = "fvp41Gm51rswaeiysohztnrqjdfz7cOnael38omHvuH2ye519ncqiXruPqjBbwewiKdmthpsphun",
    organisation = None,
    individual = Some(IndividualDetails("name1", "name2")),
    address = UkAddress(
      lines = List("50", "The Lane", "The Town"),
      postCode = "SM32 5IA"
    )
  )

  "RosmRegistration" - {
    "reads must read json correctly" - {
      "when an organisation" in {
        val res = Json.parse(rosmRegistrationOrgJson).as[RosmRegistration]
        res mustEqual ROSMRegistrationOrg
      }

      "when an individual" - {
        val res = Json.parse(rosmRegistrationIndividualJson).as[RosmRegistration]
        res mustEqual ROSMRegistrationIndividual
      }
    }
  }

}
