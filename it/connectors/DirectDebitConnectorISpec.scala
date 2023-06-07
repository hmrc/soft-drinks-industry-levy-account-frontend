package connectors

import errors.UnexpectedResponseFromDirectDebit
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import testSupport.ITCoreTestData._
import testSupport.{Specifications, TestConfiguration}
import uk.gov.hmrc.http.HeaderCarrier

class DirectDebitConnectorISpec extends Specifications with TestConfiguration {

  val ddConnector = app.injector.instanceOf[DirectDebitConnector]
  implicit val hc = new HeaderCarrier()

  "initJourney" - {
    "should return a link to redirect the user to" - {
      "when the call to direct-debit succeeds" in {
        given
          .ddStub.successCall()

        val res = ddConnector.initJourney()

        whenReady(res.value) { result =>
          result mustBe Right(directDebitResponse)
        }
      }
    }

    "should return an UnexpectedResponseFromDirectDebit error" - {
      "when the call to direct-debit fails" in {
        given
          .ddStub.failureCall

        val res = ddConnector.initJourney()

        whenReady(res.value) { result =>
          result mustBe Left(UnexpectedResponseFromDirectDebit)
        }
      }
    }
  }
}
