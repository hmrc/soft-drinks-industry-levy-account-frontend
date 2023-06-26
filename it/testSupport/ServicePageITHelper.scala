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
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers.{contain, convertToAnyMustWrapper, include}
import play.api.i18n.Messages

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

trait ServicePageITHelper extends ControllerITTestHelper {

  lazy val monthFormatter = DateTimeFormatter.ofPattern("MMMM")
  lazy val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
  lazy val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
  lazy val timeFormatter = DateTimeFormatter.ofPattern("h:mma")

  def validatePage(body: String, pendingReturns: List[ReturnPeriod],
                   lastReturn: Option[SdilReturn],
                   balance: BigDecimal,
                   interest: BigDecimal,
                   hasDirectDebit: Boolean) = {
    val page = Jsoup.parse(body)
    page.title must include(Messages("Your Soft Drinks Industry Levy account"))
    page.getElementsByClass("govuk-caption-m").text() mustBe "Super Lemonade Plc"
    page.getElementsByTag("h1").text() mustBe "Your Soft Drinks Industry Levy account"
    validateReturnsSection(page, pendingReturns, lastReturn)
    validateAccountBalance(page, balance, interest, hasDirectDebit, pendingReturns)
  }

  def validateReturnsSection(page: Document, pendingReturns: List[ReturnPeriod],
                             lastReturn: Option[SdilReturn]) = {
    val buttons = page.getElementsByClass("govuk-button")
    if (pendingReturns.nonEmpty || lastReturn.isDefined) {
      page.getElementsByClass("govuk-heading-m").eachText() must contain("Returns")
      if (pendingReturns.nonEmpty) {
        page.getElementsByClass("govuk-warning-text").text() mustBe warningMessageForPendingReturns(pendingReturns)
        val returnsDue = page.getElementById("overdueReturns")
        pendingReturns.zipWithIndex.foreach { case (pendingReturn, index) =>
          returnsDue.getElementsByTag("li").get(index).text() mustBe overdueBulletMessage(pendingReturn)
        }
        buttons.eachText() must contain("Start a return")
      } else {
        page.getElementsByClass("govuk-warning-text").size() mustBe 0
        page.getElementById("lastReturnInset").text() mustBe noReturnsPendingMessage(emptyReturn)
        buttons.eachText() mustNot contain("Start a return")
      }
    } else {
      page.getElementsByClass("govuk-heading-m").eachText() mustNot contain("Returns")
      buttons.eachText() mustNot contain("Start a return")

    }
  }
  def validateAccountBalance(page: Document,
                             balance: BigDecimal,
                             interest: BigDecimal,
                             hasDirectDebit: Boolean,
                             pendingReturns: List[ReturnPeriod]) = {
    page.getElementsByClass("govuk-heading-m").eachText() must contain("Account balance")
    val formattedBalance = f"£${balance.abs}%,.2f"
    val formattedInterest = f"£${interest.abs}%,.2f"
    val buttons = page.getElementsByClass("govuk-button govuk-button--secondary")
    if(balance == 0) {
      page.getElementById("balanceZero").text() mustBe "Your balance is £0."
      buttons.eachText() mustNot contain("Pay now")
    } else if(balance > 0) {
      page.getElementById("balanceCredit").text() mustBe s"You are ${formattedBalance} in credit."
      buttons.eachText() mustNot contain("Pay now")
    } else {
      if(interest < 0){
        page.getElementById("balanceNeedToPayWithInterest").text() mustBe s"Your balance is ${formattedBalance} including ${formattedInterest} of interest."
      } else {
        page.getElementById("balanceNeedToPayNoInterest").text() mustBe s"Your balance is ${formattedBalance}."
      }
      if(pendingReturns.nonEmpty) {
        page.getElementById("payBy").text() mustBe s"You need to pay by ${pendingReturn3.deadline.format(dateFormatter)}."
      }
      page.getElementById("otherPaymentOptions").text() mustBe "If you choose to make payments outside of this online account, you will need to take note of your Soft Drinks Levy reference XKSDIL000000022. Find out other ways to pay the levy (opens in a new tab)."
      page.getElementById("delayInAccountUpdate").text() mustBe "Your account balance may not reflect payments made in the last 3 days."
      buttons.eachText() must contain("Pay now")
    }

    if(hasDirectDebit) {
      page.getElementById("manageExistingDDSubHeader").text() mustBe "How you pay the levy"
      page.getElementById("manageExistingDD").text() mustBe "You have set up a Direct Debit to pay the levy. It will not collect any interest and penalties you owe."
      val manageExistingLink = page.getElementById("manageExistingDDLink")
      manageExistingLink.text() mustBe "Change or cancel your Direct Debit and view your next payment collection date"
      manageExistingLink.getElementsByTag("a").attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/start-direct-debit-journey"
    } else {
      page.getElementById("manageSetupDDSubHeader").text() mustBe "Pay by Direct Debit"
      val manageSetupDDContent = page.getElementById("manageSetupDD")
      manageSetupDDContent.text() mustBe "You can set up a Direct Debit to pay the Soft Drinks Industry Levy. You need to do this at least 3 working days before your return and payment due date."
      manageSetupDDContent.getElementsByTag("a").attr("href") mustBe "https://www.gov.uk/guidance/pay-the-soft-drinks-industry-levy-notice-5"
    }

    val transHistoryLink = page.getElementById("viewTransactionHistory")
    transHistoryLink.text() mustBe "View your transaction history"
    transHistoryLink.attr("href") mustBe "#"

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
