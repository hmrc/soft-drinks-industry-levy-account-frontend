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
                   hasDirectDebit: Boolean,
                   isSmallProducer: Boolean = false) = {
    val page = Jsoup.parse(body)
    page.title must include(Messages("Your Soft Drinks Industry Levy account"))
    page.getElementById("caption").text() mustBe "Company is Super Lemonade Plc"
    page.getElementsByTag("h1").text() mustBe "Your Soft Drinks Industry Levy account"
    if(isSmallProducer) {
      validateSmallProducer(page)
    } else {
      validateReturnsSection(page, pendingReturns, lastReturn)
    }
    validateAccountBalance(page, balance, interest, hasDirectDebit, pendingReturns)
    validateManageAccount(page)
    validateBusinessDetails(page)
    validateNeedNelp(page)
  }

  def validatePageDeregistered(body: String, hasVariableReturns: Boolean,
                               sentFinalReturn: Boolean,
                               optLastReturn: Option[SdilReturn],
                                balance: BigDecimal) = {
    val page = Jsoup.parse(body)
    page.title must include(Messages("Your Soft Drinks Industry Levy account"))
    page.getElementsByTag("h1").text() mustBe "Super Lemonade Plc Your Soft Drinks Industry Levy account"
    validateNotificationBanner(page, sentFinalReturn)
    optLastReturn match{
      case Some(_) if sentFinalReturn =>
        validateFinalAndLastReturnSentSection(page)
      case Some(lastReturn) => page.getElementById("lastReturnInset").text() mustBe noReturnsPendingMessage(lastReturn)
      case None if sentFinalReturn => page.toString mustNot include("Send final return")
      case None => validateSendFinalReturnWhenOverdue(page)
    }
    validateAccountBalance(page, balance, sentFinalReturn)
    if(hasVariableReturns) {
      validateManageAccount(page, true)
    } else {
      page.getElementsByClass("govuk-heading-m").eachText() mustNot contain("Manage your account")
    }
    validateBusinessDetails(page)
    validateNeedNelp(page)
  }

  def validateSmallProducer(page: Document) = {
    val buttons = page.getElementsByClass("govuk-button")
    page.getElementsByClass("govuk-heading-m").eachText() mustNot contain("Returns")
    buttons.eachText() mustNot contain("Start a return")
    val expectedText = "You are registered as a small producer," +
      " so you do not have to send returns." +
      " Make sure you give your reference number to your third-party packagers" +
      " so they will not have to pay the levy for your drinks." +
      " If you stop being a small producer," +
      " you must update your registered details and send a return on time." +
      " After changing your details, allow 15 days for your Soft Drinks Industry Levy account to be updated." +
      " You will also receive a new reference number."
    page.getElementById("voluntaryOnly").text() mustBe expectedText
  }

  def validateNotificationBanner(page: Document, sentFinalReturn: Boolean) = {
    val notificationBanner = page.getElementsByClass("govuk-notification-banner").first()
    val expectedHeading = if (sentFinalReturn) {
      "This account is no longer registered with the Soft Drinks Industry Levy."
    } else {
      "Your request to cancel your registration is on hold."
    }
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

    notificationBanner.getElementsByClass("govuk-notification-banner__heading").first().text() mustBe expectedHeading
    notificationBanner.getElementsByClass("govuk-body").text() mustBe expectedContent
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

  def validateSendFinalReturnWhenOverdue(page: Document) = {
    val deregReturnPeriod = ReturnPeriod(deregDate)
    val deregPeriodStart = deregReturnPeriod.previous.start.format(monthYearFormatter)
    val deregPeriodEnd = deregReturnPeriod.previous.end.format(monthYearFormatter)
    page.getElementById("sendFinalReturn").text() mustBe "Send final return"
    page.getElementById("sendFinalReturnParagraph").text() mustBe s"You must send a return for $deregPeriodStart to $deregPeriodEnd before we can cancel your registration."
    page.getElementsByClass("govuk-button").get(0).text() mustBe "Send a final return"
  }

  def validateFinalAndLastReturnSentSection(page: Document) = {
      val currentReturnPeriod = ReturnPeriod(localDate)
      val lastPeriodStart = currentReturnPeriod.previous.start.format(monthFormatter)
      val lastPeriodEnd = currentReturnPeriod.previous.end.format(monthYearFormatter)
      val submittedTime = submittedDateTime.format(timeFormatter).toLowerCase
      val submittedDate = submittedDateTime.format(dateFormatter)
      val expectedText = s"Your return for $lastPeriodStart to $lastPeriodEnd was submitted at $submittedTime on $submittedDate."
      page.getElementById("finalReturnCompleted").text() mustBe expectedText
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
      buttons.eachText() mustNot contain("Make a payment")
    } else if(balance > 0) {
      page.getElementById("balanceCredit").text() mustBe s"You are ${formattedBalance} in credit."
      buttons.eachText() mustNot contain("Make a payment")
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
      buttons.eachText() must contain("Make a payment")
    }

    if(hasDirectDebit) {
      page.getElementById("manageExistingDDSubHeader").text() mustBe "How you pay the levy"
      page.getElementById("manageExistingDD").text() mustBe "You have set up a Direct Debit to pay the levy. It will not collect any interest and penalties you owe."
      val manageExistingLink = page.getElementById("manageExistingDDLink")
      manageExistingLink.text() mustBe "Change or cancel your Direct Debit and view your next payment collection date"
      manageExistingLink.getElementsByTag("a").attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/start-direct-debit-journey"
    } else {
      val manageSetupDDContent = page.getElementById("manageSetupDD")
      manageSetupDDContent.text() mustBe "You can set up a Direct Debit to pay the Soft Drinks Industry Levy. You need to do this at least 3 working days before your return and payment due date."
      manageSetupDDContent.getElementsByTag("a").attr("href") mustBe "https://www.gov.uk/guidance/pay-the-soft-drinks-industry-levy-notice-5"
    }

    val transHistoryLink = page.getElementById("viewTransactionHistory")
    transHistoryLink.text() mustBe "View your transaction history"
    transHistoryLink.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/transaction-history"

  }

