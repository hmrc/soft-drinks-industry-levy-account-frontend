package connectors

import cats.data.EitherT
import errors.{AccountErrors, UnexpectedResponseFromSDIL}
import models.{FinancialLineItem, OptPreviousSubmittedReturn, OptRetrievedSubscription, ReturnPeriod}
import repositories.SessionKeys
import testSupport.ITCoreTestData._
import testSupport.{Specifications, TestConfiguration}
import uk.gov.hmrc.http.HeaderCarrier
import models.FinancialLineItem.formatter
import org.scalatest.matchers.must.Matchers._
import org.scalatest.EitherValues._
import testSupport.preConditions.{PreconditionHelpers, PreconditionBuilder }


class SoftDrinksIndustryLevyConnectorISpec extends Specifications with TestConfiguration with PreconditionHelpers {

  val sdilConnector: SoftDrinksIndustryLevyConnector = app.injector.instanceOf[SoftDrinksIndustryLevyConnector]
  implicit val hc: HeaderCarrier = new HeaderCarrier()
  implicit val builder: PreconditionBuilder = new PreconditionBuilder()


  "retrieveSubscription" - {
    "when the cache is empty" - {
      "and the backend call returns no subscription" - {
        "should return None" - {
          "when searching by utr" in {
            given
              sdilBackend
              .retrieveSubscriptionNone("utr", UTR)

            val res = sdilConnector.retrieveSubscription(UTR, "utr", identifier)

            whenReady(res.value) { result =>
              result mustBe Right(None)
            }
          }

          "when searching by sdilRef" in {
            given
              sdilBackend
              .retrieveSubscriptionNone("sdil", SDIL_REF)

            val res = sdilConnector.retrieveSubscription(SDIL_REF, "sdil", identifier)

            whenReady(res.value) { result =>
              result mustBe Right(None)
            }
          }
        }
      }

      "and the backend call returns a subscription" - {
        "should return the subscription" - {
          "when searching by utr" in {
            given
              sdilBackend
              .retrieveSubscription("utr", UTR)

            val res = sdilConnector.retrieveSubscription(UTR, "utr", identifier)

            whenReady(res.value) { result =>
              result mustBe Right(Some(aSubscription))
            }
          }

          "when searching by sdilRef" in {
            given
              sdilBackend
              .retrieveSubscription("sdil", SDIL_REF)

            val res = sdilConnector.retrieveSubscription(SDIL_REF, "sdil", identifier)

            whenReady(res.value) { result =>
              result mustBe Right(Some(aSubscription))
            }
          }
        }
      }

      "when the backend returns an internal error" - {
        "should return an UnexpectedResponseFromSDIL error" in {
          given
            sdilBackend
            .retrieveSubscriptionError("sdil", SDIL_REF)

          val res = sdilConnector.retrieveSubscription(SDIL_REF, "sdil", identifier)

          whenReady(res.value) { result =>
            result mustBe Left(UnexpectedResponseFromSDIL)
          }
        }
      }
    }

    "when the cache is not empty" - {
      "should not make a backend call" - {
        "and return None when the cache has an empty subscription" - {
          "when searching by utr" in {
            val res = for {
              _ <- EitherT.right[AccountErrors](sessionCache.save(identifier, SessionKeys.SUBSCRIPTION, OptRetrievedSubscription(None)))
              result <- sdilConnector.retrieveSubscription(UTR, "utr", identifier)
            } yield result

            whenReady(res.value) { result =>
              result mustBe Right(None)
            }
          }

          "when searching by sdilRef" in {
            val res = for {
              _ <- EitherT.right[AccountErrors](sessionCache.save(identifier, SessionKeys.SUBSCRIPTION, OptRetrievedSubscription(None)))
              result <- sdilConnector.retrieveSubscription(SDIL_REF, "sdil", identifier)
            } yield result

            whenReady(res.value) { result =>
              result mustBe Right(None)
            }
          }
        }

        "and return the subscription when in the cache" - {
          "when searching by utr" in {
            val res = for {
              _ <- EitherT.right[AccountErrors](sessionCache.save(identifier, SessionKeys.SUBSCRIPTION, OptRetrievedSubscription(Some(aSubscription))))
              result <- sdilConnector.retrieveSubscription(UTR, "utr", identifier)
            } yield result

            whenReady(res.value) { result =>
              result mustBe Right(Some(aSubscription))
            }
          }

          "when searching by sdilRef" in {
            val res = for {
              _ <- EitherT.right[AccountErrors](sessionCache.save(identifier, SessionKeys.SUBSCRIPTION, OptRetrievedSubscription(Some(aSubscription))))
              result <- sdilConnector.retrieveSubscription(SDIL_REF, "sdil", identifier)
            } yield result

            whenReady(res.value) { result =>
              result mustBe Right(Some(aSubscription))
            }
          }
        }
      }
    }
  }

