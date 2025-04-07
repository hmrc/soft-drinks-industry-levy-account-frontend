package connectors

import errors.UnexpectedResponseFromPayAPI
import testSupport.preConditions.{PreconditionBuilder, PreconditionHelpers}
import play.api.inject.NewInstanceInjector.instanceOf
import testSupport.ITCoreTestData._
import testSupport.{LoggerHelper, Specifications, TestConfiguration}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.GenericLogger
import java.time.LocalDate
import org.scalatest.matchers.must.Matchers.{mustBe, mustEqual}

class PayApiConnectorISpec extends Specifications with TestConfiguration with LoggerHelper with PreconditionHelpers  {

  implicit val builder: PreconditionBuilder = new PreconditionBuilder()

  val currentYear = LocalDate.now().getYear
  val mockQuarter2 = LocalDate.of(currentYear, 4, 1)
  val mockQuarter3 = LocalDate.of(currentYear, 7, 1)
  val mockQuarter4 = LocalDate.of(currentYear, 10, 1)
  val mockQuarter1 = LocalDate.of(currentYear, 1, 1)
  val quarter1DueDate = LocalDate.of(currentYear, 4, 30)
  val quarter2DueDate = LocalDate.of(currentYear, 7, 30)
  val quarter3DueDate = LocalDate.of(currentYear, 10, 30)
  val quarter4DueDate = LocalDate.of(currentYear, 1, 30)

  val payApiConnector: PayApiConnector = app.injector.instanceOf[PayApiConnector]
  implicit val hc: HeaderCarrier = new HeaderCarrier()

  "initJourney " - {
    "should return a link to redirect the user to " - {
      "when the call to direct-debit succeeds" in {
        build
          .payApiStub.successCall()

        val res = payApiConnector.initJourney(aSubscriptionWithDeRegDate.sdilRef, 1000L, None, 50000.0)

        whenReady(res.value) { result =>
          result mustBe Right(nextUrlResponse)
        }
      }
    }

    "should return an UnexpectedResponseFromPayAPI error " - {
      "when the call to direct-debit fails" in {
        build
          .ddStub.failureCall

        val res = payApiConnector.initJourney(aSubscriptionWithDeRegDate.sdilRef, 1000L, None, 0.0)

        whenReady(res.value) { result =>
          result mustBe Left(UnexpectedResponseFromPayAPI)
        }
      }
    }

    "should log a message when no due date is passed to the API because of an overdue payment" in {
      build
        .payApiStub.successCall()

      withCaptureOfLoggingFrom(instanceOf[GenericLogger].logger) { events =>
        val res = payApiConnector.initJourney(aSubscriptionWithDeRegDate.sdilRef, 100000, Some(emptyReturn), 50000.0, quarter2DueDate.plusDays(1))

        whenReady(res.value) { _ =>
          events.collectFirst {
            case event =>
              event.getLevel.levelStr mustEqual "INFO"
              event.getMessage mustEqual s"[PayApiConnector][generateDueDate] - optLastReturn is not empty, due date of $quarter2DueDate generated on return period end"
          }.getOrElse(fail("No logging captured"))
          events(1).getMessage mustEqual s"[PayApiConnector][generateDueDate] - No due date passed to API, may be due to overdue returns or " +
            s"balance is greater than the prior return amount"
        }
      }
    }

    "should log a message when no due date is passed to the API because of an return amount due is less than the balance due " in {
      build
        .payApiStub.successCall()

      withCaptureOfLoggingFrom(instanceOf[GenericLogger].logger) { events =>
        val res = payApiConnector.initJourney(aSubscriptionWithDeRegDate.sdilRef, 100000.0, Some(emptyReturn), 50000.0, quarter2DueDate.plusDays(1))

        whenReady(res.value) { _ =>
          events.collectFirst {
            case event =>
              event.getLevel.levelStr mustEqual "INFO"
              event.getMessage mustEqual s"[PayApiConnector][generateDueDate] - optLastReturn is not empty, due date of $quarter2DueDate generated on return period end"
          }.getOrElse(fail("No logging captured"))
          events(1).getMessage mustEqual s"[PayApiConnector][generateDueDate] - No due date passed to API, may be due to overdue returns or " +
            s"balance is greater than the prior return amount"
        }
      }
    }

    "should log a message when no due date is passed to the API due to no return being sent" in {
      build
        .payApiStub.successCall()

      withCaptureOfLoggingFrom(instanceOf[GenericLogger].logger) { events =>
        val res = payApiConnector.initJourney(aSubscriptionWithDeRegDate.sdilRef, 1000L, None, 50000.0)

        whenReady(res.value) { result =>
          events.collectFirst {
            case event =>
              event.getLevel.levelStr mustEqual "INFO"
              event.getMessage mustEqual "[PayApiConnector][generateDueDate] - no LastReturn found based on the previous return period, presumed overdue"
          }.getOrElse(fail("No logging captured"))
        }
      }
    }

    "should log a message when a due date is passed to the API" in {
      build
        .payApiStub.successCall()

      withCaptureOfLoggingFrom(instanceOf[GenericLogger].logger) { events =>
        val mockDate = LocalDate.of(2023, 4, 5)
        val res = payApiConnector.initJourney(aSubscriptionWithDeRegDate.sdilRef, 1000, Some(emptyReturn), 0.0, mockDate)
          println(Console.YELLOW + "mock date " + mockDate + Console.WHITE)
        whenReady(res.value) { _ =>
          events.collectFirst {
            case event =>
              event.getLevel.levelStr mustEqual "INFO"
              event.getMessage mustEqual "[PayApiConnector][generateDueDate] - optLastReturn is not empty, due date of 2023-04-30 generated on return period end"
          }.getOrElse(fail("No logging captured"))
          events(1).getMessage mustEqual s"[PayApiConnector][generateDueDate] - due date is after today and balance is less than the prior return amount, " +
            s"presumed not overdue and future payment date can be offered"
        }
      }
    }

  }

