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
import views.html.DeregisteredUserServiceView

class DeregisteredUserServiceViewSpec extends DeregisteredUserServiceViewHelper {

  val application = applicationBuilder().build()
  val view = application.injector.instanceOf[DeregisteredUserServiceView]
  implicit val request: Request[_] = FakeRequest()
  implicit val config: FrontendAppConfig = application
    .injector.instanceOf[FrontendAppConfig]

  object Selectors {
    val heading = "govuk-heading-l"
    val subHeading = "govuk-heading-m"
    val notificationBanner ="govuk-notification-banner"
    val body = "govuk-body"
    val warning = "govuk-warning-text"
    val li = "li"
    val button = "govuk-button"
  }

  val hasVariableReturnsOptions = List(true, false)
  val hasSentLastReturnOptions = List(true, false)
  val sentFinalReturnOptions = List(true, false)
  val balanceOptions = List(100, 0, -100)

  def hasOrHasNot(isTrue: Boolean): String = if(isTrue) {"has"} else {"has not"}

  "View" - {
    hasVariableReturnsOptions.foreach { hasVariableReturns =>
      hasSentLastReturnOptions.foreach { hasSentLastReturn =>
        sentFinalReturnOptions.foreach { sentFinalReturn =>
          balanceOptions.foreach { balance =>
            s"when the user ${hasOrHasNot(hasVariableReturns)} variable return(s)," +
              s" ${hasOrHasNot(hasSentLastReturn)} sent the last return, ${hasOrHasNot(sentFinalReturn)} sent the final return" +
              s" and has a balance of $balance" - {
              val deregUserViewModel = generateDeregUserServicePageModel(hasVariableReturns, !sentFinalReturn, hasSentLastReturn, balance)
              val html = view.apply(deregUserViewModel)
              val document = doc(html)
              "should contain the expected title" in {
                document.title() mustBe "Your Soft Drinks Industry Levy account - Soft Drinks Industry Levy - GOV.UK"
              }
              "should include the expected heading" in {
                document.getElementsByTag("h1").text() mustBe "Super Lemonade Plc Your Soft Drinks Industry Levy account"
              }

              "should include a notification banner" - {
                val notificationBanner = document.getElementsByClass(Selectors.notificationBanner).first()
                testNotificationBanner(notificationBanner, sentFinalReturn)
              }

              if(sentFinalReturn) {
                if(hasSentLastReturn) {
                  testFinalAndLastReturnSentSection(document)
                }
                "should include a section change business activity" - {
                  "that has the expected header" in {
                    document.getElementsByClass(Selectors.subHeading).first().text() mustBe "Changed your business activity?"
                  }

                  "that has the expected content" in {
                    document.getElementById("changeBusinessActivityP1").text() mustBe "If you become liable to register for the Soft Drinks Industry Levy, you must register again."
                  }
                  "that has a button to register again" in {
                    val button = document.getElementsByClass(Selectors.button)
                    button.eachText() must contain("Register again")
                    button.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/register/start"
                  }
                }
              } else {
                if (hasSentLastReturn) {
                  testFinalReturnDueInFutureSection(document)
                } else {
                  testOverdueFinalReturnSection(document)
                }

                "should not include a section change business activity" in {
                  document.getElementsByClass(Selectors.subHeading).eachText() mustNot contain("Changed your business activity?")
                }
              }

              "should include an account balance section" - {
                "that has the expected subheader" in {
                  document.getElementsByClass(Selectors.subHeading).eachText() must contain("Account balance")
                }

                s"that has the expected content when the balance is $balance" in {
                  val expectedContent = if(balance < 0) {
                    "Your balance is £100.00."
                  } else if(balance > 0 && sentFinalReturn) {
                    "Your balance is £100.00 in credit. Call the Soft Drinks Industry Levy helpline to arrange repayment."
                  } else if(balance > 0) {
                    "You are £100.00 in credit. This account is closed."
                  } else {
                    "Your balance is £0. This account is closed."
                  }
                  document.getElementsByClass("govuk-body").text() must include(expectedContent)
                }

                "that includes a link to transaction history" in {
                  val transHistoryLink = document.getElementById("viewTransactionHistory")
                  transHistoryLink.text() mustBe "View your transaction history"
                  transHistoryLink.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/transaction-history"
                }
              }

              if(hasVariableReturns) {

                "should include a manage your account section" - {
                  "that has the expected subheader" in {
                    val subHeader = document.getElementById("manageYourAccount")
                    subHeader.text() mustBe "Manage your account"
                    subHeader.className() mustBe "govuk-heading-m"
                  }

                  "that includes a secondary button that links to correctAReturn" in {
                    val button = document.getElementsByClass("govuk-button govuk-button--secondary")
                    button.eachText() must contain("Correct an error in a previous return")
                    button.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/correct-a-return"
                  }
                }
              } else {
                "should not include a manage account section" in {
                  document.getElementsByClass(Selectors.subHeading).eachText() mustNot contain("Manage your account")
                }
              }

              "includes the business details section" - {
                "that has the expected subheader" in {
                  val subHeader = document.getElementById("businessDetails")
                  subHeader.text() mustBe "Business details"
                  subHeader.className() mustBe "govuk-heading-m"
                }

                "that has the expected body including business address" in {
                  val businessDetailsForUTR = document.getElementById("businessDetailsForUTR")
                  businessDetailsForUTR.text() mustBe s"These are the details we hold for Unique Taxpayer Reference (UTR) ${aSubscription.utr}:"
                  val address = document.getElementById("businessAddress")
                  address.className mustBe "govuk-inset-text"
                  address.text() mustBe "Super Lemonade Plc 63 Clifton Roundabout Worcester WR53 7CX"
                }
              }

              "includes a need help section" - {
                "that has the expected subheader" in {
                  val subHeader = document.getElementById("needHelp")
                  subHeader.text() mustBe "Need help"
                  subHeader.className() mustBe "govuk-heading-m"
                }

                "that has a link to sdil guidance" in {
                  val link = document.getElementById("sdilGuidance")
                  link.text() mustBe "Soft Drinks Industry Levy guidance (opens in a new tab)"
                  link.getElementsByTag("a").attr("href") mustBe "https://www.gov.uk/topic/business-tax/soft-drinks-industry-levy"
                }

                "that has a link to sdil regulations" in {
                  val link = document.getElementById("sdilRegulations")
                  link.text() mustBe "The Soft Drinks Industry Levy Regulations 2018 (opens in a new tab)"
                  link.getElementsByTag("a").attr("href") mustBe "https://www.legislation.gov.uk/uksi/2018/41/made"
                }

                "that has a link to sdil contact" in {
                  val link = document.getElementById("sdilContact")
                  link.text() mustBe "Contact HMRC about your Soft Drinks Industry Levy (opens in a new tab)"
                  link.getElementsByTag("a").attr("href") mustBe "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/soft-drinks-industry-levy"
                }
              }

              testNoBackLink(document)
              validateTimeoutDialog(document)
              validateAccessibilityStatementLinkPresent(document)
            }
          }
        }
      }
    }
  }
}