  "returns_pending" - {
    "when the no pending returns in the cache" - {
      "should call the backend" - {
        "and return None when no pending returns" in {
          given
            sdilBackend
            .retrievePendingReturns(UTR, List.empty)

          val res = sdilConnector.returns_pending(identifier, UTR)

          whenReady(res.value) { result =>
            result mustBe Right(List.empty)
          }
        }
        "and return the list of pending return when exist" in {
          given
            sdilBackend
            .retrievePendingReturns(UTR, pendingReturns3)

          val res = sdilConnector.returns_pending(identifier, UTR)

          whenReady(res.value) { result =>
            result mustBe Right(pendingReturns3)
          }
        }

        "and return UnexpectedResponseFromSDIL when the backend returns an unexpectedResponse code" in {
          given
            sdilBackend
            .retrievePendingReturnsError(UTR)

          val res = sdilConnector.returns_pending(identifier, UTR)

          whenReady(res.value) { result =>
            result mustBe Left(UnexpectedResponseFromSDIL)
          }
        }
      }
    }

    "when a pending returns record is in the cache" - {
      "should read the value from the cache" - {
        "and return None when no pending returns" in {
          val res = for {
            _ <- EitherT.right[AccountErrors](sessionCache.save(identifier, SessionKeys.pendingReturn(UTR), List.empty[ReturnPeriod]))
            result <- sdilConnector.returns_pending(identifier, UTR)
          } yield result

          whenReady(res.value) { result =>
            result mustBe Right(List.empty)
          }
        }
        "and return the list of pending return when exist" in {
          val res = for {
            _ <- EitherT.right[AccountErrors](sessionCache.save(identifier, SessionKeys.pendingReturn(UTR), pendingReturns3))
            result <- sdilConnector.returns_pending(identifier, UTR)
          } yield result

          whenReady(res.value) { result =>
            result mustBe Right(pendingReturns3)
          }
        }
      }
    }
  }

  "returns_variable" - {
    "when the no variable returns in the cache" - {
      "should call the backend" - {
        "and return None when no variable returns" in {
          given
            sdilBackend
            .retrieveVariableReturns(UTR, List.empty)

          val res = sdilConnector.returns_variable(identifier, UTR)

          whenReady(res.value) { result =>
            result mustBe Right(List.empty)
          }
        }
        "and return the list of variable return when exist" in {
          given
            sdilBackend
            .retrieveVariableReturns(UTR, pendingReturns3)

          val res = sdilConnector.returns_variable(identifier, UTR)

          whenReady(res.value) { result =>
            result mustBe Right(pendingReturns3)
          }
        }

        "and return UnexpectedResponseFromSDIL when the backend returns an unexpectedResponse code" in {
          given
            sdilBackend
            .retrieveVariableReturnsError(UTR)

          val res = sdilConnector.returns_variable(identifier, UTR)

          whenReady(res.value) { result =>
            result mustBe Left(UnexpectedResponseFromSDIL)
          }
        }
      }
    }

    "when a pending returns record is in the cache" - {
      "should read the value from the cache" - {
        "and return None when no variable returns" in {
          val res = for {
            _ <- EitherT.right[AccountErrors](sessionCache.save(identifier, SessionKeys.variableReturn(UTR), List.empty[ReturnPeriod]))
            result <- sdilConnector.returns_variable(identifier, UTR)
          } yield result

          whenReady(res.value) { result =>
            result mustBe Right(List.empty)
          }
        }
        "and return the list of variable return when exist" in {
          val res = for {
            _ <- EitherT.right[AccountErrors](sessionCache.save(identifier, SessionKeys.variableReturn(UTR), pendingReturns3))
            result <- sdilConnector.returns_variable(identifier, UTR)
          } yield result

          whenReady(res.value) { result =>
            result mustBe Right(pendingReturns3)
          }
        }
      }
    }
  }


