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

package controllers

import org.jsoup.Jsoup
import play.api.http.HeaderNames
import play.api.test.WsTestClient
import testSupport.ITCoreTestData._
import testSupport.preConditions.{PreconditionHelpers, PreconditionBuilder}
import testSupport.Specifications
import org.scalatest.matchers.must.Matchers.mustBe

class DirectDebitControllerISpec extends ControllerITTestHelper with PreconditionHelpers with Specifications {

  implicit val builder: PreconditionBuilder = new PreconditionBuilder()
  val path = "/start-direct-debit-journey"

  s"GET $path" - {
    "should redirect to the url provided by direct-debit" - {
      "when the call to direct-debit succeeds" in {
        build
          .commonPrecondition
          .ddStub.successCall()

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + path)

          whenReady(result1) { res =>
            res.status mustBe 303
            res.header(HeaderNames.LOCATION).get mustBe nextUrlResponse.nextUrl
          }
        }
      }
    }

    "should render the error page" - {
      "when the call to direct-debit fails" in {
        build
          .commonPrecondition
          .ddStub.failureCall

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + path)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, there is a problem with the service - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }
    }
  }
}
