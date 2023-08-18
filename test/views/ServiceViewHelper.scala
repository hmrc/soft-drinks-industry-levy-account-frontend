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

import base.TestData._
import models.SdilReturn
import org.jsoup.nodes.Element

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

trait ServiceViewHelper extends ViewSpecHelper {

  lazy val monthFormatter = DateTimeFormatter.ofPattern("MMMM")
  lazy val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
  lazy val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
  lazy val timeFormatter = DateTimeFormatter.ofPattern("h:mma")

  def noReturnsPendingMessage(lastReturn: SdilReturn): String = {
    val submittedOn = lastReturn.submittedOn.getOrElse(Instant.now).atZone(ZoneId.of("Europe/London"))
    val lastPeriodStart = pendingReturn1.start.format(monthFormatter)
    val lastPeriodEnd = pendingReturn1.end.format(monthYearFormatter)
    val submittedTime = submittedOn.format(timeFormatter).toLowerCase
    val submittedDate = submittedOn.format(dateFormatter)
    val currentPeriodStart = currentReturnPeriod.start.format(monthFormatter)
    val currentPeriodEnd = currentReturnPeriod.end.format(monthYearFormatter)
    val nextReturnDueDate = currentReturnPeriod.deadline.format(dateFormatter)

    s"Your return for $lastPeriodStart to $lastPeriodEnd was submitted at $submittedTime on $submittedDate." +
      s" Your next return will be for $currentPeriodStart to $currentPeriodEnd." +
      s" You must submit this return and make any payments due by $nextReturnDueDate."
  }

  def testReturnDetailsSection(element: Element) = {
    val detailsSummary = element.getElementsByClass("govuk-details__summary")
    val detailsContent = element.getElementsByClass("govuk-details__text").get(0)

    "that has the expected details summary" in {
      detailsSummary.text() mustEqual "What you will need to tell us in your return"
    }

    "that has the expected content" - {
      "which includes you need to tell about liable drinks message" in {
        detailsContent.getElementById("needInReturnP1").text() mustBe "You will need to tell us how many litres of liable drink in each band you need to report that were:"
      }

      "which includes the list of liable drinks" in {
        val liableDrinksList = detailsContent.getElementById("needInReturnLiable").getElementsByTag("li")
        liableDrinksList.size() mustBe 4
        liableDrinksList.get(0).text() mustBe "packaged for your own brand or as a contract packer, not including those packaged for small producers"
        liableDrinksList.get(1).text() mustBe "packaged for each small producer and the reference number of each small producer"
        liableDrinksList.get(2).text() mustBe "brought into the UK from anywhere outside of the UK, not including those from small producers"
        liableDrinksList.get(3).text() mustBe "brought into the UK from small producers"
      }

      "which includes a message about diluted drinks" in {
        detailsContent.getElementById("needInReturnP2").text() mustBe "If the drink is dilutable, you must report the amount of ready-to-drink litres made when it is diluted according to the dilution ratio stated on the packaging."
      }

      "which includes a bold message about claiming credit" in {
        element.getElementsByClass("govuk-body govuk-!-font-weight-bold").text() mustBe "You can only claim credit if you've registered for the levy and paid it directly to HMRC. Claiming credits you're not entitled to is a criminal offence."
      }

      "which includes you need to tell about liable credit drinks message" in {
        detailsContent.getElementById("needInReturnP4").text() mustBe "You can claim a credit for liable drinks that you have reported in either a previous or the current return that have been:"
      }

      "which includes the list of liable credit drinks" in {
        val creditBulletList = detailsContent.getElementById("needInReturnCredit").getElementsByTag("li")
        creditBulletList.size() mustBe 2
        creditBulletList.get(0).text() mustBe "exported, or are expected to be exported"
        creditBulletList.get(1).text() mustBe "lost or destroyed"
      }

      "which includes a link to govuk guidance" in {
        val link = element.getElementsByClass("govuk-link").get(0)
        link.text() mustBe "You must get and keep certain types of evidence to be able to claim a credit (opens in a new tab)."
        link.attr("href") mustBe "https://www.gov.uk/guidance/soft-drinks-industry-levy-credit-for-exported-lost-or-destroyed-drinks-notice-4"
      }
    }
  }



}
