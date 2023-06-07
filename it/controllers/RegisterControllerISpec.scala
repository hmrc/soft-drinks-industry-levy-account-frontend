package controllers

import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers.{convertToAnyMustWrapper, include}
import play.api.http.HeaderNames
import play.api.test.WsTestClient
import testSupport.ITCoreTestData._
import testSupport.ReturnsITHelper

class RegisterControllerISpec extends ReturnsITHelper {

  val startPath = "/register/start"

  s"GET $startPath" - {
    "should redirect to /soft-drinks-industry-levy-registration/start" - {
      "when the user has a utr and subscription with a deregister date" in {
        given
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
        given
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
        given
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
        given
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
      given
        .user.isAuthorisedAndEnrolledSDILRef
        .sdilBackend.retrieveSubscriptionNone("sdil", SDIL_REF)

      WsTestClient.withClient { client =>
        val result1 = createClientRequestGet(client, baseUrl + startPath)

        whenReady(result1) { res =>
          res.status mustBe 404
          val page = Jsoup.parse(res.body)
          page.title() mustBe "Page not found - 404 - Soft Drinks Industry Levy - GOV.UK"
        }
      }
    }

    "when the backend call to get sdilSubscription fails with UTR" in {
      given
        .user.isAuthorisedAndEnrolled
        .sdilBackend.retrieveSubscriptionError("utr", UTR)

      WsTestClient.withClient { client =>
        val result1 = createClientRequestGet(client, baseUrl + startPath)

        whenReady(result1) { res =>
          res.status mustBe 500
          val page = Jsoup.parse(res.body)
          page.title() mustBe "Sorry, we are experiencing technical difficulties - 500 - Soft Drinks Industry Levy - GOV.UK"
        }
      }
    }

    "when the backend call to get sdilSubscription fails with SDIL_REF" in {
      given
        .user.isAuthorisedAndEnrolledSDILRef
        .sdilBackend.retrieveSubscriptionError("sdil", SDIL_REF)

      WsTestClient.withClient { client =>
        val result1 = createClientRequestGet(client, baseUrl + startPath)

        whenReady(result1) { res =>
          res.status mustBe 500
          val page = Jsoup.parse(res.body)
          page.title() mustBe "Sorry, we are experiencing technical difficulties - 500 - Soft Drinks Industry Levy - GOV.UK"
        }
      }
    }
  }
}
