package connectors

import errors.UnexpectedResponseFromDirectDebit
import testSupport.ITCoreTestData._
import testSupport.{Specifications, TestConfiguration}
import uk.gov.hmrc.http.HeaderCarrier
import org.scalatest.matchers.must.Matchers._
import org.scalatest.EitherValues._
import testSupport.preConditions.PreconditionHelpers


class DirectDebitConnectorISpec extends Specifications with PreconditionHelpers  {

  val ddConnector = app.injector.instanceOf[DirectDebitConnector]
  implicit val hc: HeaderCarrier = new HeaderCarrier()

  "initJourney" - {
    "should return a link to redirect the user to" - {
      "when the call to direct-debit succeeds" in {
        given ddStub.successCall()

        val res = ddConnector.initJourney()

        whenReady(res.value) { result =>
          result mustBe Right(nextUrlResponse)
        }
      }
    }

    "should return an UnexpectedResponseFromDirectDebit error" - {
      "when the call to direct-debit fails" in {
        given ddStub.failureCall()

        val res = ddConnector.initJourney()

        whenReady(res.value) { result =>
          result mustBe Left(UnexpectedResponseFromDirectDebit)
        }
      }
    }
  }
}
