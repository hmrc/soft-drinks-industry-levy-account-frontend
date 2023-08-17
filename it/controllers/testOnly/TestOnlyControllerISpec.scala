package controllers.testOnly

import controllers.ControllerITTestHelper
import models.SetupPayApiRequest
import org.scalatest.matchers.must.Matchers.{convertToAnyMustWrapper, include}
import play.api.libs.json.{JsString, Json}
import play.api.libs.ws.DefaultWSCookie
import play.api.test.Helpers.{CONTENT_TYPE, JSON, LOCATION}
import play.api.test.WsTestClient
import testSupport.ITCoreTestData._

class TestOnlyControllerISpec extends ControllerITTestHelper {

  val initialiseDDPath = "/direct-debit-backend"
  val journeyDDPath = "/simulate-direct-debit-journey"

  val initialisePayPath = "/bta/sdil/journey/start"
  val journeyPayPath = "/simulate-pay-api-journey"

  val homeUrl = "http://www.example.com/soft-drinks-industry-levy-account-frontend/home"

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
          .post(Json.toJson(SetupPayApiRequest(SDIL_REF, 1000L, homeUrl, homeUrl)))

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
