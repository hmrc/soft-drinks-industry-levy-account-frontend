/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package orchestrators

import base.SpecBase
import base.TestData._
import config.FrontendAppConfig
import connectors.SoftDrinksIndustryLevyConnector
import errors.{NoPendingReturns, UnexpectedResponseFromSDIL}
import models.{FinancialLineItem, PaymentOnAccount, ReturnCharge, ReturnPeriod, TransactionHistoryItem}
import models.requests.RegisteredRequest
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import repositories.SessionCache
import uk.gov.hmrc.auth.core.Enrolments

import java.time.LocalDate
import scala.concurrent.Future

class RegisteredOrchestratorSpec extends SpecBase with MockitoSugar {

  implicit val registeredRequest: RegisteredRequest[AnyContent] = RegisteredRequest(FakeRequest(), "id", Enrolments(Set.empty), aSubscription)
  val mockSDILConnector = mock[SoftDrinksIndustryLevyConnector]
  val mockSessionCache = mock[SessionCache]
  val mockConfig = mock[FrontendAppConfig]


  val orchestrator = new RegisteredOrchestrator(mockSDILConnector, mockSessionCache, mockConfig)

  "handleServicePageRequest" - {
    "should return a servicePageViewModel" - {
      "containing the pending returns, balance, no interest, direct debit status and subscription" - {
        "when the user has pending returns, no lastReturn, a positive balance, and no interest to pay and has a direct debit setup" in {
          val balance = BigDecimal(123.45)
          val balanceHistory = List(finincialItemReturnCharge)
          val interest = BigDecimal(0)
          val ddStatus = true
          val expectedResult = servicePageViewModel(pendingReturns3, None, balance, interest, Some(ddStatus))
          when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createSuccessAccountResult(pendingReturns3))
          when(mockSDILConnector.returns_get(UTR, currentReturnPeriod.previous, "id")(hc)).thenReturn(createSuccessAccountResult(None))
          when(mockSDILConnector.balance(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createSuccessAccountResult(balance))
          when(mockConfig.directDebitEnabled).thenReturn(true)
          when(mockSDILConnector.balanceHistory(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createSuccessAccountResult(balanceHistory))
          when(mockSDILConnector.checkDirectDebitStatus(aSubscription.sdilRef)(hc)).thenReturn(createSuccessAccountResult(ddStatus))

          val res = orchestrator.handleServicePageRequest(registeredRequest, hc, ec)

          whenReady(res.value) {result =>
            result mustBe Right(expectedResult)
          }
        }
      }

      "containing the last return sent, balance, no interest, ddStatus and subscription" - {
        "when the user has no pending returns, submitted a return for the current return period, has 0 balance and interest and has no direct debit setup" in {
          val balance = BigDecimal(0)
          val balanceHistory = List.empty
          val interest = BigDecimal(0)
          val ddStatus = false
          val expectedResult = servicePageViewModel(List.empty, Some(emptyReturn), balance, interest, Some(ddStatus))
          when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createSuccessAccountResult(List.empty))
          when(mockSDILConnector.returns_get(UTR, currentReturnPeriod.previous, "id")(hc)).thenReturn(createSuccessAccountResult(Some(emptyReturn)))
          when(mockSDILConnector.balance(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createSuccessAccountResult(balance))
          when(mockSDILConnector.balanceHistory(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createSuccessAccountResult(balanceHistory))
          when(mockConfig.directDebitEnabled).thenReturn(true)
          when(mockSDILConnector.checkDirectDebitStatus(aSubscription.sdilRef)(hc)).thenReturn(createSuccessAccountResult(ddStatus))

          val res = orchestrator.handleServicePageRequest(registeredRequest, hc, ec)

          whenReady(res.value) { result =>
            result mustBe Right(expectedResult)
          }
        }
      }


      "containing pending returns, the last return sent, balance, no interest, ddStatus and subscription" - {
        "when the user has pending returns, submitted a return for the current return period, has a balance in credit and no interest and direct debit is disabled" in {
          val balance = BigDecimal(-123.45)
          val balanceHistory = List.empty
          val interest = BigDecimal(0)
          val expectedResult = servicePageViewModel(pendingReturns3, Some(emptyReturn), balance, interest, None)
          when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createSuccessAccountResult(pendingReturns3))
          when(mockSDILConnector.returns_get(UTR, currentReturnPeriod.previous, "id")(hc)).thenReturn(createSuccessAccountResult(Some(emptyReturn)))
          when(mockSDILConnector.balance(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createSuccessAccountResult(balance))
          when(mockConfig.directDebitEnabled).thenReturn(false)
          when(mockSDILConnector.balanceHistory(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createSuccessAccountResult(balanceHistory))

          val res = orchestrator.handleServicePageRequest(registeredRequest, hc, ec)

          whenReady(res.value) { result =>
            result mustBe Right(expectedResult)
          }
        }
      }


      "containing pending returns sorted, no last return sent, a balance and interest to be payed and direct debit disabled" - {
        "when the user has pending returns in the wrong order and not submitted a return for the current return period" in {
          val balance = BigDecimal(123.45)
          val balanceHistory = allFinicialItems
          val interest = BigDecimal(20.45)
          val expectedResult = servicePageViewModel(pendingReturns3, None, balance, interest, None)

          when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createSuccessAccountResult(List(pendingReturn3, pendingReturn1, pendingReturn2)))
          when(mockSDILConnector.returns_get(UTR, currentReturnPeriod.previous, "id")(hc)).thenReturn(createSuccessAccountResult(None))
          when(mockSDILConnector.balance(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createSuccessAccountResult(balance))
          when(mockConfig.directDebitEnabled).thenReturn(false)
          when(mockSDILConnector.balanceHistory(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createSuccessAccountResult(balanceHistory))

          val res = orchestrator.handleServicePageRequest(registeredRequest, hc, ec)

          whenReady(res.value) { result =>
            result mustBe Right(expectedResult)
          }
        }
      }

      "when the finicial item list has no distinct values" - {
        "should filter out repeated items and calculate the interest based off that" in {
          val balance = BigDecimal(123.45)
          val balanceHistory = allFinicialItems ++ allFinicialItems
          val interest = BigDecimal(20.45)
          val expectedResult = servicePageViewModel(pendingReturns3, None, balance, interest, None)

          when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createSuccessAccountResult(List(pendingReturn3, pendingReturn1, pendingReturn2)))
          when(mockSDILConnector.returns_get(UTR, currentReturnPeriod.previous, "id")(hc)).thenReturn(createSuccessAccountResult(None))
          when(mockSDILConnector.balance(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createSuccessAccountResult(balance))
          when(mockSDILConnector.balanceHistory(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createSuccessAccountResult(balanceHistory))
          when(mockConfig.directDebitEnabled).thenReturn(false)

          val res = orchestrator.handleServicePageRequest(registeredRequest, hc, ec)

          whenReady(res.value) { result =>
            result mustBe Right(expectedResult)
          }
        }
      }
    }

    "return an UnexpectedResponseFromSDIL" - {
      "when the call to get pending returns fails" in {
        when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createFailureAccountResult(UnexpectedResponseFromSDIL))
        when(mockSDILConnector.returns_get(UTR, currentReturnPeriod.previous, "id")(hc)).thenReturn(createSuccessAccountResult(None))

        val res = orchestrator.handleServicePageRequest(registeredRequest, hc, ec)

        whenReady(res.value) { result =>
          result mustBe Left(UnexpectedResponseFromSDIL)
        }
      }

      "when the call to get lastReturn fails" in {
        when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createSuccessAccountResult(List.empty))
        when(mockSDILConnector.returns_get(UTR, currentReturnPeriod.previous, "id")(hc)).thenReturn(createFailureAccountResult(UnexpectedResponseFromSDIL))

        val res = orchestrator.handleServicePageRequest(registeredRequest, hc, ec)

        whenReady(res.value) { result =>
          result mustBe Left(UnexpectedResponseFromSDIL)
        }
      }

      "when the call to get balance fails" in {
        when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createSuccessAccountResult(List.empty))
        when(mockSDILConnector.returns_get(UTR, currentReturnPeriod.previous, "id")(hc)).thenReturn(createSuccessAccountResult(None))
        when(mockSDILConnector.balance(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createFailureAccountResult(UnexpectedResponseFromSDIL))

        val res = orchestrator.handleServicePageRequest(registeredRequest, hc, ec)

        whenReady(res.value) { result =>
          result mustBe Left(UnexpectedResponseFromSDIL)
        }
      }

      "when the call to get balanceAll fails" in {
        val balance = BigDecimal(123.45)
        when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createSuccessAccountResult(List.empty))
        when(mockSDILConnector.returns_get(UTR, currentReturnPeriod.previous, "id")(hc)).thenReturn(createSuccessAccountResult(None))
        when(mockSDILConnector.balance(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createSuccessAccountResult(balance))
        when(mockSDILConnector.balanceHistory(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createFailureAccountResult(UnexpectedResponseFromSDIL))

        val res = orchestrator.handleServicePageRequest(registeredRequest, hc, ec)

        whenReady(res.value) { result =>
          result mustBe Left(UnexpectedResponseFromSDIL)
        }
      }

      "when the call to get direct debit fails" in {
        val balance = BigDecimal(123.45)
        val balanceHistory = allFinicialItems ++ allFinicialItems
        when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createSuccessAccountResult(List.empty))
        when(mockSDILConnector.returns_get(UTR, currentReturnPeriod.previous, "id")(hc)).thenReturn(createSuccessAccountResult(None))
        when(mockSDILConnector.balance(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createSuccessAccountResult(balance))
        when(mockSDILConnector.balanceHistory(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createSuccessAccountResult(balanceHistory))
        when(mockConfig.directDebitEnabled).thenReturn(true)
        when(mockSDILConnector.checkDirectDebitStatus(aSubscription.sdilRef)(hc)).thenReturn(createFailureAccountResult(UnexpectedResponseFromSDIL))

        val res = orchestrator.handleServicePageRequest(registeredRequest, hc, ec)

        whenReady(res.value) { result =>
          result mustBe Left(UnexpectedResponseFromSDIL)
        }
      }

      "when all the calls fail" in {
        when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createFailureAccountResult(UnexpectedResponseFromSDIL))
        when(mockSDILConnector.returns_get(UTR, currentReturnPeriod.previous, "id")(hc)).thenReturn(createFailureAccountResult(UnexpectedResponseFromSDIL))
        when(mockSDILConnector.balance(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createFailureAccountResult(UnexpectedResponseFromSDIL))
        when(mockSDILConnector.balanceHistory(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createFailureAccountResult(UnexpectedResponseFromSDIL))
        when(mockConfig.directDebitEnabled).thenReturn(true)
        when(mockSDILConnector.checkDirectDebitStatus(aSubscription.sdilRef)(hc)).thenReturn(createFailureAccountResult(UnexpectedResponseFromSDIL))
        val res = orchestrator.handleServicePageRequest(registeredRequest, hc, ec)

        whenReady(res.value) { result =>
          result mustBe Left(UnexpectedResponseFromSDIL)
        }
      }
    }
  }

  "getTransactionHistoryForAllYears" - {
    "when the call to get balance history is successful" - {
      "and the user has no fininicial items" - {
        "should return an empty map" in {
          when(mockSDILConnector.balanceHistory(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createSuccessAccountResult(List.empty[FinancialLineItem]))

          val res = orchestrator.getTransactionHistoryForAllYears(registeredRequest, hc, ec)

          whenReady(res.value) { result =>
            result mustBe Right(Map.empty[Int, List[TransactionHistoryItem]])
          }
        }
      }

      "should return the expected" - {
        val year = 2022
        val date1 = LocalDate.of(year, 1, 30)
        val date2 = LocalDate.of(year, 6, 20)
        val date3 = LocalDate.of(year, 12, 1)
        val fi1 = ReturnCharge(ReturnPeriod.apply(date1), BigDecimal(-123.00))
        val fi2 = PaymentOnAccount(date2, "test", BigDecimal(123.00))
        val fi3 = finincialItemUnknown.copy(date = date3)

        "when the user only has one fininicial items" in {
          val balanceHistoryList = List(fi1)
          val expectedResult = Map(year -> List(TransactionHistoryItem(fi1, fi1.amount)))
          when(mockSDILConnector.balanceHistory(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createSuccessAccountResult(balanceHistoryList))

          val res = orchestrator.getTransactionHistoryForAllYears(registeredRequest, hc, ec)

          whenReady(res.value) { result =>
            result mustBe Right(expectedResult)
          }
        }

        "when the user only has multiple fininicial items for the same year" in {
          val balanceHistoryList = List(fi1, fi2, fi3)
          val expectedTransactionHistoryItems = List(
            TransactionHistoryItem(fi3, fi3.amount),
            TransactionHistoryItem(fi2, BigDecimal(0)),
            TransactionHistoryItem(fi1, fi1.amount)
          )
          val expectedResult = Map(year -> expectedTransactionHistoryItems)
          when(mockSDILConnector.balanceHistory(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createSuccessAccountResult(balanceHistoryList))

          val res = orchestrator.getTransactionHistoryForAllYears(registeredRequest, hc, ec)

          whenReady(res.value) { result =>
            result mustBe Right(expectedResult)
          }
        }

        "when the user only has multiple fininicial items for different years" in {
          val balanceHistoryList = List(fi1, fi2, fi3)
          val expectedTransactionHistoryItems = List(
            TransactionHistoryItem(fi3, fi3.amount),
            TransactionHistoryItem(fi2, BigDecimal(0)),
            TransactionHistoryItem(fi1, fi1.amount)
          )
          val expectedResult = Map(year -> expectedTransactionHistoryItems)
          when(mockSDILConnector.balanceHistory(aSubscription.sdilRef, true, "id")(hc)).thenReturn(createSuccessAccountResult(balanceHistoryList))

          val res = orchestrator.getTransactionHistoryForAllYears(registeredRequest, hc, ec)

          whenReady(res.value) { result =>
            result mustBe Right(expectedResult)
          }
        }
      }
    }
  }

  "handleStartAReturn" - {
    "should return lastest return" - {
      "when there is only one return pending" in {
        when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createSuccessAccountResult(pendingReturns1))
        when(mockSessionCache.removeRecord("id")).thenReturn(Future.successful(true))

        val res = orchestrator.handleStartAReturn(registeredRequest, hc, ec)

        whenReady(res.value) { result =>
          result mustBe Right(pendingReturn1)
        }
      }

      "when there is only more than 1 return pending and already ordered" in {
        when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createSuccessAccountResult(pendingReturns3))
        when(mockSessionCache.removeRecord("id")).thenReturn(Future.successful(true))

        val res = orchestrator.handleStartAReturn(registeredRequest, hc, ec)

        whenReady(res.value) { result =>
          result mustBe Right(pendingReturn3)
        }
      }

      "when there is only more than 1 return pending and they are not ordered" in {
        when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createSuccessAccountResult(List(pendingReturn2, pendingReturn3, pendingReturn1)))
        when(mockSessionCache.removeRecord("id")).thenReturn(Future.successful(true))

        val res = orchestrator.handleStartAReturn(registeredRequest, hc, ec)

        whenReady(res.value) { result =>
          result mustBe Right(pendingReturn3)
        }
      }
    }

    "return NoPendingReturns error when there are no returns pending" in {
      when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createSuccessAccountResult(List.empty))
      val res = orchestrator.handleStartAReturn(registeredRequest, hc, ec)

      whenReady(res.value) { result =>
        result mustBe Left(NoPendingReturns)
      }
    }


    "return UnexpectedResponseFromSDIL error when an error occurs" in {
      when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createFailureAccountResult(UnexpectedResponseFromSDIL))
      val res = orchestrator.handleStartAReturn(registeredRequest, hc, ec)

      whenReady(res.value) { result =>
        result mustBe Left(UnexpectedResponseFromSDIL)
      }
    }
  }

}
