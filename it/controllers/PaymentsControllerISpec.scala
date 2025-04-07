package controllers

import org.jsoup.Jsoup
import play.api.http.HeaderNames
import play.api.test.WsTestClient
import testSupport.ITCoreTestData.*
import testSupport.preConditions.{PreconditionBuilder, PreconditionHelpers}
import testSupport.Specifications
import org.scalatest.matchers.must.Matchers.mustBe

class PaymentsControllerISpec extends ControllerITTestHelper with PreconditionHelpers with Specifications {

  val path = "/pay-now"
  implicit val builder: PreconditionBuilder = new PreconditionBuilder()

  s"GET $path " - {
    "should redirect to the url provided by pay-api " - {
      "when the call to pay-api succeeds" in {
        build
          .commonPrecondition
          .sdilBackend.balance(aSubscriptionWithDeRegDate.sdilRef, withAssessment = true)
          .sdilBackend.retrieveReturn(aSubscriptionWithDeRegDate.utr, pendingReturn1, resp = Some(emptyReturn))
          .sdilBackend.balanceHistory(aSubscriptionWithDeRegDate.sdilRef, withAssessment = true, financialItems = allFinancialItems)
          .payApiStub.successCall()

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + path)

          whenReady(result1) { res =>
            res.status mustBe 303
            res.header(HeaderNames.LOCATION).get mustBe nextUrlResponse.nextUrl
          }
        }
      }
    }

    "should render the error page " - {
      "when the call to pay-api fails" in {
        build
          .commonPrecondition
          .sdilBackend.balance(aSubscriptionWithDeRegDate.sdilRef, withAssessment = true)
          .sdilBackend.retrieveReturn(aSubscriptionWithDeRegDate.utr, pendingReturn1, resp = Some(emptyReturn))
          .sdilBackend.balanceHistory(aSubscriptionWithDeRegDate.sdilRef, withAssessment = true, allFinancialItems)
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


    "should render the error page " - {
      "when the call to sdil backend fails" in {
        build
          .commonPrecondition
          .sdilBackend.balancefailure(aSubscriptionWithDeRegDate.sdilRef, withAssessment = true)

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
