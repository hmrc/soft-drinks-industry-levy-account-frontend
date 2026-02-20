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

package controllers.testOnly

import controllers.ControllerITTestHelper
import models.SetupPayApiRequest
import org.scalatest.matchers.must.Matchers.*
import play.api.libs.json.{JsString, Json}
import play.api.libs.ws.DefaultWSCookie
import play.api.test.Helpers.{CONTENT_TYPE, JSON, LOCATION}
import play.api.test.WsTestClient
import testSupport.ITCoreTestData.*
import play.api.libs.ws.writeableOf_JsValue
import testSupport.preConditions.PreconditionBuilder
import play.api.libs.ws.DefaultBodyReadables.readableAsString

class TestOnlyControllerISpec extends ControllerITTestHelper {

  implicit val builder: PreconditionBuilder = new PreconditionBuilder()
  val initialiseDDPath = "/direct-debit-backend"
  val journeyDDPath = "/simulate-direct-debit-journey"
  val initialisePayPath = "/bta/sdil/journey/start"
  val journeyPayPath = "/simulate-pay-api-journey"

  s"POST $initialiseDDPath" - {
    "should return OK with the url for direct debit journey" in {
      WsTestClient.withClient { client =>
        val result1 = client.url(s"$testOnlyBaseUrl$initialiseDDPath")
          .withFollowRedirects(false)
          .addCookies(DefaultWSCookie("mdtp", authAndSessionCookie))
          .addHttpHeaders((CONTENT_TYPE, JSON))
          .post(Json.obj(
            ("returnUrl" -> JsString(homeUrl)),
            ("backUrl" -> JsString(homeUrl))
          ))

        whenReady(result1) { res =>
          res.status mustBe 200
          res.body must include(journeyDDPath)
        }
      }
    }
  }


  s"GET $journeyDDPath" - {
    "should redirect to sdil home page" in {
      WsTestClient.withClient { client =>
        val result1 = client.url(s"$testOnlyBaseUrl$journeyDDPath")
          .withFollowRedirects(false)
          .addCookies(DefaultWSCookie("mdtp", authAndSessionCookie))
          .get()

        whenReady(result1) { res =>
          res.status mustBe 303
          res.header(LOCATION).get mustBe homeUrl
        }
      }
    }
  }

  s"POST $initialisePayPath" - {
    "should return OK with the url for pay-api journey" in {
      WsTestClient.withClient { client =>
        val result1 = client.url(s"$testOnlyBaseUrl$initialisePayPath")
          .withFollowRedirects(false)
          .addCookies(DefaultWSCookie("mdtp", authAndSessionCookie))
          .addHttpHeaders((CONTENT_TYPE, JSON))
          .post(Json.toJson(SetupPayApiRequest(SDIL_REF, 1000L, None, homeUrl, homeUrl)))

        whenReady(result1) { res =>
          res.status mustBe 200
          res.body must include(journeyPayPath)
        }
      }
    }
  }


  s"GET $journeyPayPath" - {
    "should redirect to sdil home page" in {
      WsTestClient.withClient { client =>
        val result1 = client.url(s"$testOnlyBaseUrl$journeyPayPath")
          .withFollowRedirects(false)
          .addCookies(DefaultWSCookie("mdtp", authAndSessionCookie))
          .get()

        whenReady(result1) { res =>
          res.status mustBe 303
          res.header(LOCATION).get mustBe homeUrl
        }
      }
    }
  }

}
