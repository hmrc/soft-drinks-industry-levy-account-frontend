package connectors

import errors.UnexpectedResponseFromPayAPI
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import testSupport.ITCoreTestData._
import testSupport.{Specifications, TestConfiguration}
import uk.gov.hmrc.http.HeaderCarrier

class PayApiConnectorISpec extends Specifications with TestConfiguration {

  val payApiConnector = app.injector.instanceOf[PayApiConnector]
  implicit val hc = new HeaderCarrier()

  "initJourney" - {
    "should return a link to redirect the user to" - {
      "when the call to direct-debit succeeds" in {
        given
          .payApiStub.successCall()

        val res = payApiConnector.initJourney(aSubscriptionWithDeRegDate.sdilRef, 1000L)

        whenReady(res.value) { result =>
          result mustBe Right(nextUrlResponse)
        }
      }
    }

    "should return an UnexpectedResponseFromPayAPI error" - {
      "when the call to direct-debit fails" in {
        given
          .ddStub.failureCall

        val res = payApiConnector.initJourney(aSubscriptionWithDeRegDate.sdilRef, 1000L)

        whenReady(res.value) { result =>
          result mustBe Left(UnexpectedResponseFromPayAPI)
        }
      }
    }
  }
}
