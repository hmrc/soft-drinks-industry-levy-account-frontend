package connectors

import errors.UnexpectedResponseFromDirectDebit
import testSupport.ITCoreTestData.*
import testSupport.{Specifications, TestConfiguration}
import uk.gov.hmrc.http.HeaderCarrier
import org.scalatest.matchers.must.Matchers.*
import org.scalatest.EitherValues.*
import testSupport.preConditions.{PreconditionBuilder, PreconditionHelpers}
import testSupport.Specifications
import org.scalatest.matchers.must.Matchers.mustBe


class DirectDebitConnectorISpec extends Specifications with TestConfiguration  {

  val ddConnector: DirectDebitConnector = app.injector.instanceOf[DirectDebitConnector]

  implicit val builder: PreconditionBuilder = new PreconditionBuilder()


  implicit val hc: HeaderCarrier = new HeaderCarrier()

  "initJourney" - {
    "should return a link to redirect the user to" - {
      "when the call to direct-debit succeeds" in {
        build
         .ddStub.successCall()

        val res = ddConnector.initJourney()

        whenReady(res.value) { result =>
          result mustBe Right(nextUrlResponse)
        }
      }
    }

    "should return an UnexpectedResponseFromDirectDebit error" - {
      "when the call to direct-debit fails" in {
        build
          .ddStub.failureCall

        val res = ddConnector.initJourney()

        whenReady(res.value) { result =>
          result mustBe Left(UnexpectedResponseFromDirectDebit)
        }
      }
    }
  }
}
