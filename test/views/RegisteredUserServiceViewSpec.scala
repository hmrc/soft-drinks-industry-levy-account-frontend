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
import config.FrontendAppConfig
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.ServiceView

class RegisteredUserServiceViewSpec extends RegisteredUserServiceViewHelper {

  val application = applicationBuilder().build()
  val view = application.injector.instanceOf[ServiceView]
  implicit val request: Request[_] = FakeRequest()
  implicit val config: FrontendAppConfig = application
    .injector.instanceOf[FrontendAppConfig]

  object Selectors {
    val heading = "govuk-heading-m"
    val body = "govuk-body"
    val warning = "govuk-warning-text"
    val li = "li"
    val button = "govuk-button"
  }

  val htmlNoReturn = view(registeredUserServicePageViewModelWithNoReturnInfo)(request, messages(application), config)
  val documentNoReturn = doc(htmlNoReturn)

  val html1Return = view(registeredUserServicePageViewModel1PendingReturns)(request, messages(application), config)
  val document1Return = doc(html1Return)

  val html3Return = view(registeredUserServicePageViewModel3PendingReturns)(request, messages(application), config)
  val document3Return = doc(html3Return)

  val htmlWithLastReturn = view(registeredUserServicePageViewModelWithLastReturn)(request, messages(application), config)
  val documentLastReturn = doc(htmlWithLastReturn)

  val htmlForVolunaryRegistration = view(registeredUserServicePageViewModelForVoluntaryRegistration)(request, messages(application), config)
  val documentForVolunaryRegistration = doc(htmlForVolunaryRegistration)