  "returns_get" - {
    "when the no previous submitted returns in the cache" - {
      "should call the backend" - {
        "and return None when no previous return submitted" in {
          given
            sdilBackend
            .retrieveReturn(UTR, currentReturnPeriod.previous, None)

          val res = sdilConnector.returns_get(UTR, currentReturnPeriod.previous, identifier)

          whenReady(res.value) { result =>
            result mustBe Right(None)
          }
        }
        "and return the sdil return when exists" in {
          given
            sdilBackend
            .retrieveReturn(UTR, currentReturnPeriod.previous, Some(emptyReturn))

          val res = sdilConnector.returns_get(UTR, currentReturnPeriod.previous, identifier)

          whenReady(res.value) { result =>
            result mustBe Right(Some(emptyReturn))
          }
        }

        "and return UnexpectedResponseFromSDIL when the backend returns an unexpectedResponse code" in {
          given
            sdilBackend
            .retrieveReturnError(UTR, currentReturnPeriod.previous)

          val res = sdilConnector.returns_get(UTR, currentReturnPeriod.previous, identifier)

          whenReady(res.value) { result =>
            result mustBe Left(UnexpectedResponseFromSDIL)
          }
        }
      }
    }

    "when a submitted returns record is in the cache for the given period" - {
      "should read the value from the cache" - {
        "and return None when no return submitted for period" in {
          val res = for {
            _ <- EitherT.right[AccountErrors](sessionCache.save(identifier, SessionKeys.previousSubmittedReturn(UTR, currentReturnPeriod.previous), OptPreviousSubmittedReturn(None)))
            result <- sdilConnector.returns_get(UTR, currentReturnPeriod.previous, identifier)
          } yield result

          whenReady(res.value) { result =>
            result mustBe Right(None)
          }
        }
        "and return the submitted return when exist" in {
          val res = for {
            _ <- EitherT.right[AccountErrors](sessionCache.save(identifier, SessionKeys.previousSubmittedReturn(UTR, currentReturnPeriod.previous), OptPreviousSubmittedReturn(Some(emptyReturn))))
            result <- sdilConnector.returns_get(UTR, currentReturnPeriod.previous, identifier)
          } yield result

          whenReady(res.value) { result =>
            result mustBe Right(Some(emptyReturn))
          }
        }
      }
    }
  }

  "balance" - {
    "when assessment is true" - {
      "and there is no balance in the cache" - {
        "should call the backend" - {
          "and return the balance when sucessful" in {
            given
              sdilBackend
              .balance(aSubscription.sdilRef, true)

            val res = sdilConnector.balance(aSubscription.sdilRef, true, identifier)

            whenReady(res.value) { result =>
              result mustBe Right(BigDecimal(1000))
            }
          }

          "and return UnexpectedResponseFromSDIL when call fails" in {
            given
              sdilBackend
              .balancefailure(aSubscription.sdilRef, true)

            val res = sdilConnector.balance(aSubscription.sdilRef, true, identifier)

            whenReady(res.value) { result =>
              result mustBe Left(UnexpectedResponseFromSDIL)
            }
          }
        }
      }

      "and the balance is in the cache" - {
        "should return the balance" in {
          val res = for {
            _ <- EitherT.right[AccountErrors](sessionCache.save(identifier, SessionKeys.balance(true), BigDecimal(1000)))
            result <- sdilConnector.balance(aSubscription.sdilRef, true, identifier)
          } yield result

          whenReady(res.value) { result =>
            result mustBe Right(BigDecimal(1000))
          }
        }
      }
    }
    "when assessment is false" - {
      "and there is no balance in the cache" - {
        "should call the backend" - {
          "and return the balance when sucessful" in {
            given
              sdilBackend
              .balance(aSubscription.sdilRef, false)

            val res = sdilConnector.balance(aSubscription.sdilRef, false, identifier)

            whenReady(res.value) { result =>
              result mustBe Right(BigDecimal(1000))
            }
          }

          "and return UnexpectedResponseFromSDIL when call fails" in {
            given
              sdilBackend
              .balancefailure(aSubscription.sdilRef, false)

            val res = sdilConnector.balance(aSubscription.sdilRef, false, identifier)

            whenReady(res.value) { result =>
              result mustBe Left(UnexpectedResponseFromSDIL)
            }
          }
        }
      }

      "and the balance is in the cache" - {
        "should return the balance from the cache" in {
          val res = for {
            _ <- EitherT.right[AccountErrors](sessionCache.save(identifier, SessionKeys.balance(false), BigDecimal(1000)))
            result <- sdilConnector.balance(aSubscription.sdilRef, false, identifier)
          } yield result

          whenReady(res.value) { result =>
            result mustBe Right(BigDecimal(1000))
          }
        }
      }
    }
  }

