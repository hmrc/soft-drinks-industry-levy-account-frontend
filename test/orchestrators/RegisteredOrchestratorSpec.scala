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

import base.TestData._
import base.SpecBase
import connectors.SoftDrinksIndustryLevyConnector
import errors.{NoPendingReturns, UnexpectedResponseFromSDIL}
import models.requests.RegisteredRequest
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import repositories.SessionCache
import uk.gov.hmrc.auth.core.Enrolments

import scala.concurrent.Future

class RegisteredOrchestratorSpec extends SpecBase with MockitoSugar {

  implicit val registeredRequest: RegisteredRequest[AnyContent] = RegisteredRequest(FakeRequest(), "id", Enrolments(Set.empty), aSubscription)
  val mockSDILConnector = mock[SoftDrinksIndustryLevyConnector]
  val mockSessionCache = mock[SessionCache]

  val orchestrator = new RegisteredOrchestrator(mockSDILConnector, mockSessionCache)

  "handleServicePageRequest" - {
    "should return a servicePageViewModel" - {
      "containing the pending returns and subscription" - {
        "when the user has pending returns and no lastReturn" in {
          when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createSuccessAccountResult(pendingReturns3))
          when(mockSDILConnector.returns_get(UTR, currentReturnPeriod.previous, "id")(hc)).thenReturn(createSuccessAccountResult(None))

          val res = orchestrator.handleServicePageRequest(registeredRequest, hc, ec)

          whenReady(res.value) {result =>
            result mustBe Right(servicePageViewModel3PendingReturns)
          }
        }
      }

      "containing the last return sent and subscription" - {
        "when the user has no pending returns and submitted a return for the current return period" in {
          when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createSuccessAccountResult(List.empty))
          when(mockSDILConnector.returns_get(UTR, currentReturnPeriod.previous, "id")(hc)).thenReturn(createSuccessAccountResult(Some(emptyReturn)))

          val res = orchestrator.handleServicePageRequest(registeredRequest, hc, ec)

          whenReady(res.value) { result =>
            result mustBe Right(servicePageViewModelWithLastReturn)
          }
        }
      }


      "containing pending returns and the last return sent and subscription" - {
        "when the user has pending returns and submitted a return for the current return period" in {
          when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createSuccessAccountResult(pendingReturns1))
          when(mockSDILConnector.returns_get(UTR, currentReturnPeriod.previous, "id")(hc)).thenReturn(createSuccessAccountResult(Some(emptyReturn)))

          val res = orchestrator.handleServicePageRequest(registeredRequest, hc, ec)

          whenReady(res.value) { result =>
            result mustBe Right(servicePageViewModelWithLastReturn.copy(overdueReturns = pendingReturns1))
          }
        }
      }


      "containing pending returns sorted and no last return sent and subscription" - {
        "when the user has pending returns in the wrong order and not submitted a return for the current return period" in {
          when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createSuccessAccountResult(List(pendingReturn3, pendingReturn1, pendingReturn2)))
          when(mockSDILConnector.returns_get(UTR, currentReturnPeriod.previous, "id")(hc)).thenReturn(createSuccessAccountResult(None))

          val res = orchestrator.handleServicePageRequest(registeredRequest, hc, ec)

          whenReady(res.value) { result =>
            result mustBe Right(servicePageViewModel3PendingReturns)
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


      "when both the call to get lastReturn and pending returns fails" in {
        when(mockSDILConnector.returns_pending("id", UTR)(hc)).thenReturn(createFailureAccountResult(UnexpectedResponseFromSDIL))
        when(mockSDILConnector.returns_get(UTR, currentReturnPeriod.previous, "id")(hc)).thenReturn(createFailureAccountResult(UnexpectedResponseFromSDIL))

        val res = orchestrator.handleServicePageRequest(registeredRequest, hc, ec)

        whenReady(res.value) { result =>
          result mustBe Left(UnexpectedResponseFromSDIL)
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
