package controllers.testOnly

import controllers.ControllerITTestHelper
import org.scalatest.matchers.must.Matchers.{convertToAnyMustWrapper, include}
import play.api.libs.json.{JsString, Json}
import play.api.libs.ws.DefaultWSCookie
import play.api.test.Helpers.{CONTENT_TYPE, JSON, LOCATION}
import play.api.test.WsTestClient

class TestOnlyControllerISpec extends ControllerITTestHelper {

  val initialisePath = "/direct-debit-backend"
  val journeyPath = "/simulate-direct-debit-journey"

  s"POST $initialisePath" - {
    "should return OK with the url for direct debit journey" in {
      WsTestClient.withClient { client =>
        val result1 = client.url(s"$testOnlyBaseUrl$initialisePath")
          .withFollowRedirects(false)
          .addCookies(DefaultWSCookie("mdtp", authAndSessionCookie))
          .addHttpHeaders((CONTENT_TYPE, JSON))
          .post(Json.obj(
            ("returnUrl" -> JsString("http://example.com")),
            ("backUrl" -> JsString("http://example.com"))
          ))

        whenReady(result1) { res =>
          res.status mustBe 200
          res.body must include(journeyPath)
        }
      }
    }
  }


  s"GET $journeyPath" - {
    "should redirect to sdil home page" in {
      WsTestClient.withClient { client =>
        val result1 = client.url(s"$testOnlyBaseUrl$journeyPath")
          .withFollowRedirects(false)
          .addCookies(DefaultWSCookie("mdtp", authAndSessionCookie))
          .get()

        whenReady(result1) { res =>
          res.status mustBe 303
          res.header(LOCATION).get mustBe "http://www.example.com/home"
        }
      }
    }
  }

}
