/*
 * Copyright 2026 HM Revenue & Customs
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

package service

import Service.SdilSubscriptionService
import base.{SpecBase, TestData}
import connectors.SoftDrinksIndustryLevyConnector
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import base.TestData.aSubscription
import errors.UnexpectedResponseFromSDIL
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import cats.data.EitherT

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class SdilSubscriptionServiceSpec extends SpecBase with MockitoSugar{

  val mockConnector = mock[SoftDrinksIndustryLevyConnector]
  val service = new SdilSubscriptionService(mockConnector)

  "resolveActiveSdilRef" - {
    "return first SDIL ref when it is active" in {
      val active = TestData.aSubscription.copy (deregDate = None)
      when (
        mockConnector.retrieveSubscription("sdilRef1","sdil","intId")
      ).thenReturn(
        EitherT.fromEither[Future](Right(Some(active)))
      )

      val result = service.resolveActiveSdilRef(
        Seq("sdilRef1","sdilRef2"),"intId"
      )

      result.futureValue mustBe Some("sdilRef1")
    }

    "skip expired ref and return next active one" in {
      val expired = TestData.aSubscription.copy(deregDate = Some(LocalDate.now.minusDays(1)))
      val  active= TestData.aSubscription.copy (deregDate = None)
      when(
        mockConnector.retrieveSubscription("sdilRef1", "sdil", "intId")
      ).thenReturn(EitherT.fromEither[Future](Right(Some(expired))))
      when(
        mockConnector.retrieveSubscription("sdilRef2", "sdil", "intId")
      ).thenReturn(EitherT.fromEither[Future](Right(Some(active))))

      val result = service.resolveActiveSdilRef(
        Seq("sdilRef1", "sdilRef2"), "intId"
      )

      result.futureValue mustBe Some("sdilRef2")
    }

    "return None if all refs are expired" in {
      val expired = TestData.aSubscription.copy(deregDate = Some(LocalDate.now.minusDays(1)))

      when(
        mockConnector.retrieveSubscription("sdilRef1", "sdil", "intId")
      ).thenReturn(EitherT.fromEither[Future](Right(Some(expired))))

      when(
        mockConnector.retrieveSubscription("sdilRef2", "sdil", "intId")
      ).thenReturn(EitherT.fromEither[Future](Right(Some(expired))))

      val result = service.resolveActiveSdilRef(
        Seq("sdilRef1","sdilRef2"), "intId"
      )

      result.futureValue mustBe None
    }

    "return None if no refs provided" in {

      val result = service.resolveActiveSdilRef(
        Seq.empty, "intId"
      )
      result.futureValue mustBe None
    }

    "skip ref if connector returns error" in {
      val active = TestData.aSubscription.copy(deregDate = None)
      when(
        mockConnector.retrieveSubscription("sdilRef1", "sdil", "intId")
      ).thenReturn(EitherT.fromEither[Future](Left(UnexpectedResponseFromSDIL)))
      when(
        mockConnector.retrieveSubscription("sdilRef2", "sdil", "intId")
      ).thenReturn(EitherT.fromEither[Future](Right(Some(active))))

      val result = service.resolveActiveSdilRef(
        Seq("sdilRef1", "sdilRef2"), "intId"
      )

      result.failed.futureValue mustBe a[Exception]
    }
  }

  "isActive" - {
    "return true when deregistrationDate is None" in {
      val sub = aSubscription.copy(
        deregDate = None
      )
      service.isActive(sub) mustBe true
    }
    "return false when deregistrationDate is in the past" in {
      val sub = aSubscription.copy(
        deregDate = Some(LocalDate.now.minusDays(1))
      )
      service.isActive(sub) mustBe false
    }
    "return true when deregistrationDate is in the past" in {
      val sub = aSubscription.copy(
        deregDate = Some(LocalDate.now.plusDays(5))
      )
      service.isActive(sub) mustBe true
    }
  }


}
