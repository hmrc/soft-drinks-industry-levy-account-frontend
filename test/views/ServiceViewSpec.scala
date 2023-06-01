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

class ServiceViewSpec extends ReturnsViewHelper {

  val application = registeredApplicationBuilder().build()
  val view = application.injector.instanceOf[ServiceView]
  implicit val request: Request[_] = FakeRequest()
  implicit val config: FrontendAppConfig = application
    .injector.instanceOf[FrontendAppConfig]

  object Selectors {
    val heading = "govuk-heading-m"
    val caption = "govuk-caption-m"
    val body = "govuk-body"
    val warning = "govuk-warning-text"
    val li = "li"
    val button = "govuk-button"
  }

  val htmlNoReturn = view(servicePageViewModelWithNoReturnInfo)(request, messages(application), config)
  val documentNoReturn = doc(htmlNoReturn)

  val html1Return = view(servicePageViewModel1PendingReturns)(request, messages(application), config)
  val document1Return = doc(html1Return)

  val html3Return = view(servicePageViewModel3PendingReturns)(request, messages(application), config)
  val document3Return = doc(html3Return)

  val htmlWithLastReturn = view(servicePageViewModelWithLastReturn)(request, messages(application), config)
  val documentLastReturn = doc(htmlWithLastReturn)

  "View" - {
    "should contain the expected title" in {
      documentNoReturn.title() must include("Your Soft Drinks Industry Levy account")
    }

    "should include a caption containing the orgName" in {
      documentNoReturn.getElementsByClass(Selectors.caption).text() mustBe "Super Lemonade Plc"
    }

    "should include the expected h1 heading" in {
      documentNoReturn.getElementsByTag("h1").text() mustBe "Your Soft Drinks Industry Levy account"
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
      "when there is no pending returns or return submitted for previous period" in {
        documentNoReturn.getElementsByClass(Selectors.heading).eachText() mustNot contain("Returns")
      }
    }
  }

}
