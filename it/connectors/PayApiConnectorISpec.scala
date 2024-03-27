package connectors

import errors.UnexpectedResponseFromPayAPI
import models.{ReturnPeriod, SdilReturn, SetupPayApiRequest}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import testSupport.ITCoreTestData._
import testSupport.{PayAPITestHelper, Specifications, TestConfiguration}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate

class PayApiConnectorISpec extends Specifications with TestConfiguration {

  val payApiConnector: PayApiConnector = app.injector.instanceOf[PayApiConnector]
  implicit val hc: HeaderCarrier = new HeaderCarrier()

  "initJourney " - {
    "should return a link to redirect the user to " - {
      "when the call to direct-debit succeeds" in {
        given
          .payApiStub.successCall()

        val res = payApiConnector.initJourney(aSubscriptionWithDeRegDate.sdilRef, 1000L, None, 50000.0)

        whenReady(res.value) { result =>
          result mustBe Right(nextUrlResponse)
        }
      }
    }

    "should return an UnexpectedResponseFromPayAPI error " - {
      "when the call to direct-debit fails" in {
        given
          .ddStub.failureCall

        val res = payApiConnector.initJourney(aSubscriptionWithDeRegDate.sdilRef, 1000L, None, 0.0)

        whenReady(res.value) { result =>
          result mustBe Left(UnexpectedResponseFromPayAPI)
        }
      }
    }

  }

  def generateDueDate(optLastReturn: Option[SdilReturn], priorReturnAmount: BigDecimal, balance: BigDecimal, mockToday: LocalDate): Option[LocalDate] = {
    val lastReturnPeriod = ReturnPeriod(mockToday).previous
    val dueDate = if (optLastReturn.nonEmpty) {
      Some(lastReturnPeriod.deadline)
    } else {
      None
    }

    dueDate match {
      case Some(dueDate) =>
        if (dueDate.isAfter(mockToday) && (balance - priorReturnAmount >= 0)) {
          Some(dueDate)
        } else {
          None
        }
      case None => None
    }
  }

  "generateDue date " - {
    val currentYear = LocalDate.now().getYear
    val mockQuarter2 = LocalDate.of(currentYear, 4, 1)
    val mockQuarter3 = LocalDate.of(currentYear, 7, 1)
    val mockQuarter4 = LocalDate.of(currentYear, 10, 1)
    val mockQuarter1 = LocalDate.of(currentYear, 1, 1)
    val quarter1DueDate = LocalDate.of(currentYear, 4, 30)
    val quarter2DueDate = LocalDate.of(currentYear, 7, 30)
    val quarter3DueDate = LocalDate.of(currentYear, 10, 30)
    val quarter4DueDate = LocalDate.of(currentYear, 1, 30)
    "when the optLastReturn is not None " - {
      val optLastReturn = Some(emptyReturn)
      "and the last return balance is less than or equal to the current balance " - {
        val currentBalance: BigDecimal = -43512.27
        val lastReturnBalance: BigDecimal = -43512.27
        "assuming the user is paying within 30 days of " - {
          "quarter 1 should return the deadline of the last return period" in {
            val results = generateDueDate(optLastReturn, currentBalance, lastReturnBalance, mockQuarter2)
            results mustBe Some(quarter1DueDate)
          }
          "quarter 2 should return the deadline of the last return period" in {
            val results = generateDueDate(optLastReturn, currentBalance, lastReturnBalance, mockQuarter3)
            results mustBe Some(quarter2DueDate)
          }
          "quarter 3 should return the deadline of the last return period" in {
            val results = generateDueDate(optLastReturn, currentBalance, lastReturnBalance, mockQuarter4)
            results mustBe Some(quarter3DueDate)
          }
          "quarter 4 should return the deadline of the last return period" in {
            val results = generateDueDate(optLastReturn, currentBalance, lastReturnBalance, mockQuarter1)
            results mustBe Some(quarter4DueDate)
          }
        }
        "assuming the user is paying after 30 days of " - {
          "quarter 1 should return a due date of None" in {
            val results = generateDueDate(optLastReturn, lastReturnBalance, currentBalance, quarter1DueDate.plusDays(1))
            results mustBe None
          }
          "quarter 2 should return a due date of None" in {
            val results = generateDueDate(optLastReturn, lastReturnBalance, currentBalance, quarter2DueDate.plusDays(1))
            results mustBe None
          }
          "quarter 3 should return a due date of None" in {
            val results = generateDueDate(optLastReturn, lastReturnBalance, currentBalance, quarter3DueDate.plusDays(1))
            results mustBe None
          }
          "quarter 4 should return a due date of None" in {
            val results = generateDueDate(optLastReturn, lastReturnBalance, currentBalance, quarter4DueDate.plusDays(1))
            results mustBe None
          }
        }
      }
      "and the return balance is less than the current balance due " - {
        val currentBalanceWithInterest: BigDecimal = -43650.09
        val lastReturnBalance: BigDecimal = -43512.27
        "should return a due date of None" in {
          val results = generateDueDate(optLastReturn, lastReturnBalance, currentBalanceWithInterest, mockQuarter2)
          results mustBe None
        }
      }
    }

    "when no optLastReturn is provided " - {
      "should return a due date of None" in {
        val results = generateDueDate(None, 1000.0, 50000.0, mockQuarter2)
        results mustBe None
      }
    }
  }

}
