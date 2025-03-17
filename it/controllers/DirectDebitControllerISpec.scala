package controllers

import org.jsoup.Jsoup
import play.api.http.HeaderNames
import play.api.test.WsTestClient
import testSupport.ITCoreTestData._
import testSupport.preConditions.PreconditionHelpers
import testSupport.preConditions.PreconditionBuilder
import testSupport.Specifications
import org.scalatest.matchers.must.Matchers.mustBe

class DirectDebitControllerISpec extends ControllerITTestHelper with PreconditionHelpers with Specifications {

  // Provide an implicit PreconditionBuilder as required by PreconditionHelpers
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
            page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }
    }
  }
}
