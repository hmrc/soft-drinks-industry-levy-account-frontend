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

package controllers

import base.SpecBase
import connectors.DirectDebitConnector
import errors.UnexpectedResponseFromDirectDebit
import models.NextUrl
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._

class DirectDebitControllerSpec extends SpecBase with MockitoSugar {

  "setup" - {
    "must redirect to the url provided by direct debit" in {

      val mockDDConnector = mock[DirectDebitConnector]
      when(mockDDConnector.initJourney()(any())) thenReturn createSuccessAccountResult(NextUrl("http://test"))

      val application =
        applicationBuilder()
          .overrides(bind[DirectDebitConnector].toInstance(mockDDConnector))
          .build()

      running(application) {

        val request = FakeRequest(GET, routes.DirectDebitController.setup().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).get mustBe "http://test"
      }
    }
  }

  "render the error page when the call to direct debit fails" in {
    val mockDDConnector = mock[DirectDebitConnector]
    when(mockDDConnector.initJourney()(any())) thenReturn createFailureAccountResult(UnexpectedResponseFromDirectDebit)

    val application =
      applicationBuilder()
        .overrides(bind[DirectDebitConnector].toInstance(mockDDConnector))
        .build()

    running(application) {

      val request = FakeRequest(GET, routes.DirectDebitController.setup().url)

      val result = route(application, request).value

      status(result) mustEqual INTERNAL_SERVER_ERROR
    }
  }
}
