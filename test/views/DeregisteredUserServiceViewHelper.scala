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

package views

import base.TestData.{deregDate, emptyReturn, localDate, submittedDateTime}
import models.ReturnPeriod
import org.jsoup.nodes.Element

trait DeregisteredUserServiceViewHelper extends ServiceViewHelper {

  def testNotificationBanner(notificationBanner: Element, sentFinalReturn: Boolean) = {
    "that contains the expected header" in {
      val notificationHeader = notificationBanner.getElementsByClass("govuk-notification-banner__header").first()
      notificationHeader.text() mustBe "Important"
    }

    "that has content" - {
      val content = notificationBanner.getElementsByClass("govuk-notification-banner__content").first()

      "with the expected subheading" in {
        val heading = content.getElementsByClass("govuk-notification-banner__heading").first()
        val expectedHeader = if (sentFinalReturn) {
          "This account is no longer registered with the Soft Drinks Industry Levy."
        } else {
          "Your request to cancel your registration is on hold."
        }
        heading.text() mustBe expectedHeader
      }

      "with the expected content" in {
        val notificationContent = notificationBanner.getElementsByClass("govuk-body")
        val expectedContent = if (sentFinalReturn) {
          val formattedDeregDate = deregDate.format(dateFormatter)
          val formattedAccessToDate = deregDate.plusYears(7L).format(dateFormatter)
          s"The registration was cancelled on the $formattedDeregDate. You will be able to access this account until $formattedAccessToDate."
        } else {
          val deregReturnPeriod = ReturnPeriod(deregDate)
          val formattedDeregStart = deregReturnPeriod.start.format(dateFormatter)
          val formattedDeregEnd = deregReturnPeriod.end.format(dateFormatter)
          val deregReturnPeriodNext = deregReturnPeriod.next
          val formattedDeregNextStart = deregReturnPeriodNext.start.format(dateFormatter)
          val formattedDeregNextEnd = deregReturnPeriodNext.end.format(dateFormatter)
            s"Before we can cancel your registration, you must send a final return for the current period," +
            s" $formattedDeregStart to $formattedDeregEnd and make any outstanding payments." +
            s" You will be able to send the final return from $formattedDeregNextStart until $formattedDeregNextEnd." +
            s" You will be able to access your account for 7 years from when you send your final return." +
            s" If you do not want to cancel your registration, call the Soft Drinks Industry Levy helpline on 0300 200 3700."
        }
        notificationContent.text() mustBe expectedContent
      }
    }

  }

  def testOverdueFinalReturnSection(document: Element) = {
    "should include a section to send final return" - {
      "that has the expected header" in {
        val header = document.getElementById("sendFinalReturn")
        header.text() mustBe "Send final return"
        header.className() mustBe "govuk-heading-m"
      }

      "that has the expected content" in {
        val content = document.getElementById("sendFinalReturnParagraph")
        val deregReturnPeriod = ReturnPeriod(deregDate)
        val deregPeriodStart = deregReturnPeriod.previous.start.format(monthYearFormatter)
        val deregPeriodEnd = deregReturnPeriod.previous.end.format(monthYearFormatter)
        content.text() mustBe s"You must send a return for $deregPeriodStart to $deregPeriodEnd before we can cancel your registration."
      }
      "that includes a details section" - {
        val details = document.getElementById("needInReturnDetails")
        testReturnDetailsSection(details)
      }

      "that includes a button to start a return" in {
        val button = document.getElementsByClass("govuk-button").get(0)
        button.text() mustBe "Send a final return"
        button.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/start-a-return/nilReturn/false"
      }

      "that includes a link to send a nil return" in {
        val nilReturnLink = document.getElementById("startNilReturn")
        nilReturnLink.text() mustBe "I have no activity to report for this quarter."
        nilReturnLink.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/start-a-return/nilReturn/true"
      }
    }
  }

  def testFinalReturnDueInFutureSection(document: Element) = {
    "should contain content about sending final return when due" in {
      document.getElementById("lastReturnInset").text() mustBe noReturnsPendingMessage(emptyReturn)
    }
  }


  def testFinalAndLastReturnSentSection(document: Element) = {
    "should contain content about final return sent" in {
      val currentReturnPeriod = ReturnPeriod(localDate)
      val lastPeriodStart = currentReturnPeriod.previous.start.format(monthFormatter)
      val lastPeriodEnd = currentReturnPeriod.previous.end.format(monthYearFormatter)
      val submittedTime = submittedDateTime.format(timeFormatter).toLowerCase
      val submittedDate = submittedDateTime.format(dateFormatter)
      val expectedText = s"Your return for $lastPeriodStart to $lastPeriodEnd was submitted at $submittedTime on $submittedDate."
      document.getElementById("finalReturnCompleted").text() mustBe expectedText
    }
  }

}