  "View" - {
    "should contain the expected title" in {
      documentNoReturn.title() must include("Your Soft Drinks Industry Levy account")
    }

    "should include a caption containing the orgName" in {
      documentNoReturn.getElementById("caption").text() mustBe "Company is Super Lemonade Plc"
    }

    "should include the expected h1 heading" in {
      documentNoReturn.getElementsByTag("h1").text() mustBe "Your Soft Drinks Industry Levy account"
    }

    "should include inset text for about being a small producer and not requiring to send a return" - {
      "when the user has voluntary registration activity" in {
        val insetText = documentForVolunaryRegistration.getElementById("voluntaryOnly")
        insetText.className() mustBe "govuk-inset-text"
        insetText.text() mustBe "You are registered as a small producer," +
          " so you do not have to send returns." +
          " Make sure you give your reference number to your third-party packagers" +
          " so they will not have to pay the levy for your drinks." +
          " If you stop being a small producer," +
          " you must update your registered details and send a return on time." +
          " After changing your details, allow 15 days for your Soft Drinks Industry Levy account to be updated." +
          " You will also receive a new reference number."
        insetText.getElementsByTag("a").attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/make-a-change"
      }
    }

    "should include a returns section" - {
      "when there is 1 return pending" - {
        "that has the return header" in {
          document1Return.getElementsByClass(Selectors.heading).eachText() must contain("Returns")
        }
        "that includes the expected warning" in {
          document1Return.getElementsByClass(Selectors.warning).text() mustBe warningMessageForPendingReturns(pendingReturns1)
        }
        "that includes details of pending return" - {
          "which has a list of returns due" in {
            document1Return.getElementsByClass(Selectors.body).eachText() must contain("You must send your 1 overdue returns in this order, oldest first:")
            val returnsDue = document1Return.getElementById("overdueReturns")
            returnsDue.getElementsByTag(Selectors.li).get(0).text() mustBe overdueBulletMessage(pendingReturns1.head)
          }
          "which includes a message to send the return" in {
            document1Return.getElementsByClass(Selectors.body).eachText() must contain("You need to send a return to HMRC every quarter, even if you have nothing to report for the period.")
          }
        }

        "includes a details section" - {
          val details = document1Return.getElementById("needInReturnDetails")
          testReturnDetailsSection(details)
        }

        "includes a button to start a return" in {
          val button = document1Return.getElementsByClass(Selectors.button).get(0)
          button.text() mustBe "Start a return"
          button.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/start-a-return/nilReturn/false"
        }

        "includes a link to send a nil return" in {
          val nilReturnLink = document1Return.getElementById("startNilReturn")
          nilReturnLink.text() mustBe "I have no activity to report for this quarter."
          nilReturnLink.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/start-a-return/nilReturn/true"
        }
      }

      "when there is more than 1 return pending" - {
        "that has the return header" in {
          document3Return.getElementsByClass(Selectors.heading).eachText() must contain("Returns")
        }
        "that includes the expected warning" in {
          document3Return.getElementsByClass(Selectors.warning).text() mustBe warningMessageForPendingReturns(pendingReturns3)
        }
        "that includes details of pending return" - {
          "which has a list of returns due" in {
            document3Return.getElementsByClass(Selectors.body).eachText() must contain("You must send your 3 overdue returns in this order, oldest first:")
            val returnsDue = document3Return.getElementById("overdueReturns")
            pendingReturns3.zipWithIndex.foreach { case (pendingReturn, index) =>
              returnsDue.getElementsByTag(Selectors.li).get(index).text() mustBe overdueBulletMessage(pendingReturn)
            }
          }
          "which includes a message to send the return" in {
            document3Return.getElementsByClass(Selectors.body).eachText() must contain("You need to send a return to HMRC every quarter, even if you have nothing to report for the period.")
          }
        }
        "includes a details section" - {
          val details = document3Return.getElementById("needInReturnDetails")
          testReturnDetailsSection(details)
        }

        "includes a button to start a return" in {
          val button = document3Return.getElementsByClass(Selectors.button).get(0)
          button.text() mustBe "Start a return"
          button.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/start-a-return/nilReturn/false"
        }

        "includes a link to send a nil return" in {
          val nilReturnLink = document3Return.getElementById("startNilReturn")
          nilReturnLink.text() mustBe "I have no activity to report for this quarter."
          nilReturnLink.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/start-a-return/nilReturn/true"
        }

      }

      "when there is no return pending but a lastReturn present" - {
        "that has the return header" in {
          documentLastReturn.getElementsByClass(Selectors.heading).eachText() must contain("Returns")
        }
        "that does not include a warning" in {
          documentLastReturn.getElementsByClass(Selectors.warning).size() mustEqual 0
        }

        "that includes an inset text with details of last return submitted" in {
          val insetText = documentLastReturn.getElementById("lastReturnInset")
          insetText.text() mustBe noReturnsPendingMessage(emptyReturn)
        }

        "that does not include a send a return button" in {
          val buttons = documentLastReturn.getElementsByClass(Selectors.button)
          buttons.eachText() mustNot contain("Start a return")
        }

        "that does not contain a link to send a nil return" in {
          documentLastReturn.getElementsByClass("govuk-link").eachText() mustNot contain("I have no activity to report for this quarter.")
        }
      }
    }

    "should not include a returns section" - {
      "when the user has voluntaryRegistration activity" in {
        documentForVolunaryRegistration.getElementsByClass(Selectors.heading).eachText() mustNot contain("Returns")
      }
      "when there is no pending returns or return submitted for previous period" in {
        documentNoReturn.getElementsByClass(Selectors.heading).eachText() mustNot contain("Returns")
      }
    }

    "should include an account balance section" - {
      "that has the expected subheader" in {
        document1Return.getElementsByClass(Selectors.heading).eachText() must contain("Account balance")
      }
      "when the users balance is negative, has no interest, has a DD setup and has a return pending" - {
        val balance = BigDecimal(-123.45)
        val interest = BigDecimal(0)
        val viewModel = registeredUserServicePageViewModel(pendingReturns1, None, balance, interest, Some(true))
        val html = view(viewModel)(request, messages(application), config)
        val document = doc(html)

        "that include the amount owed message not including interest" in {
          val balanceMessage1 = document.getElementById("balanceNeedToPayNoInterest")
          balanceMessage1.text() mustBe getExpectedBalanceMessage(document, balance, interest)
          balanceMessage1.className() mustBe "govuk-body govuk-!-font-weight-bold"
        }

        "that includes a pay by oldest return deadline message" in {
          document.getElementById("payBy").text() mustBe s"You need to pay by ${pendingReturn1.deadline.format(dateFormatter)}."
        }

        "that includes how to manage direct debit" in {
          document.getElementById("manageExistingDDSubHeader").text() mustBe "How you pay the levy"
          document.getElementById("manageExistingDD").text() mustBe "You have set up a Direct Debit to pay the levy. It will not collect any interest and penalties you owe."
          val manageExistingLink = document.getElementById("manageExistingDDLink")
          manageExistingLink.text() mustBe "Change or cancel your Direct Debit and view your next payment collection date"
          manageExistingLink.getElementsByTag("a").attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/start-direct-debit-journey"
        }

        "that includes details on other ways to pay" in {
          val otherwaystoPay = document.getElementById("otherPaymentOptions")
          otherwaystoPay.text() mustBe "If you choose to make payments outside of this online account, you will need to take note of your Soft Drinks Levy reference XKSDIL000000022. Find out other ways to pay the levy (opens in a new tab)."
          otherwaystoPay.className() mustBe "govuk-inset-text"
          otherwaystoPay.getElementsByTag("a").attr("href") mustBe "https://www.gov.uk/guidance/pay-the-soft-drinks-industry-levy-notice-5"
        }

        "that includes details of delay in payments being displayed on account" in {
          document.getElementById("delayInAccountUpdate").text() mustBe "Your account balance may not reflect payments made in the last 3 days."
        }

        "that includes a secondary button that links to payments" in {
          val button = document.getElementsByClass("govuk-button govuk-button--secondary").get(0)
          button.text() mustBe "Pay now"
          button.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/pay-now"
        }

        "that includes a link to transaction history" in {
          val transHistoryLink = document.getElementById("viewTransactionHistory")
          transHistoryLink.text() mustBe "View your transaction history"
          transHistoryLink.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/transaction-history"
        }
      }

      "when the users balance is negative, has no interest, has no DD setup and has no returns pending" - {
        val balance = BigDecimal(-123.45)
        val interest = BigDecimal(0)
        val viewModel = registeredUserServicePageViewModel(List.empty, None, balance, interest, Some(false))
        val html = view(viewModel)(request, messages(application), config)
        val document = doc(html)
        "that include the amount owed message not including interest" in {
          val balanceMessage1 = document.getElementById("balanceNeedToPayNoInterest")
          balanceMessage1.text() mustBe getExpectedBalanceMessage(document, balance, interest)
          balanceMessage1.className() mustBe "govuk-body govuk-!-font-weight-bold"
        }

        "that does not include a pay by message if no pending returns" in {
          document.getElementById("payBy") mustBe null
        }

        "that includes how to setup direct debit" in {
          document.getElementById("manageSetupDDSubHeader").text() mustBe "Pay by Direct Debit"
          val manageSetupDDContent = document.getElementById("manageSetupDD")
          manageSetupDDContent.text() mustBe "You can set up a Direct Debit to pay the Soft Drinks Industry Levy. You need to do this at least 3 working days before your return and payment due date."
          manageSetupDDContent.getElementsByTag("a").attr("href") mustBe "https://www.gov.uk/guidance/pay-the-soft-drinks-industry-levy-notice-5"
        }

        "that includes details on other ways to pay" in {
          val otherwaystoPay = document.getElementById("otherPaymentOptions")
          otherwaystoPay.text() mustBe "If you choose to make payments outside of this online account, you will need to take note of your Soft Drinks Levy reference XKSDIL000000022. Find out other ways to pay the levy (opens in a new tab)."
          otherwaystoPay.className() mustBe "govuk-inset-text"
          otherwaystoPay.getElementsByTag("a").attr("href") mustBe "https://www.gov.uk/guidance/pay-the-soft-drinks-industry-levy-notice-5"
        }

        "that includes details of delay in payments being displayed on account" in {
          document.getElementById("delayInAccountUpdate").text() mustBe "Your account balance may not reflect payments made in the last 3 days."
        }

        "that includes a secondary button that links to payments" in {
          val button = document.getElementsByClass("govuk-button govuk-button--secondary").get(0)
          button.text() mustBe "Pay now"
          button.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/pay-now"
        }

        "that includes a link to transaction history" in {
          val transHistoryLink = document.getElementById("viewTransactionHistory")
          transHistoryLink.text() mustBe "View your transaction history"
          transHistoryLink.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/transaction-history"
        }
      }

      "when the users balance and interest is negative, direct debit is disabled and has multiple returns pending" - {
        val balance = BigDecimal(-123.45)
        val interest = BigDecimal(-10)
        val viewModel = registeredUserServicePageViewModel(pendingReturns3, None, balance, interest, None)
        val html = view(viewModel)(request, messages(application), config)
        val document = doc(html)
        "that include the amount owed message in bold not including interest" in {
          val balanceMessage1 = document.getElementById("balanceNeedToPayWithInterest")
          balanceMessage1.text() mustBe getExpectedBalanceMessage(document, balance, interest)
          balanceMessage1.className() mustBe "govuk-body govuk-!-font-weight-bold"
        }

        "that includes a pay by oldest return deadline message" in {
          document.getElementById("payBy").text() mustBe s"You need to pay by ${pendingReturn3.deadline.format(dateFormatter)}."
        }

        "that does not include a direct debit message" in {
          document.getElementById("manageSetupDDSubHeader") mustBe null
          document.getElementById("manageExistingDDSubHeader") mustBe null
        }

        "that includes details on other ways to pay" in {
          val otherwaystoPay = document.getElementById("otherPaymentOptions")
          otherwaystoPay.text() mustBe "If you choose to make payments outside of this online account, you will need to take note of your Soft Drinks Levy reference XKSDIL000000022. Find out other ways to pay the levy (opens in a new tab)."
          otherwaystoPay.className() mustBe "govuk-inset-text"
          otherwaystoPay.getElementsByTag("a").attr("href") mustBe "https://www.gov.uk/guidance/pay-the-soft-drinks-industry-levy-notice-5"
        }

        "that includes details of delay in payments being displayed on account" in {
          document.getElementById("delayInAccountUpdate").text() mustBe "Your account balance may not reflect payments made in the last 3 days."
        }

        "that includes a secondary button that links to payments" in {
          val button = document.getElementsByClass("govuk-button govuk-button--secondary").get(0)
          button.text() mustBe "Pay now"
          button.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/pay-now"
        }

        "that includes a link to transaction history" in {
          val transHistoryLink = document.getElementById("viewTransactionHistory")
          transHistoryLink.text() mustBe "View your transaction history"
          transHistoryLink.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/transaction-history"
        }
      }

      "when the users balance is positive, has no interest, has a DD setup and has returns pending" - {
        val balance = BigDecimal(123.45)
        val interest = BigDecimal(0)
        val viewModel = registeredUserServicePageViewModel(pendingReturns1, None, balance, interest, Some(true))
        val html = view(viewModel)(request, messages(application), config)
        val document = doc(html)
        "that include the amount in credit in normal text" in {
          val balanceMessage1 = document.getElementById("balanceCredit")
          balanceMessage1.text() mustBe getExpectedBalanceMessage(document, balance, interest)
          balanceMessage1.className() mustBe Selectors.body
        }

        "that does not include a pay by message" in {
          document.getElementById("payBy") mustBe null
        }

        "that includes how to manage direct debit" in {
          document.getElementById("manageExistingDDSubHeader").text() mustBe "How you pay the levy"
          document.getElementById("manageExistingDD").text() mustBe "You have set up a Direct Debit to pay the levy. It will not collect any interest and penalties you owe."
          val manageExistingLink = document.getElementById("manageExistingDDLink")
          manageExistingLink.text() mustBe "Change or cancel your Direct Debit and view your next payment collection date"
          manageExistingLink.getElementsByTag("a").attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/start-direct-debit-journey"
        }

        "that does not include details on other ways to pay" in {
          document.getElementById("otherPaymentOptions") mustBe null
        }

        "that does not include details of delay in payments being displayed on account" in {
          document.getElementById("delayInAccountUpdate") mustBe null
        }

        "that does not include a secondary button that links to payments" in {
          document.getElementsByClass("govuk-button govuk-button--secondary").eachText() mustNot contain("Pay now")
        }

        "that includes a link to transaction history" in {
          val transHistoryLink = document.getElementById("viewTransactionHistory")
          transHistoryLink.text() mustBe "View your transaction history"
          transHistoryLink.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/transaction-history"
        }
      }

      "when the users balance is zero, has no interest, has a DD setup and has no returns pending" - {
        val balance = BigDecimal(0)
        val interest = BigDecimal(0)
        val viewModel = registeredUserServicePageViewModel(List.empty, None, balance, interest, Some(true))
        val html = view(viewModel)(request, messages(application), config)
        val document = doc(html)
        "that include nothing owed message in normal text" in {
          val balanceMessage1 = document.getElementById("balanceZero")
          balanceMessage1.text() mustBe getExpectedBalanceMessage(document, balance, interest)
          balanceMessage1.className() mustBe Selectors.body
        }

        "that does not include a pay by message" in {
          document.getElementById("payBy") mustBe null
        }

        "that includes how to manage direct debit" in {
          document.getElementById("manageExistingDDSubHeader").text() mustBe "How you pay the levy"
          document.getElementById("manageExistingDD").text() mustBe "You have set up a Direct Debit to pay the levy. It will not collect any interest and penalties you owe."
          val manageExistingLink = document.getElementById("manageExistingDDLink")
          manageExistingLink.text() mustBe "Change or cancel your Direct Debit and view your next payment collection date"
          manageExistingLink.getElementsByTag("a").attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/start-direct-debit-journey"
        }

        "that does not include details on other ways to pay" in {
          document.getElementById("otherPaymentOptions") mustBe null
        }

        "that does not include details of delay in payments being displayed on account" in {
          document.getElementById("delayInAccountUpdate") mustBe null
        }

        "that does not include a secondary button that links to payments" in {
          document.getElementsByClass("govuk-button govuk-button--secondary").eachText() mustNot contain("Pay now")
        }

        "that includes a link to transaction history" in {
          val transHistoryLink = document.getElementById("viewTransactionHistory")
          transHistoryLink.text() mustBe "View your transaction history"
          transHistoryLink.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/transaction-history"
        }
      }
    }

    "include a manage your account section" - {
      "that has the expected subheader" in {
        val subHeader = document1Return.getElementById("manageYourAccount")
        subHeader.text() mustBe "Manage your account"
        subHeader.className() mustBe "govuk-heading-m"
      }

      "that has the expected body including list items" in {
        document1Return.getElementById("manageYourAccountChangesTellWhen").text() mustBe "You should tell HMRC when you:"
        val listItems = document1Return.getElementById("manageYourAccountChanges").getElementsByTag("li")
        listItems.size() mustBe 4
        listItems.eachText().get(0) mustBe "updated contact, address, packaging site or warehouse details"
        listItems.eachText().get(1) mustBe "change the amount of liable drinks produced"
        listItems.eachText().get(2) mustBe "cancel Soft Drinks Industry Levy registration"
        listItems.eachText().get(3) mustBe "correct an error in a previous return"
      }

      "that includes a secondary button that links to makeAChange" in {
        val button = document1Return.getElementsByClass("govuk-button govuk-button--secondary")
        button.eachText() must contain("Tell HMRC about a change")
        button.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/make-a-change"
      }
    }

    "includes the business details section" - {
      "that has the expected subheader" in {
        val subHeader = document1Return.getElementById("businessDetails")
        subHeader.text() mustBe "Business details"
        subHeader.className() mustBe "govuk-heading-m"
      }

      "that has the expected body including business address" in {
       val businessDetailsForUTR = document1Return.getElementById("businessDetailsForUTR")
         businessDetailsForUTR.text() mustBe s"These are the details we hold for Unique Taxpayer Reference (UTR) ${aSubscription.utr}:"
        val address = document1Return.getElementById("businessAddress")
        address.className mustBe "govuk-inset-text"
        address.text() mustBe "Super Lemonade Plc 63 Clifton Roundabout Worcester WR53 7CX"
      }
    }

    "includes a need help section" - {
      "that has the expected subheader" in {
        val subHeader = document1Return.getElementById("needHelp")
        subHeader.text() mustBe "Need help"
        subHeader.className() mustBe "govuk-heading-m"
      }

      "that has a link to sdil guidance" in {
        val link = document1Return.getElementById("sdilGuidance")
        link.text() mustBe "Soft Drinks Industry Levy guidance (opens in a new tab)"
        link.getElementsByTag("a").attr("href") mustBe "https://www.gov.uk/topic/business-tax/soft-drinks-industry-levy"
      }

      "that has a link to sdil regulations" in {
        val link = document1Return.getElementById("sdilRegulations")
        link.text() mustBe "The Soft Drinks Industry Levy Regulations 2018 (opens in a new tab)"
        link.getElementsByTag("a").attr("href") mustBe "https://www.legislation.gov.uk/uksi/2018/41/made"
      }

      "that has a link to sdil contact" in {
        val link = document1Return.getElementById("sdilContact")
        link.text() mustBe "Contact HMRC about your cccc (opens in a new tab)"
        link.getElementsByTag("a").attr("href") mustBe "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/soft-drinks-industry-levy"
      }
    }

    testNoBackLink(document1Return)
    validateTimeoutDialog(document1Return)
    validateAccessibilityStatementLinkPresent(document1Return)
  }

}
