package controllers.oldRoute

import controllers.ControllerITTestHelper
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.HeaderNames
import play.api.test.WsTestClient

class RedirectToNewServiceControllerISpec extends ControllerITTestHelper {

  val path = "/"
  val homePath = "/home"
  val registerPath = "/register/start"

  s"GET $path" - {
    "should redirect to /soft-drinks-industry-levy-account-frontend/home" in {
      given
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
      given
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
      given
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
