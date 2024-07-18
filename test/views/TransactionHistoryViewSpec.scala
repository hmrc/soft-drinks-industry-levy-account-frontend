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

import config.FrontendAppConfig
import models.TransactionHistoryItem
import play.api.mvc.Request
import play.api.test.FakeRequest
import views.html.TransactionHistoryView

class TransactionHistoryViewSpec extends TransactionHistoryViewHelper {

  val application = applicationBuilder().build()
  val view = application.injector.instanceOf[TransactionHistoryView]
  implicit val request: Request[_] = FakeRequest()
  implicit val config: FrontendAppConfig = application
    .injector.instanceOf[FrontendAppConfig]

  object Selectors {
    val heading = "govuk-heading-m"
    val caption = "govuk-caption-m"
    val body = "govuk-body"
    val li = "li"
  }

  val htmlNoTransactions = view("Super Lemonade Plc", Map.empty[Int, List[TransactionHistoryItem]])(request, messages(application), config)
  val documentNoTransactions = doc(htmlNoTransactions)

  val htmlTransitionHistory1Item = view("Super Lemonade Plc", transitionHistoryItems1Item)(request, messages(application), config)
  val documentTransitionHistory1Item = doc(htmlTransitionHistory1Item)

  val htmlTransitionHistoryMultiItemsSameYear = view("Super Lemonade Plc", transitionHistoryItemsSameYear)(request, messages(application), config)
  val documentTransitionHistoryMultiItemsSameYear = doc(htmlTransitionHistoryMultiItemsSameYear)

  val htmlTransitionHistoryMultiItemsDiffYear = view("Super Lemonade Plc", transitionHistoryItemsDiffYears)(request, messages(application), config)
  val documentTransitionHistoryMultiItemsDiffYear = doc(htmlTransitionHistoryMultiItemsDiffYear)

  val testCases = List(
    ("no finincial items", documentNoTransactions, Map.empty),
    ("1 finincial item", documentTransitionHistory1Item, transitionHistoryItems1Item),
    ("multiple finincial items for same year", documentTransitionHistoryMultiItemsSameYear, transitionHistoryItemsSameYear),
    ("multiple fininical items for different years", documentTransitionHistoryMultiItemsDiffYear, transitionHistoryItemsDiffYears)
  )


  "View" - {
    testCases.foreach { case (description, document, transactionHistoryItemsForYears) =>
      s"when there is $description" - {
        "should contain the expected title" in {
          document.title() must include("Transaction history")
        }

        "should include the expected h1 heading" in {
          document.getElementsByTag("h1").text() mustBe "Transaction history"
        }

        "should include a body containing the orgName" in {
          document.getElementsByClass(Selectors.body).first().text() mustBe "Super Lemonade Plc"
        }

        if (transactionHistoryItemsForYears.isEmpty) {
          "should not contain any tabs" in {
            document.getElementsByClass("govuk-tabs").size() mustBe 0
          }
        } else {
          val tabs = document.getElementsByClass("govuk-tabs").first()
          s"should contain a govuk tab for year" - {

            "with the expected title" in {
              document.getElementsByClass("govuk-tabs__title").first().text() mustBe "Contents"
            }

            val panels = document.getElementsByClass("govuk-tabs__list").first().getElementsByTag("li")

            s"that contains ${transactionHistoryItemsForYears.size} panels" in {
              panels.size() mustBe transactionHistoryItemsForYears.size
            }

            transactionHistoryItemsForYears.zipWithIndex.foreach { case ((year, transactionHistoryItems), index) =>
              s"which has the tab link for the year $year" in {
                val tabLink = panels.get(index).getElementsByTag("a").first()
                tabLink.text() mustBe year.toString
                tabLink.attr("href") mustBe s"#year-$year"
              }

              s"that has a panel for $year" - {
                val panel = tabs.getElementById(s"year-$year")
                "with the expected class" in {
                  val expectedPanelClassName = if (index == 0) {
                    "govuk-tabs__panel"
                  } else {
                    "govuk-tabs__panel govuk-tabs__panel--hidden"
                  }
                  panel.className() mustBe expectedPanelClassName
                }

                "which includes a heading with the year " in {
                  panel.getElementsByClass("govuk-heading-m").text() mustBe year.toString
                }

                "which includes a table" - {
                  val table = panel.getElementsByClass("govuk-table").first()
                  "that has the expected table headers" in {
                    val tableHeaders = table.getElementsByClass("govuk-table__header")
                    tableHeaders.size() mustBe 5
                    tableHeaders.get(0).text() mustBe "Date"
                    tableHeaders.get(1).text() mustBe "Transaction"
                    tableHeaders.get(2).text() mustBe "Credits"
                    tableHeaders.get(3).text() mustBe "Debits"
                    tableHeaders.get(4).text() mustBe "Balance"
                  }

                  "that has the expected table rows" in {
                    val tableRows = table.getElementsByClass("govuk-table__body").first().getElementsByTag("tr")
                    tableRows.size() mustBe transactionHistoryItems.size
                    transactionHistoryItems.zipWithIndex.foreach {
                      case (transactionHistoryItem, index1) =>
                        val tableRow = tableRows.get(index1)
                        val rowValues = tableRow.getElementsByTag("td")
                        rowValues.get(0).text() mustBe expectedDateField(transactionHistoryItem)
                        rowValues.get(1).text() mustBe expectedTransactionField(transactionHistoryItem)
                        rowValues.get(2).text() mustBe expectedCredit(transactionHistoryItem)
                        rowValues.get(3).text() mustBe expectedDebit(transactionHistoryItem)
                        rowValues.get(4).text() mustBe formatPounds(transactionHistoryItem.balance)
                    }
                  }
                }
              }
            }
          }
        }

        "that includes a link to sdil home" in {
          val homeLink = document.getElementById("returnToServicePage")
          homeLink.text() mustBe "You can go to your Soft Drinks Industry Levy account"
          homeLink.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/home"
        }

        testBackLink(document)
        validateTimeoutDialog(document)
        validateAccessibilityStatementLinkPresent(document)
      }
    }
  }

}