def validateAccountBalance(page: Document,
                           balance: BigDecimal,
                           sentFinalReturn: Boolean) = {
  page.getElementsByClass("govuk-heading-m").eachText() must contain("Account balance")
  val expectedContent = if (balance < 0) {
    "Your balance is £100.00."
  } else if (balance > 0 && sentFinalReturn) {
    "Your balance is £100.00 in credit. Call the Soft Drinks Industry Levy helpline to arrange repayment."
  } else if (balance > 0) {
    "You are £100.00 in credit. This account is closed."
  } else {
    "Your balance is £0. This account is closed."
  }
  page.getElementsByClass("govuk-body").text() must include(expectedContent)

  val transHistoryLink = page.getElementById("viewTransactionHistory")
  transHistoryLink.text() mustBe "View your transaction history"
  transHistoryLink.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/transaction-history"

}

  def validateManageAccount(page: Document, isDeregistered: Boolean = false) = {
    page.getElementsByClass("govuk-heading-m").eachText() must contain("Manage your account")
    if (isDeregistered) {
      page.getElementsByClass("govuk-button govuk-button--secondary").text() mustBe "Correct an error in a previous return"
    } else {
      page.getElementById("manageYourAccountChangesTellWhen").text() mustBe "You should tell HMRC when you:"
      val listItems = page.getElementById("manageYourAccountChanges").getElementsByTag("li")
      listItems.size() mustBe 4
      listItems.eachText().get(0) mustBe "update contact, address, packaging site or warehouse details"
      listItems.eachText().get(1) mustBe "change the amount of liable drinks produced"
      listItems.eachText().get(2) mustBe "cancel Soft Drinks Industry Levy registration"
      listItems.eachText().get(3) mustBe "correct an error in a previous return"
    }
  }

  def validateBusinessDetails(page: Document) = {
    page.getElementsByClass("govuk-heading-m").eachText() must contain("Business details")
    val businessDetailsForUTR = page.getElementById("businessDetailsForUTR")
    businessDetailsForUTR.text() mustBe s"These are the details we hold for Unique Taxpayer Reference (UTR) ${aSubscription.utr}:"
    val address = page.getElementById("businessAddress")
    address.className mustBe "govuk-inset-text"
    address.text() mustBe "Super Lemonade Plc 63 Clifton Roundabout Worcester WR53 7CX"
  }

  def validateNeedNelp(page: Document) = {
    page.getElementsByClass("govuk-heading-m").eachText() must contain("Need help")
    val link1 = page.getElementById("sdilGuidance")
    link1.text() mustBe "Soft Drinks Industry Levy guidance (opens in a new tab)"
    link1.getElementsByTag("a").attr("href") mustBe "https://www.gov.uk/topic/business-tax/soft-drinks-industry-levy"
    val link2 = page.getElementById("sdilRegulations")
    link2.text() mustBe "The Soft Drinks Industry Levy Regulations 2018 (opens in a new tab)"
    link2.getElementsByTag("a").attr("href") mustBe "https://www.legislation.gov.uk/uksi/2018/41/made"
    val link3 = page.getElementById("sdilContact")
    link3.text() mustBe "Contact HMRC about your Soft Drinks Industry Levy (opens in a new tab)"
    link3.getElementsByTag("a").attr("href") mustBe "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/soft-drinks-industry-levy"
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
    val submittedOn = lastReturn.submittedOn.map(_.atZone(ZoneId.of("Europe/London"))).getOrElse(Instant.now().atZone(ZoneId.of("Europe/London")))
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
