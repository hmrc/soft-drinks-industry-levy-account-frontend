package controllers.oldRoute

import controllers.ControllerITTestHelper
import org.scalatest.matchers.must.Matchers.*
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