  "generateDue date " - {
    "when the optLastReturn is not None " - {
      val optLastReturn = Some(emptyReturn)
      "and the last return balance is less than or equal to the current balance " - {
        val currentBalance: BigDecimal = -43512.27
        val lastReturnBalance: BigDecimal = -43512.27
        "assuming the user is paying within 30 days of " - {
          "quarter 1 should return the deadline of the last return period" in {
            val results = payApiConnector.generateDueDate(optLastReturn, currentBalance, lastReturnBalance, mockQuarter2)
            results mustBe Some(quarter1DueDate)
          }
          "quarter 2 should return the deadline of the last return period" in {
            val results = payApiConnector.generateDueDate(optLastReturn, currentBalance, lastReturnBalance, mockQuarter3)
            results mustBe Some(quarter2DueDate)
          }
          "quarter 3 should return the deadline of the last return period" in {
            val results = payApiConnector.generateDueDate(optLastReturn, currentBalance, lastReturnBalance, mockQuarter4)
            results mustBe Some(quarter3DueDate)
          }
          "quarter 4 should return the deadline of the last return period" in {
            val results = payApiConnector.generateDueDate(optLastReturn, currentBalance, lastReturnBalance, mockQuarter1)
            results mustBe Some(quarter4DueDate)
          }
        }
        "assuming the user is paying after 30 days of " - {
          "quarter 1 should return a due date of None" in {
            val results = payApiConnector.generateDueDate(optLastReturn, lastReturnBalance, currentBalance, quarter1DueDate.plusDays(1))
            results mustBe None
          }
          "quarter 2 should return a due date of None" in {
            val results = payApiConnector.generateDueDate(optLastReturn, lastReturnBalance, currentBalance, quarter2DueDate.plusDays(1))
            results mustBe None
          }
          "quarter 3 should return a due date of None" in {
            val results = payApiConnector.generateDueDate(optLastReturn, lastReturnBalance, currentBalance, quarter3DueDate.plusDays(1))
            results mustBe None
          }
          "quarter 4 should return a due date of None" in {
            val results = payApiConnector.generateDueDate(optLastReturn, lastReturnBalance, currentBalance, quarter4DueDate.plusDays(1))
            results mustBe None
          }
        }
      }
      "and the return balance is less than the current balance due " - {
        val currentBalanceWithInterest: BigDecimal = -43650.09
        val lastReturnBalance: BigDecimal = -43512.27
        "should return a due date of None" in {
          val results = payApiConnector.generateDueDate(optLastReturn, lastReturnBalance, currentBalanceWithInterest, mockQuarter2)
          results mustBe None

        }
      }
    }

    "when no optLastReturn is provided " - {
      "should return a due date of None" in {
        val results = payApiConnector.generateDueDate(None, 1000.0, 50000.0, mockQuarter2)
        results mustBe None
      }
    }
  }

}
