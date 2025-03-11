package controllers

import models.ReturnPeriod
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers._
import play.api.http.HeaderNames
import play.api.test.WsTestClient
import testSupport.ITCoreTestData._
import testSupport.ServicePageITHelper
import testSupport.Specifications
import org.scalatest.matchers.must.Matchers.mustBe

class ServicePageControllerISpec extends ServicePageITHelper with Specifications {

  val servicePagePath = "/home"
  val startAReturnPath = "/start-a-return/nilReturn/false"
  val startANilReturnPath = "/start-a-return/nilReturn/true"
  val makeAChangePath = "/make-a-change"
  val correctAReturnPath = "/correct-a-return"

  s"GET $servicePagePath" - {
    "when the user is authenticated, registered and has a subscription" - {
      "should render the service page" - {
        "that includes a returns section and account balance section" - {
          "with a list of pending returns, and an amount to pay message" - {
            "when the user has 1 return pending, currently owes money with no interest and has a direct debit setup" in {
              build
                .commonPrecondition
                .sdilBackend.retrievePendingReturns(UTR, pendingReturns1)
                .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)
                .sdilBackend.balance(SDIL_REF, true, -1000)
                .sdilBackend.balanceHistory(SDIL_REF, true, List(financialItemReturnCharge))
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
              build
                .commonPrecondition
                .sdilBackend.retrievePendingReturns(UTR, pendingReturns3)
                .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)
                .sdilBackend.balance(SDIL_REF, true, -1000)
                .sdilBackend.balanceHistory(SDIL_REF, true, allFinancialItems)
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
              build
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
              build
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
            build
              .authorisedSmallProducer
              .sdilBackend.retrievePendingReturns(UTR, pendingReturns1)
              .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, Some(emptyReturn))
              .sdilBackend.balance(SDIL_REF, true, -1000)
              .sdilBackend.balanceHistory(SDIL_REF, true, allFinancialItems ++ allFinancialItems)
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
            build
              .commonPreconditionBoth
              .sdilBackend.retrievePendingReturns(UTR, List.empty)
              .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)
              .sdilBackend.balance(SDIL_REF, true, -1000)
              .sdilBackend.balanceHistory(SDIL_REF, true, allFinancialItems ++ allFinancialItems)
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

    "when the user is authenticated but deregistered" - {
      "should render the deregistered service page that includes the business details, how to get help" - {
        "a notification banner stating the user is no longer registered," - {
          "no returns section, a register again section," - {
            "a correct error in previous return section, and a manage account section that has nothing owed" - {
              "when the user has submitted their final return, has no last return, has variable returns and a balance of 0" in {
                build
                  .authorisedWithSdilSubscriptionIncDeRegDatePrecondition
                  .sdilBackend.retrieveVariableReturns(UTR, pendingReturns1)
                  .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)
                  .sdilBackend.retrieveReturn(UTR, ReturnPeriod(deregDate), Some(emptyReturn))
                  .sdilBackend.balance(SDIL_REF, true, 0)

                WsTestClient.withClient { client =>
                  val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

                  whenReady(result1) { res =>
                    res.status mustBe 200
                    validatePageDeregistered(res.body, true, true, None, 0)
                  }
                }
              }
            }
            "no correct error in previous return section, and a manage account section that has shows account in credit" - {
              "when the user has submitted their final return, has no last return, has no variable returns and a balance of 100" in {
                build
                  .authorisedWithSdilSubscriptionIncDeRegDatePrecondition
                  .sdilBackend.retrieveVariableReturns(UTR, List())
                  .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)
                  .sdilBackend.retrieveReturn(UTR, ReturnPeriod(deregDate), Some(emptyReturn))
                  .sdilBackend.balance(SDIL_REF, true, 100)

                WsTestClient.withClient { client =>
                  val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

                  whenReady(result1) { res =>
                    res.status mustBe 200
                    validatePageDeregistered(res.body, false, true, None, 100)
                  }
                }
              }
            }
          }

          "has message about the last return sent, a register again section," - {
            "a correct error in previous return section, and a manage account section that states they owe money" - {
              "when the user has submitted their final return, has last return, has variable returns and a balance of -100" in {
                build
                  .authorisedWithSdilSubscriptionIncDeRegDatePrecondition
                  .sdilBackend.retrieveVariableReturns(UTR, pendingReturns1)
                  .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, Some(emptyReturn))
                  .sdilBackend.retrieveReturn(UTR, ReturnPeriod(deregDate), Some(emptyReturn))
                  .sdilBackend.balance(SDIL_REF, true, -100)

                WsTestClient.withClient { client =>
                  val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

                  whenReady(result1) { res =>
                    res.status mustBe 200
                    validatePageDeregistered(res.body, true, true, Some(emptyReturn), -100)
                  }
                }
              }
            }
            "no correct error in previous return section, and a manage account section that has shows account with nothing owed" - {
              "when the user has submitted their final return, has last return, has no variable returns and a balance of 0" in {
                build
                  .authorisedWithSdilSubscriptionIncDeRegDatePrecondition
                  .sdilBackend.retrieveVariableReturns(UTR, List())
                  .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, Some(emptyReturn))
                  .sdilBackend.retrieveReturn(UTR, ReturnPeriod(deregDate), Some(emptyReturn))
                  .sdilBackend.balance(SDIL_REF, true, 0)

                WsTestClient.withClient { client =>
                  val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

                  whenReady(result1) { res =>
                    res.status mustBe 200
                    validatePageDeregistered(res.body, false, true, Some(emptyReturn), 0)
                  }
                }
              }
            }
          }

          "a notification banner stating the final return needs to be sent to cancel" - {
            "has message about the last return sent with the deadline for the final return, a register again section," - {
              "a correct error in previous return section, and a manage account section that has shows account with nothing owed" - {
                "when the user has not submitted their final return, has last return, has variable returns and a balance of 0" in {
                  build
                    .authorisedWithSdilSubscriptionIncDeRegDatePrecondition
                    .sdilBackend.retrieveVariableReturns(UTR, pendingReturns1)
                    .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, Some(emptyReturn))
                    .sdilBackend.retrieveReturn(UTR, ReturnPeriod(deregDate), None)
                    .sdilBackend.balance(SDIL_REF, true, 0)

                  WsTestClient.withClient { client =>
                    val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

                    whenReady(result1) { res =>
                      res.status mustBe 200
                      validatePageDeregistered(res.body, true, false, Some(emptyReturn), 0)
                    }
                  }
                }
              }
              "no correct error in previous return section, and a manage account section that has shows account with nothing owed" - {
                "when the user has not submitted their final return, has last return, has no variable returns and a balance of 0" in {
                  build
                    .authorisedWithSdilSubscriptionIncDeRegDatePrecondition
                    .sdilBackend.retrieveVariableReturns(UTR, List())
                    .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, Some(emptyReturn))
                    .sdilBackend.retrieveReturn(UTR, ReturnPeriod(deregDate), None)
                    .sdilBackend.balance(SDIL_REF, true, 0)

                  WsTestClient.withClient { client =>
                    val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

                    whenReady(result1) { res =>
                      res.status mustBe 200
                      validatePageDeregistered(res.body, false, false, Some(emptyReturn), 0)
                    }
                  }
                }
              }
            }

