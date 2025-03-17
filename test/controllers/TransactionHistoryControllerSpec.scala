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
import errors.UnexpectedResponseFromSDIL
import helpers.LoggerHelper
import models._
import orchestrators.RegisteredOrchestrator
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.TransactionHistoryView
import java.time.LocalDate

class TransactionHistoryControllerSpec extends SpecBase with MockitoSugar with LoggerHelper{

  lazy val mockOrchestrator = mock[RegisteredOrchestrator]
  lazy val mockConfig = mock[FrontendAppConfig]

  val transactionHistoryRoute = routes.TransactionHistoryController.onPageLoad

  val year = 2022
  val date1 = LocalDate.of(year, 12, 1)
  val date2 = LocalDate.of(year, 6, 20)
  val date3 = LocalDate.of(year, 1, 30)

  val fi1 = PaymentOnAccount(date1, "test", BigDecimal(132.00))
  val fi2 = ReturnCharge(ReturnPeriod.apply(date2), BigDecimal(-120.00))
  val fi3 = ReturnChargeInterest(date3, BigDecimal(-12.00))

  val expectedTransactionHistoryItems = List(
    TransactionHistoryItem(fi1, BigDecimal(0.00)),
    TransactionHistoryItem(fi2, BigDecimal(-132.00)),
    TransactionHistoryItem(fi3, fi3.amount)
  )
  val transactionHistoryForYears = Map(year -> expectedTransactionHistoryItems)

  "onPageLoad" - {
    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder()
        .overrides(
          bind[RegisteredOrchestrator].toInstance(mockOrchestrator)
        ).build()

      running(application) {
        val request = FakeRequest(GET, transactionHistoryRoute.url)

        val config = application.injector.instanceOf[FrontendAppConfig]

        when(mockOrchestrator.getTransactionHistoryForAllYears(any(), any(), any())).thenReturn(createSuccessAccountResult(transactionHistoryForYears))

        val result = route(application, request).value

        val view = application.injector.instanceOf[TransactionHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(aSubscription.orgName, transactionHistoryForYears)(request, messages(application), config).toString
      }
    }

    "must return internal server error page" - {
      "when an internal error occurs" in {
        val application = applicationBuilder()
          .overrides(
            bind[RegisteredOrchestrator].toInstance(mockOrchestrator)).build()

        running(application) {
          val request = FakeRequest(GET, transactionHistoryRoute.url)

          when(mockOrchestrator.getTransactionHistoryForAllYears(any(), any(), any())).thenReturn(createFailureAccountResult(UnexpectedResponseFromSDIL))
          val result = route(application, request).value

          status(result) mustEqual INTERNAL_SERVER_ERROR
        }
      }
    }
  }

}
