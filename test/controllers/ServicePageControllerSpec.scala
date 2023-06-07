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
import base.TestData._
import config.FrontendAppConfig
import errors.{NoPendingReturns, UnexpectedResponseFromSDIL}
import helpers.LoggerHelper
import orchestrators.RegisteredOrchestrator
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, _}
import utilities.GenericLogger
import views.html.ServiceView

class ServicePageControllerSpec extends SpecBase with MockitoSugar with LoggerHelper{

  lazy val mockOrchestrator = mock[RegisteredOrchestrator]
  lazy val mockConfig = mock[FrontendAppConfig]

  val servicePageRoute = routes.ServicePageController.onPageLoad
  def startAReturnRoute(isNilReturn: Boolean) = routes.ServicePageController.startAReturn(isNilReturn)

  "onPageLoad" - {
    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder()
        .overrides(
          bind[RegisteredOrchestrator].toInstance(mockOrchestrator)
        ).build()

      running(application) {
        val request = FakeRequest(GET, servicePageRoute.url)

        val config = application.injector.instanceOf[FrontendAppConfig]

        when(mockOrchestrator.handleServicePageRequest(any(), any(), any())).thenReturn(createSuccessAccountResult(servicePageViewModel1PendingReturns))

        val result = route(application, request).value

        val view = application.injector.instanceOf[ServiceView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(servicePageViewModel1PendingReturns)(request, messages(application), config).toString
      }
    }
  }

  "startAReturn" - {
    "must redirect to sdilReturns when there is a pending return" - {

      "when a request for a non nilReturn is submitted" in {

        val application = applicationBuilder()
          .overrides(
            bind[RegisteredOrchestrator].toInstance(mockOrchestrator)).build()

        running(application) {
          val request = FakeRequest(GET, startAReturnRoute(false).url)
          val config = application.injector.instanceOf[FrontendAppConfig]
          when(mockOrchestrator.handleStartAReturn(any(), any(), any())).thenReturn(createSuccessAccountResult(pendingReturn1))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual config.startReturnUrl(pendingReturn1.year, pendingReturn1.quarter, false)
        }
      }

      "when a request for a nilReturn is submitted" in {

        val application = applicationBuilder()
          .overrides(
            bind[RegisteredOrchestrator].toInstance(mockOrchestrator)).build()

        running(application) {
          val config = application.injector.instanceOf[FrontendAppConfig]

          val request = FakeRequest(GET, startAReturnRoute(true).url)

          when(mockOrchestrator.handleStartAReturn(any(), any(), any())).thenReturn(createSuccessAccountResult(pendingReturn1))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual config.startReturnUrl(pendingReturn1.year, pendingReturn1.quarter, true)
        }
      }
    }

    "must redirect to servicePage" - {
      "when there are no pending returns" in {
        val application = applicationBuilder()
          .overrides(
            bind[RegisteredOrchestrator].toInstance(mockOrchestrator)).build()

        running(application) {
          val request = FakeRequest(GET, startAReturnRoute(false).url)

          when(mockOrchestrator.handleStartAReturn(any(), any(), any())).thenReturn(createFailureAccountResult(NoPendingReturns))
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.ServicePageController.onPageLoad.url
        }
      }
    }

    "must return internal server error page" - {
      "when an internal error occurs" in {
        val application = applicationBuilder()
          .overrides(
            bind[RegisteredOrchestrator].toInstance(mockOrchestrator)).build()

        running(application) {
          val request = FakeRequest(GET, startAReturnRoute(false).url)

          when(mockOrchestrator.handleStartAReturn(any(), any(), any())).thenReturn(createFailureAccountResult(UnexpectedResponseFromSDIL))
          val result = route(application, request).value

          status(result) mustEqual INTERNAL_SERVER_ERROR
        }
      }
    }

    "must log an error when no pending returns" in {
      val application = applicationBuilder()
        .overrides(
          bind[RegisteredOrchestrator].toInstance(mockOrchestrator)).build()

      running(application) {
        withCaptureOfLoggingFrom(application.injector.instanceOf[GenericLogger].logger) { events =>
          val request = FakeRequest(GET, startAReturnRoute(false).url)
          when(mockOrchestrator.handleStartAReturn(any(), any(), any())).thenReturn(createFailureAccountResult(NoPendingReturns))
          await(route(application, request).value)
          events.collectFirst {
            case event =>
              event.getLevel.levelStr mustBe "WARN"
              event.getMessage mustEqual "Unable to start return - no returns pending"
          }.getOrElse(fail("No logging captured"))
        }
      }
    }
  }

}
