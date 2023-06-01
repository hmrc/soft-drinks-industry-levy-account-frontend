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

package testSupport

import controllers.ControllerITTestHelper
import testSupport.ITCoreTestData._
import models.{ReturnPeriod, SdilReturn}
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers.{contain, convertToAnyMustWrapper, include}
import play.api.i18n.Messages

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

trait ReturnsITHelper extends ControllerITTestHelper {

  lazy val monthFormatter = DateTimeFormatter.ofPattern("MMMM")
  lazy val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
  lazy val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
  lazy val timeFormatter = DateTimeFormatter.ofPattern("h:mma")

  def validatePage(body: String, pendingReturns: List[ReturnPeriod],
                   lastReturn: Option[SdilReturn]) = {
    val page = Jsoup.parse(body)
    page.title must include(Messages("Your Soft Drinks Industry Levy account"))
    page.getElementsByClass("govuk-caption-m").text() mustBe "Super Lemonade Plc"
    page.getElementsByTag("h1").text() mustBe "Your Soft Drinks Industry Levy account"
    if (pendingReturns.nonEmpty || lastReturn.isDefined) {
      page.getElementsByClass("govuk-heading-m").eachText() must contain("Returns")
      if (pendingReturns.nonEmpty) {
        page.getElementsByClass("govuk-warning-text").text() mustBe warningMessageForPendingReturns(pendingReturns)
        val returnsDue = page.getElementById("overdueReturns")
        pendingReturns.zipWithIndex.foreach { case (pendingReturn, index) =>
          returnsDue.getElementsByTag("li").get(index).text() mustBe overdueBulletMessage(pendingReturn)
        }
      } else {
        page.getElementsByClass("govuk-warning-text").size() mustBe 0
        page.getElementById("lastReturnInset").text() mustBe noReturnsPendingMessage(emptyReturn)
      }
    } else {
      page.getElementsByClass("govuk-heading-m").eachText() mustNot contain("Returns")
    }
  }

  def warningMessageForPendingReturns(pendingReturns: List[ReturnPeriod]): String = {
    if(pendingReturns.size == 1) {
      val pendingReturn = pendingReturns.head
      val startMonth = pendingReturn.start.format(monthFormatter)
      val endDate = pendingReturn.end.format(monthYearFormatter)
      val deadline = pendingReturn.deadline.format(dateFormatter)
      s"! Warning The return for Super Lemonade Plc from $startMonth to $endDate is due by $deadline."
    } else {
      s"! Warning You have ${pendingReturns.size} overdue returns"
    }
  }

  def overdueBulletMessage(returnPeriod: ReturnPeriod): String = {
    val startMonth = returnPeriod.start.format(monthFormatter)
    val endDate = returnPeriod.end.format(monthYearFormatter)
    s"$startMonth to $endDate"
  }
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

}
