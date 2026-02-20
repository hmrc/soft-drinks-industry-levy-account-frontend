/*
 * Copyright 2026 HM Revenue & Customs
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

package connectors

import errors.UnexpectedResponseFromDirectDebit
import testSupport.ITCoreTestData.*
import testSupport.TestConfiguration
import uk.gov.hmrc.http.HeaderCarrier
import testSupport.preConditions.PreconditionBuilder
import testSupport.Specifications
import org.scalatest.matchers.must.Matchers.mustBe


class DirectDebitConnectorISpec extends Specifications with TestConfiguration  {

  val ddConnector: DirectDebitConnector = app.injector.instanceOf[DirectDebitConnector]
  implicit val builder: PreconditionBuilder = new PreconditionBuilder()
  implicit val hc: HeaderCarrier = new HeaderCarrier()

  "initJourney" - {
    "should return a link to redirect the user to" - {
      "when the call to direct-debit succeeds" in {
        build
         .ddStub.successCall()

        val res = ddConnector.initJourney()

        whenReady(res.value) { result =>
          result mustBe Right(nextUrlResponse)
        }
      }
    }

    "should return an UnexpectedResponseFromDirectDebit error" - {
      "when the call to direct-debit fails" in {
        build
          .ddStub.failureCall

        val res = ddConnector.initJourney()

        whenReady(res.value) { result =>
          result mustBe Left(UnexpectedResponseFromDirectDebit)
        }
      }
    }
  }
}
