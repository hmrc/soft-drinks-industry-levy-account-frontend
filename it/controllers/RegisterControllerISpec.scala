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
import org.scalatest.matchers.must.Matchers.*
import play.api.http.HeaderNames
import play.api.test.WsTestClient
import testSupport.ITCoreTestData.*
import testSupport.ServicePageITHelper
import testSupport.Specifications
import org.scalatest.matchers.must.Matchers.mustBe
import testSupport.preConditions.PreconditionBuilder

class RegisterControllerISpec extends ServicePageITHelper with Specifications {

  implicit val builder: PreconditionBuilder = new PreconditionBuilder()

  val startPath = "/register/start"

  s"GET $startPath" - {
    "should redirect to /soft-drinks-industry-levy-registration/start" - {
      "when the user has a utr and subscription with a deregister date" in {
        build
          .authorisedWithSdilSubscriptionIncDeRegDatePrecondition

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + startPath)

          whenReady(result1) { res =>
            res.status mustBe 303
            res.header(HeaderNames.LOCATION).get must include("/soft-drinks-industry-levy-registration/start")
          }
        }
      }

      "when the user has no utr, a sdilRef and subscription with a deregister date" in {
        build
          .user.isAuthorisedAndEnrolledSDILRef
          .sdilBackend.retrieveSubscriptionWithDeRegDate("sdil", SDIL_REF)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + startPath)

          whenReady(result1) { res =>
            res.status mustBe 303
            res.header(HeaderNames.LOCATION).get must include("/soft-drinks-industry-levy-registration/start")
          }
        }
      }

      "when the user has no utr, sdilRef or subscription" in {
        build
          .user.isAuthorisedButNotEnrolled()

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + startPath)

          whenReady(result1) { res =>
            res.status mustBe 303
            res.header(HeaderNames.LOCATION).get must include("/soft-drinks-industry-levy-registration/start")
          }
        }
      }
    }

    "should redirect to the service page" - {
      "when the user has a subscription with no deregister date" in {
        build
          .commonPrecondition

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + startPath)

          whenReady(result1) { res =>
            res.status mustBe 303
            res.header(HeaderNames.LOCATION).get mustBe routes.ServicePageController.onPageLoad.url
          }
        }
      }
    }

    "should render the NotFound page when no utr, subscription but has sdilRef" in {
      build
        .user.isAuthorisedAndEnrolledSDILRef
        .sdilBackend.retrieveSubscriptionNone("sdil", SDIL_REF)

      WsTestClient.withClient { client =>
        val result1 = createClientRequestGet(client, baseUrl + startPath)

        whenReady(result1) { res =>
          res.status mustBe 404
          val page = Jsoup.parse(res.body)
          page.title() mustBe "Page not found - Soft Drinks Industry Levy - GOV.UK"
        }
      }
    }

    "when the backend call to get sdilSubscription fails with UTR" in {
      build
        .user.isAuthorisedAndEnrolled
        .sdilBackend.retrieveSubscriptionError("utr", UTR)

      WsTestClient.withClient { client =>
        val result1 = createClientRequestGet(client, baseUrl + startPath)

        whenReady(result1) { res =>
          res.status mustBe 500
          val page = Jsoup.parse(res.body)
          page.title() mustBe "Sorry, there is a problem with the service - Soft Drinks Industry Levy - GOV.UK"
        }
      }
    }

    "when the backend call to get sdilSubscription fails with SDIL_REF" in {
      build
        .user.isAuthorisedAndEnrolledSDILRef
        .sdilBackend.retrieveSubscriptionError("sdil", SDIL_REF)

      WsTestClient.withClient { client =>
        val result1 = createClientRequestGet(client, baseUrl + startPath)

        whenReady(result1) { res =>
          res.status mustBe 500
          val page = Jsoup.parse(res.body)
          page.title() mustBe "Sorry, there is a problem with the service - Soft Drinks Industry Levy - GOV.UK"
        }
      }
    }
  }
}
