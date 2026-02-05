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

package controllers.oldRoute

import controllers.ControllerITTestHelper
import play.api.http.HeaderNames
import play.api.test.WsTestClient
import testSupport.Specifications
import org.scalatest.matchers.must.Matchers.mustBe
import testSupport.preConditions.PreconditionBuilder

class RedirectToNewServiceControllerISpec extends ControllerITTestHelper with Specifications {

  implicit val builder: PreconditionBuilder = new PreconditionBuilder()

  val path = "/"
  val homePath = "/home"
  val registerPath = "/register/start"

  s"GET $path" - {
    "should redirect to /soft-drinks-industry-levy-account-frontend/home" in {
      build
        .commonPrecondition

      WsTestClient.withClient { client =>
        val result1 = createClientRequestGet(client, oldBaseUrl + path)

        whenReady(result1) { res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION).get mustBe controllers.routes.ServicePageController.onPageLoad.url
        }
      }
    }
  }

  s"GET $homePath" - {
    "should redirect to /soft-drinks-industry-levy-account-frontend/home" in {
      build
        .commonPrecondition

      WsTestClient.withClient { client =>
        val result1 = createClientRequestGet(client, oldBaseUrl + homePath)

        whenReady(result1) { res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION).get mustBe controllers.routes.ServicePageController.onPageLoad.url
        }
      }
    }
  }

  s"GET $registerPath" - {
    "should redirect to /soft-drinks-industry-levy-account-frontend/register" in {
      build
        .commonPrecondition

      WsTestClient.withClient { client =>
        val result1 = createClientRequestGet(client, oldBaseUrl + registerPath)

        whenReady(result1) { res =>
          res.status mustBe 303
          res.header(HeaderNames.LOCATION).get mustBe controllers.routes.RegisterController.start.url
        }
      }
    }
  }
}
