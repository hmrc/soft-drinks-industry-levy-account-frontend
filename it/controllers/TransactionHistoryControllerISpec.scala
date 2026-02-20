/*
 * Copyright 2026 HM Revenue & Customs
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

import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.test.WsTestClient
import testSupport.ITCoreTestData.*
import testSupport.TransactionHistoryITHelper
import testSupport.preConditions.PreconditionBuilder
import testSupport.Specifications

class TransactionHistoryControllerISpec extends TransactionHistoryITHelper with Specifications {

  implicit val builder: PreconditionBuilder = new PreconditionBuilder()
  val transactionHistoryPath = "/transaction-history"

  s"GET $transactionHistoryPath" - {
    "when the user is authenticated and has a subscription" - {
      "should render the transaction history page" - {
        "with one tab containing the expected item" - {
          "when the user has 1 fininical item in balance history" in {
            build
              .commonPrecondition
              .sdilBackend.balanceHistory(SDIL_REF, true, balanceHistory1Item)

            WsTestClient.withClient { client =>
              val result1 = createClientRequestGet(client, baseUrl + transactionHistoryPath)

              whenReady(result1) { res =>
                res.status mustBe 200
                validatePage(res.body, transitionHistoryItems1Item)
              }
            }
          }

          "when the user has several fininical item in balance history that are all the same" in {
            build
              .commonPrecondition
              .sdilBackend.balanceHistory(SDIL_REF, true, balanceHistoryDuplicates)

            WsTestClient.withClient { client =>
              val result1 = createClientRequestGet(client, baseUrl + transactionHistoryPath)

              whenReady(result1) { res =>
                res.status mustBe 200
                validatePage(res.body, transitionHistoryItems1Item)
              }
            }
          }
        }

        "with one tab containing the expected items" - {
          "when the user has multiple fininical item in balance history for the same year" in {
            build
              .commonPrecondition
              .sdilBackend.balanceHistory(SDIL_REF, true, balanceHistoryMultiItemsSameYear)

            WsTestClient.withClient { client =>
              val result1 = createClientRequestGet(client, baseUrl + transactionHistoryPath)

              whenReady(result1) { res =>
                res.status mustBe 200
                validatePage(res.body, transitionHistoryItemsSameYear)
              }
            }
          }
        }

        "with multiple tabs containing the expected items" - {
          "when the user has multiple fininical item in balance history for different years" in {
            build
              .commonPrecondition
              .sdilBackend.balanceHistory(SDIL_REF, true, balanceHistoryMultiItemsDiffYears)

            WsTestClient.withClient { client =>
              val result1 = createClientRequestGet(client, baseUrl + transactionHistoryPath)

              whenReady(result1) { res =>
                res.status mustBe 200
                validatePage(res.body, transitionHistoryItemsDiffYears)
              }
            }
          }

          "when the user has multiple fininical item in balance history for different years that are not ordered" in {
            build
              .commonPrecondition
              .sdilBackend.balanceHistory(SDIL_REF, true, balanceHistoryMultiItemsDiffYearsNotOrder)

            WsTestClient.withClient { client =>
              val result1 = createClientRequestGet(client, baseUrl + transactionHistoryPath)

              whenReady(result1) { res =>
                res.status mustBe 200
                validatePage(res.body, transitionHistoryItemsDiffYears)
              }
            }
          }
        }
      }
    }
    testUnauthorisedUser(baseUrl + transactionHistoryPath)

    "render the error page" - {

      "when the backend call to get sdilSubscription fails with UTR" in {
        build
          .user.isAuthorisedAndEnrolled
          .sdilBackend.retrieveSubscriptionError("utr", UTR)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + transactionHistoryPath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, there is a problem with the service - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }

      "when the backend call to get sdilSubscription fails with SDIL_REF" in {
        build
          .user.isAuthorisedAndEnrolledSDILRef
          .sdilBackend.retrieveSubscriptionError("sdil", SDIL_REF)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + transactionHistoryPath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, there is a problem with the service - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }
    }
  }
}
