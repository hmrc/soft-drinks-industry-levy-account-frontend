package controllers

import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.HeaderNames
import play.api.test.WsTestClient
import testSupport.ITCoreTestData._

class DirectDebitControllerISpec extends ControllerITTestHelper {

  val path = "/start-direct-debit-journey"

  s"GET $path" - {
    "should redirect to the url provided by direct-debit" - {
      "when the call to direct-debit succeeds" in {
        given
          .commonPrecondition
          .ddStub.successCall()

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + path)

          whenReady(result1) { res =>
            res.status mustBe 303
            res.header(HeaderNames.LOCATION).get mustBe directDebitResponse.nextUrl
          }
        }
      }
    }

    "should render the error page" - {
      "when the call to direct-debit fails" in {
        given
          .commonPrecondition
          .ddStub.failureCall

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + path)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, we are experiencing technical difficulties - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }
    }
  }
}
