package controllers

import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers.{convertToAnyMustWrapper, include}
import play.api.http.HeaderNames
import play.api.test.WsTestClient
import testSupport.ITCoreTestData._
import testSupport.ReturnsITHelper

class ServicePageControllerISpec extends ReturnsITHelper {

  val servicePagePath = "/home"
  val startAReturnPath = "/start-a-return/nilReturn/false"
  val startANilReturnPath = "/start-a-return/nilReturn/true"

  s"GET $servicePagePath" - {
    "when the user is authenticated and has a subscription" - {
      "should render the service page" - {
        "that includes a returns section" - {
          "with a list of pending returns" - {
            "when the user has 1 return pending" in {
              given
                .commonPrecondition
                .sdilBackend.retrievePendingReturns(UTR, pendingReturns1)
                .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)

              WsTestClient.withClient { client =>
                val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

                whenReady(result1) { res =>
                  res.status mustBe 200
                  validatePage(res.body, pendingReturns1, None)
                }
              }
            }

            "when the user has more than 1 return pending" in {
              given
                .commonPrecondition
                .sdilBackend.retrievePendingReturns(UTR, pendingReturns3)
                .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)

              WsTestClient.withClient { client =>
                val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

                whenReady(result1) { res =>
                  res.status mustBe 200
                  validatePage(res.body, pendingReturns3, None)
                }
              }
            }

            "when the user has 1 return pending and also a lastReturn" in {
              given
                .commonPreconditionSdilRef
                .sdilBackend.retrievePendingReturns(UTR, pendingReturns1)
                .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, Some(emptyReturn))

              WsTestClient.withClient { client =>
                val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

                whenReady(result1) { res =>
                  res.status mustBe 200
                  validatePage(res.body, pendingReturns1, Some(emptyReturn))
                }
              }
            }
          }

          "with no warning message but inset text containing details of last sent return" in {
            given
              .commonPrecondition
              .sdilBackend.retrievePendingReturns(UTR, List.empty)
              .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, Some(emptyReturn))

            WsTestClient.withClient { client =>
              val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

              whenReady(result1) { res =>
                res.status mustBe 200
                validatePage(res.body, List.empty, Some(emptyReturn))
              }
            }
          }
        }

        "that does not include a returns section when there are no pending returns or lastReturn" in {
          given
            .commonPrecondition
            .sdilBackend.retrievePendingReturns(UTR, List.empty)
            .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)

          WsTestClient.withClient { client =>
            val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

            whenReady(result1) { res =>
              res.status mustBe 200
              validatePage(res.body, List.empty, None)
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

      "when the backend call to get lastReturn fails" in {
        given
          .commonPrecondition
          .sdilBackend.retrievePendingReturns(UTR, List.empty)
          .sdilBackend.retrieveReturnError(UTR, currentReturnPeriod.previous)

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
                    s"/submit-return/year/${pendingReturn1.year}" +
                    s"/quarter/${pendingReturn1.quarter}/nil-return/false")
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
                    s"/submit-return/year/${pendingReturn1.year}" +
                    s"/quarter/${pendingReturn1.quarter}/nil-return/true")
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
}
