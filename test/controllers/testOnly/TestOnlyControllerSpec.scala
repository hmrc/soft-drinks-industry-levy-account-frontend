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

package controllers.testOnly

import base.SpecBase
import config.FrontendAppConfig
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers._

class TestOnlyControllerSpec extends SpecBase with MockitoSugar {

  val mockConfig = mock[FrontendAppConfig]
  val controller = new TestOnlyController(mcc, mockConfig)

  "stubDirectDebitJourney" - {
    "must redirect to the sdil home" in {
      when(mockConfig.homePage).thenReturn("http://test.com")
      val res = controller.stubDirectDebitJourney().apply(FakeRequest("", ""))

      status(res) mustEqual SEE_OTHER
      redirectLocation(res).get mustBe "http://test.com"
    }
  }

  "stubDirectDebitInitialise" - {
    "must redirect to the sdil home" in {
      val res = controller.stubDirectDebitInitialise()
        .apply(FakeRequest("", ""))

      status(res) mustEqual OK
      contentAsString(res) must include(routes.TestOnlyController.stubDirectDebitJourney().url)
    }
  }

  "stubPayApiJourney" - {
    "must redirect to the sdil home" in {
      when(mockConfig.homePage).thenReturn("http://test.com")
      val res = controller.stubPayApiJourney().apply(FakeRequest("", ""))

      status(res) mustEqual SEE_OTHER
      redirectLocation(res).get mustBe "http://test.com"
    }
  }

  "stubPayApiInitialise" - {
    "must redirect to the sdil home" in {
      val res = controller.stubPayApiInitialise()
        .apply(FakeRequest("", ""))

      status(res) mustEqual OK
      contentAsString(res) must include(routes.TestOnlyController.stubPayApiJourney().url)
    }
  }

}
