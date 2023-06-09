package controllers

import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers.{convertToAnyMustWrapper, include}
import play.api.http.HeaderNames
import play.api.test.WsTestClient
import testSupport.ITCoreTestData._
import testSupport.ServicePageITHelper

class ServicePageControllerISpec extends ServicePageITHelper {

  val servicePagePath = "/home"
  val startAReturnPath = "/start-a-return/nilReturn/false"
  val startANilReturnPath = "/start-a-return/nilReturn/true"
  val makeAChangePath = "/make-a-change"

  s"GET $servicePagePath" - {
    "when the user is authenticated and has a subscription" - {
      "should render the service page" - {
        "that includes a returns section and account balance section" - {
          "with a list of pending returns, and an amount to pay message" - {
            "when the user has 1 return pending, currently owes money with no interest and has a direct debit setup" in {
              given
                .commonPrecondition
                .sdilBackend.retrievePendingReturns(UTR, pendingReturns1)
                .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)
                .sdilBackend.balance(SDIL_REF, true, -1000)
                .sdilBackend.balanceHistory(SDIL_REF, true, List(finincialItemReturnCharge))
                .sdilBackend.checkDirectDebitStatus(SDIL_REF, true)

              WsTestClient.withClient { client =>
                val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

                whenReady(result1) { res =>
                  res.status mustBe 200
                  validatePage(res.body, pendingReturns1, None, -1000, 0, true)
                }
              }
            }

            "when the user has more than 1 return pending, currently owes money with interest and has no direct debit setup" in {
              given
                .commonPrecondition
                .sdilBackend.retrievePendingReturns(UTR, pendingReturns3)
                .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)
                .sdilBackend.balance(SDIL_REF, true, -1000)
                .sdilBackend.balanceHistory(SDIL_REF, true, allFinicialItems)
                .sdilBackend.checkDirectDebitStatus(SDIL_REF, false)

              WsTestClient.withClient { client =>
                val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

                whenReady(result1) { res =>
                  res.status mustBe 200
                  validatePage(res.body, pendingReturns3, None, -1000, -20.45, false)
                }
              }
            }

            "when the user has 1 return pending, a lastReturn, a balance of zero and no ddSetup" in {
              given
                .commonPreconditionSdilRef
                .sdilBackend.retrievePendingReturns(UTR, pendingReturns1)
                .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, Some(emptyReturn))
                .sdilBackend.balance(SDIL_REF, true, 0)
                .sdilBackend.balanceHistory(SDIL_REF, true, List.empty)
                .sdilBackend.checkDirectDebitStatus(SDIL_REF, false)

              WsTestClient.withClient { client =>
                val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

                whenReady(result1) { res =>
                  res.status mustBe 200
                  validatePage(res.body, pendingReturns1, Some(emptyReturn), 0, 0, false)
                }
              }
            }
          }

          "with no warning message but inset text containing details of last sent return" - {

            "when there is no returns pending, a return for the previous period submitted, and an balance in credit" in {
              given
                .commonPrecondition
                .sdilBackend.retrievePendingReturns(UTR, List.empty)
                .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, Some(emptyReturn))
                .sdilBackend.balance(SDIL_REF, true)
                .sdilBackend.balanceHistory(SDIL_REF, true, List.empty)
                .sdilBackend.checkDirectDebitStatus(SDIL_REF, false)

              WsTestClient.withClient { client =>
                val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

                whenReady(result1) { res =>
                  res.status mustBe 200
                  validatePage(res.body, List.empty, Some(emptyReturn), 1000, 0, false)
                }
              }
            }
          }
        }

        "that has 5 sections which includes a message about small producers not submitting returns instead of a returns, account with correct balance and interest, manage your account, business details and need help" - {
          "when there are no pending returns or lastReturn and balance history returns duplicate items" in {
            given
              .authorisedSmallProducer
              .sdilBackend.retrievePendingReturns(UTR, pendingReturns1)
              .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, Some(emptyReturn))
              .sdilBackend.balance(SDIL_REF, true, -1000)
              .sdilBackend.balanceHistory(SDIL_REF, true, allFinicialItems ++ allFinicialItems)
              .sdilBackend.checkDirectDebitStatus(SDIL_REF, true)

            WsTestClient.withClient { client =>
              val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

              whenReady(result1) { res =>
                res.status mustBe 200
                validatePage(res.body, List.empty, None, -1000, -20.45, true, true)
              }
            }
          }
        }

        "that does not include a returns section but has an account section with correct balance and interest" - {
          "when there are no pending returns or lastReturn and balance history returns duplicate items" in {
            given
              .commonPreconditionBoth
              .sdilBackend.retrievePendingReturns(UTR, List.empty)
              .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)
              .sdilBackend.balance(SDIL_REF, true, -1000)
              .sdilBackend.balanceHistory(SDIL_REF, true, allFinicialItems ++ allFinicialItems)
              .sdilBackend.checkDirectDebitStatus(SDIL_REF, true)

            WsTestClient.withClient { client =>
              val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

              whenReady(result1) { res =>
                res.status mustBe 200
                validatePage(res.body, List.empty, None, -1000, -20.45, true)
              }
            }
          }
        }
      }
    }
    testUnauthorisedUser(baseUrl + servicePagePath)

    "render the error page" - {
      "when the backend call to get pending enrolments fails" in {
        given
          .commonPrecondition
          .sdilBackend.retrievePendingReturnsError(UTR)
          .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, we are experiencing technical difficulties - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }

      "when the backend call to get sdilSubscription fails with UTR" in {
        given
          .user.isAuthorisedAndEnrolled
          .sdilBackend.retrieveSubscriptionError("utr", UTR)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, we are experiencing technical difficulties - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }

      "when the backend call to get sdilSubscription fails with SDIL_REF" in {
        given
          .user.isAuthorisedAndEnrolledSDILRef
          .sdilBackend.retrieveSubscriptionError("sdil", SDIL_REF)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, we are experiencing technical difficulties - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }
    }
  }

  s"GET $startAReturnPath" - {
    "when the user is authenticated and has a subscription" - {
      "should redirect to sdilReturns" - {
        "with the year and quarter from the earliest pending return" - {
          "when there is 1 pending return" in {
            given
              .commonPrecondition
              .sdilBackend.retrievePendingReturns(UTR, pendingReturns1)

            WsTestClient.withClient { client =>
              val result1 = createClientRequestGet(client, baseUrl + startAReturnPath)

              whenReady(result1) { res =>
                res.status mustBe 303
                res.header(HeaderNames.LOCATION).get must include(
                  s"/soft-drinks-industry-levy-returns-frontend" +
                    s"/submit-return/year/${pendingReturn3.year}" +
                    s"/quarter/${pendingReturn3.quarter}/nil-return/false")
              }
            }
          }

          "when there is more than 1 pending return" in {
            given
              .commonPrecondition
              .sdilBackend.retrievePendingReturns(UTR, pendingReturns3)

            WsTestClient.withClient { client =>
              val result1 = createClientRequestGet(client, baseUrl + startAReturnPath)

              whenReady(result1) { res =>
                res.status mustBe 303
                res.header(HeaderNames.LOCATION).get must include(
                  s"/soft-drinks-industry-levy-returns-frontend" +
                    s"/submit-return/year/${pendingReturn3.year}" +
                    s"/quarter/${pendingReturn3.quarter}/nil-return/false")
              }
            }
          }
        }
      }
    }
    testUnauthorisedUser(baseUrl + startAReturnPath)

    "render the error page" - {
      "when the backend call to get pending enrolments fails" in {
        given
          .commonPrecondition
          .sdilBackend.retrievePendingReturnsError(UTR)
          .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + startAReturnPath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, we are experiencing technical difficulties - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }

      "when the backend call to get sdilSubscription fails with UTR" in {
        given
          .user.isAuthorisedAndEnrolled
          .sdilBackend.retrieveSubscriptionError("utr", UTR)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + startAReturnPath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, we are experiencing technical difficulties - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }

      "when the backend call to get sdilSubscription fails with SDIL_REF" in {
        given
          .user.isAuthorisedAndEnrolledSDILRef
          .sdilBackend.retrieveSubscriptionError("sdil", SDIL_REF)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + startAReturnPath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, we are experiencing technical difficulties - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }
    }
  }

  s"GET $startANilReturnPath" - {
    "when the user is authenticated and has a subscription" - {
      "should redirect to sdilReturns" - {
        "with the year and quarter from the earliest pending return" - {
          "when there is 1 pending return" in {
            given
              .commonPrecondition
              .sdilBackend.retrievePendingReturns(UTR, pendingReturns1)

            WsTestClient.withClient { client =>
              val result1 = createClientRequestGet(client, baseUrl + startANilReturnPath)

              whenReady(result1) { res =>
                res.status mustBe 303
                res.header(HeaderNames.LOCATION).get must include(
                  s"/soft-drinks-industry-levy-returns-frontend" +
                    s"/submit-return/year/${pendingReturn3.year}" +
                    s"/quarter/${pendingReturn3.quarter}/nil-return/true")
              }
            }
          }

          "when there is more than 1 pending return" in {
            given
              .commonPrecondition
              .sdilBackend.retrievePendingReturns(UTR, pendingReturns3)

            WsTestClient.withClient { client =>
              val result1 = createClientRequestGet(client, baseUrl + startANilReturnPath)

              whenReady(result1) { res =>
                res.status mustBe 303
                res.header(HeaderNames.LOCATION).get must include(
                  s"/soft-drinks-industry-levy-returns-frontend" +
                    s"/submit-return/year/${pendingReturn3.year}" +
                    s"/quarter/${pendingReturn3.quarter}/nil-return/true")
              }
            }
          }
        }
      }
    }
    testUnauthorisedUser(baseUrl + startANilReturnPath)

    "render the error page" - {
      "when the backend call to get pending enrolments fails" in {
        given
          .commonPrecondition
          .sdilBackend.retrievePendingReturnsError(UTR)
          .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + startANilReturnPath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, we are experiencing technical difficulties - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }

      "when the backend call to get sdilSubscription fails with UTR" in {
        given
          .user.isAuthorisedAndEnrolled
          .sdilBackend.retrieveSubscriptionError("utr", UTR)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + startANilReturnPath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, we are experiencing technical difficulties - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }

      "when the backend call to get sdilSubscription fails with SDIL_REF" in {
        given
          .user.isAuthorisedAndEnrolledSDILRef
          .sdilBackend.retrieveSubscriptionError("sdil", SDIL_REF)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + startANilReturnPath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, we are experiencing technical difficulties - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }
    }
  }

  s"GET $makeAChangePath" - {
    "when the user is authenticated and has a subscription" - {
      "should redirect to sdilVariations" in {
        given
          .commonPrecondition

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + makeAChangePath)

          whenReady(result1) { res =>
            res.status mustBe 303
            res.header(HeaderNames.LOCATION).get must include(
              s"/soft-drinks-industry-levy-variations-frontend/select-change")
          }
        }
      }
    }
    "render the error page" - {
      "when the backend call to get sdilSubscription fails with UTR" in {
        given
          .user.isAuthorisedAndEnrolled
          .sdilBackend.retrieveSubscriptionError("utr", UTR)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + makeAChangePath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, we are experiencing technical difficulties - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }

      "when the backend call to get sdilSubscription fails with SDIL_REF" in {
        given
          .user.isAuthorisedAndEnrolledSDILRef
          .sdilBackend.retrieveSubscriptionError("sdil", SDIL_REF)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + makeAChangePath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, we are experiencing technical difficulties - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }
    }
  }
}