            "has a section to send final return, no register again section," - {
              "a correct error in previous return section, and a manage account section that has shows account with nothing owed" - {
                "when the user has not submitted their final return, has no last return, has variable returns and a balance of 0" in {
                  build
                    .authorisedWithSdilSubscriptionIncDeRegDatePrecondition
                    .sdilBackend.retrieveVariableReturns(UTR, pendingReturns1)
                    .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)
                    .sdilBackend.retrieveReturn(UTR, ReturnPeriod(deregDate), None)
                    .sdilBackend.balance(SDIL_REF, true, 0)

                  WsTestClient.withClient { client =>
                    val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

                    whenReady(result1) { res =>
                      res.status mustBe 200
                      validatePageDeregistered(res.body, true, false, None, 0)
                    }
                  }
                }
              }
              "no correct error in previous return section, and a manage account section that has shows account in credit" - {
                "when the user has not submitted their final return, has no last return, has no variable returns and a balance of 100" in {
                  build
                    .authorisedWithSdilSubscriptionIncDeRegDatePrecondition
                    .sdilBackend.retrieveVariableReturns(UTR, List())
                    .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)
                    .sdilBackend.retrieveReturn(UTR, ReturnPeriod(deregDate), None)
                    .sdilBackend.balance(SDIL_REF, true, 100)

                  WsTestClient.withClient { client =>
                    val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

                    whenReady(result1) { res =>
                      res.status mustBe 200
                      validatePageDeregistered(res.body, false, false, None, 100)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    testUnauthorisedUser(baseUrl + servicePagePath)

    "render the error page" - {
      "for a deregistered user" - {
        "when the backend call to get variable enrolments fails" in {
          build
            .authorisedWithSdilSubscriptionIncDeRegDatePrecondition
            .sdilBackend.retrieveVariableReturnsError(UTR)
            .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)
            .sdilBackend.retrieveReturn(UTR, ReturnPeriod(deregDate), None)
            .sdilBackend.balance(SDIL_REF, true, 100)

          WsTestClient.withClient { client =>
            val result1 = createClientRequestGet(client, baseUrl + startANilReturnPath)

            whenReady(result1) { res =>
              res.status mustBe 500
              val page = Jsoup.parse(res.body)
              page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
            }
          }
        }

        "when the backend call to get last return fails" in {
          build
            .authorisedWithSdilSubscriptionIncDeRegDatePrecondition
            .sdilBackend.retrieveVariableReturns(UTR, List())
            .sdilBackend.retrieveReturnError(UTR, currentReturnPeriod.previous)
            .sdilBackend.retrieveReturn(UTR, ReturnPeriod(deregDate), None)
            .sdilBackend.balance(SDIL_REF, true, 100)

          WsTestClient.withClient { client =>
            val result1 = createClientRequestGet(client, baseUrl + startANilReturnPath)

            whenReady(result1) { res =>
              res.status mustBe 500
              val page = Jsoup.parse(res.body)
              page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
            }
          }
        }
        "when the backend call to get final return fails" in {
          build
            .authorisedWithSdilSubscriptionIncDeRegDatePrecondition
            .sdilBackend.retrieveVariableReturns(UTR, List())
            .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)
            .sdilBackend.retrieveReturnError(UTR, ReturnPeriod(deregDate))
            .sdilBackend.balance(SDIL_REF, true, 100)

          WsTestClient.withClient { client =>
            val result1 = createClientRequestGet(client, baseUrl + startANilReturnPath)

            whenReady(result1) { res =>
              res.status mustBe 500
              val page = Jsoup.parse(res.body)
              page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
            }
          }
        }

        "when the backend call to get balance fails" in {
          build
            .authorisedWithSdilSubscriptionIncDeRegDatePrecondition
            .sdilBackend.retrieveVariableReturns(UTR, List())
            .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)
            .sdilBackend.retrieveReturn(UTR, ReturnPeriod(deregDate), None)
            .sdilBackend.balancefailure(SDIL_REF, true)

          WsTestClient.withClient { client =>
            val result1 = createClientRequestGet(client, baseUrl + startANilReturnPath)

            whenReady(result1) { res =>
              res.status mustBe 500
              val page = Jsoup.parse(res.body)
              page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
            }
          }
        }
        "when all the backend calls fail" in {
          build
            .authorisedWithSdilSubscriptionIncDeRegDatePrecondition
            .sdilBackend.retrieveVariableReturnsError(UTR)
            .sdilBackend.retrieveReturnError(UTR, currentReturnPeriod.previous)
            .sdilBackend.retrieveReturnError(UTR, ReturnPeriod(deregDate))
            .sdilBackend.balancefailure(SDIL_REF, true)

          WsTestClient.withClient { client =>
            val result1 = createClientRequestGet(client, baseUrl + startANilReturnPath)

            whenReady(result1) { res =>
              res.status mustBe 500
              val page = Jsoup.parse(res.body)
              page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
            }
          }
        }
      }
      "when the backend call to get pending enrolments fails" in {
        build
          .commonPrecondition
          .sdilBackend.retrievePendingReturnsError(UTR)
          .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }

      "when the backend call to get sdilSubscription fails with UTR" in {
        build
          .user.isAuthorisedAndEnrolled
          .sdilBackend.retrieveSubscriptionError("utr", UTR)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }

      "when the backend call to get sdilSubscription fails with SDIL_REF" in {
        build
          .user.isAuthorisedAndEnrolledSDILRef
          .sdilBackend.retrieveSubscriptionError("sdil", SDIL_REF)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + servicePagePath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
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
            build
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
            build
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
        build
          .commonPrecondition
          .sdilBackend.retrievePendingReturnsError(UTR)
          .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + startAReturnPath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }

      "when the backend call to get sdilSubscription fails with UTR" in {
        build
          .user.isAuthorisedAndEnrolled
          .sdilBackend.retrieveSubscriptionError("utr", UTR)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + startAReturnPath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }

      "when the backend call to get sdilSubscription fails with SDIL_REF" in {
        build
          .user.isAuthorisedAndEnrolledSDILRef
          .sdilBackend.retrieveSubscriptionError("sdil", SDIL_REF)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + startAReturnPath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
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
            build
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
            build
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
        build
          .commonPrecondition
          .sdilBackend.retrievePendingReturnsError(UTR)
          .sdilBackend.retrieveReturn(UTR, currentReturnPeriod.previous, None)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + startANilReturnPath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }

      "when the backend call to get sdilSubscription fails with UTR" in {
        build
          .user.isAuthorisedAndEnrolled
          .sdilBackend.retrieveSubscriptionError("utr", UTR)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + startANilReturnPath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }

      "when the backend call to get sdilSubscription fails with SDIL_REF" in {
        build
          .user.isAuthorisedAndEnrolledSDILRef
          .sdilBackend.retrieveSubscriptionError("sdil", SDIL_REF)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + startANilReturnPath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }
    }
  }

  s"GET $makeAChangePath" - {
    "when the user is authenticated and has a subscription" - {
      "should redirect to sdilVariations" in {
        build
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
        build
          .user.isAuthorisedAndEnrolled
          .sdilBackend.retrieveSubscriptionError("utr", UTR)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + makeAChangePath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }

      "when the backend call to get sdilSubscription fails with SDIL_REF" in {
        build
          .user.isAuthorisedAndEnrolledSDILRef
          .sdilBackend.retrieveSubscriptionError("sdil", SDIL_REF)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + makeAChangePath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }
    }
  }

  s"GET $correctAReturnPath" - {
    "when the user is authenticated and has a subscription" - {
      "should redirect to sdilVariations" in {
        build
          .authorisedWithSdilSubscriptionIncDeRegDatePrecondition

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + correctAReturnPath)

          whenReady(result1) { res =>
            res.status mustBe 303
            res.header(HeaderNames.LOCATION).get must include(
              s"/soft-drinks-industry-levy-variations-frontend/correct-return/select")
          }
        }
      }
    }
    "render the error page" - {
      "when the backend call to get sdilSubscription fails with UTR" in {
        build
          .user.isAuthorisedAndEnrolled
          .sdilBackend.retrieveSubscriptionError("utr", UTR)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + correctAReturnPath)

          whenReady(result1) { res =>
            res.status mustBe 500
            val page = Jsoup.parse(res.body)
            page.title() mustBe "Sorry, there is a problem with the service - 500 - Soft Drinks Industry Levy - GOV.UK"
          }
        }
      }

      "when the backend call to get sdilSubscription fails with SDIL_REF" in {
        build
          .user.isAuthorisedAndEnrolledSDILRef
          .sdilBackend.retrieveSubscriptionError("sdil", SDIL_REF)

        WsTestClient.withClient { client =>
          val result1 = createClientRequestGet(client, baseUrl + correctAReturnPath)

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
