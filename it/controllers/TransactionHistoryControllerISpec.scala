package controllers

import org.jsoup.Jsoup
import play.api.test.WsTestClient
import testSupport.ITCoreTestData._
import testSupport.TransactionHistoryITHelper
import org.scalatest.matchers.must.Matchers._


class TransactionHistoryControllerISpec extends TransactionHistoryITHelper {

  val transactionHistoryPath = "/transaction-history"

  s"GET $transactionHistoryPath" - {
    "when the user is authenticated and has a subscription" - {
      "should render the transaction history page" - {
        "with one tab containing the expected item" - {
          "when the user has 1 fininical item in balance history" in {
            given
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
            given
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
            given
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
            given
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
            given
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
        given
          .user.isAuthorisedAndEnrolled
          .sdilBackend.retrieveSubscriptionError("utr", UTR)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + transactionHistoryPath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }

      "when the backend call to get sdilSubscription fails with SDIL_REF" in {
        given
          .user.isAuthorisedAndEnrolledSDILRef
          .sdilBackend.retrieveSubscriptionError("sdil", SDIL_REF)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + transactionHistoryPath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }
    }
  }
}