  "balanceHistory " - {
    "when assessment is true " - {
      "and there is no balanceHistory in the cache " - {
        "should call the backend " - {
          "and return the balanceHistory when successful" in {
            given
              sdilBackend
              .balanceHistory(aSubscription.sdilRef, withAssessment = true, allFinancialItems)

            val res = sdilConnector.balanceHistory(aSubscription.sdilRef, withAssessment = true, identifier)

            whenReady(res.value) { result =>
              result mustBe Right(allFinancialItems)
            }
          }

          "and return UnexpectedResponseFromSDIL when call fails" in {
            given
              sdilBackend
              .balanceHistoryfailure(aSubscription.sdilRef, withAssessment = true)

            val res = sdilConnector.balanceHistory(aSubscription.sdilRef, withAssessment = true, identifier)

            whenReady(res.value) { result =>
              result mustBe Left(UnexpectedResponseFromSDIL)
            }
          }
        }
      }

      "and the balanceHistory is in the cache" - {
        "should return the balanceHistory" in {
          val res = for {
            _ <- EitherT.right[AccountErrors](sessionCache.save[List[FinancialLineItem]](identifier, SessionKeys.balanceHistory(true), allFinancialItems))
            result <- sdilConnector.balanceHistory(aSubscription.sdilRef, withAssessment = true, identifier)
          } yield result

          whenReady(res.value) { result =>
            result mustBe Right(allFinancialItems)
          }
        }
      }
    }
    "when assessment is false " - {
      "and there is no balanceHistory in the cache " - {
        "should call the backend " - {
          "and return an empty list when no history and when successful " in {
            given
              sdilBackend
              .balanceHistory(aSubscription.sdilRef, false, List.empty)

            val res = sdilConnector.balanceHistory(aSubscription.sdilRef, withAssessment = false, identifier)

            whenReady(res.value) { result =>
              result mustBe Right(List.empty)
            }
          }

          "and return UnexpectedResponseFromSDIL when call fails" in {
            given
              sdilBackend
              .balanceHistoryfailure(aSubscription.sdilRef, withAssessment = false)

            val res = sdilConnector.balanceHistory(aSubscription.sdilRef, withAssessment = false, identifier)

            whenReady(res.value) { result =>
              result mustBe Left(UnexpectedResponseFromSDIL)
            }
          }
        }
      }

      "and the balanceHistory is in the cache" - {
        "should return the balanceHistory from the cache" in {
          val res = for {
            _ <- EitherT.right[AccountErrors](sessionCache.save[List[FinancialLineItem]](identifier, SessionKeys.balanceHistory(false), allFinancialItems))
            result <- sdilConnector.balanceHistory(aSubscription.sdilRef, false, identifier)
          } yield result

          whenReady(res.value) { result =>
            result mustBe Right(allFinancialItems)
          }
        }
      }
    }
  }

  "checkDirectDebitStatus " - {
    "when the user has a direct debit setup " - {
      "should call the backend " - {
        "and return true" in {
          given
            sdilBackend
            .checkDirectDebitStatus(SDIL_REF, hasDD = true)

          val res = sdilConnector.checkDirectDebitStatus(SDIL_REF)

          whenReady(res.value) { result =>
            result mustBe Right(true)
          }
        }
      }
      "when the user has no direct debit setup " - {
        "should call the backend " - {
          "and return false" in {
            given
              sdilBackend
              .checkDirectDebitStatus(SDIL_REF, hasDD = false)

            val res = sdilConnector.checkDirectDebitStatus(SDIL_REF)

            whenReady(res.value) { result =>
              result mustBe Right(false)
            }
          }
        }
        "should return UnexpectedResponseFromSDIL when the backend returns an unexpectedResponse code" in {
          given
            sdilBackend
            .checkDirectDebitStatusfailure(SDIL_REF)

          val res = sdilConnector.checkDirectDebitStatus(SDIL_REF)

          whenReady(res.value) { result =>
            result mustBe Left(UnexpectedResponseFromSDIL)
          }
        }
      }
    }
  }
}
