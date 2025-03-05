package controllers

import testSupport.preConditions.PreconditionHelpers._
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers.include
import org.scalatest.matchers.must.Matchers._
import org.scalatest.EitherValues._
import play.api.http.HeaderNames
import play.api.libs.json.JsValue
import play.api.libs.ws.{DefaultWSCookie, WSClient, WSResponse}
import play.api.test.WsTestClient
import testSupport.{Specifications, TestConfiguration}
import play.api.libs.ws.writeableOf_JsValue


import scala.concurrent.Future

trait ControllerITTestHelper extends Specifications with TestConfiguration with PreconditionHelpers {

  def createClientRequestGet(client: WSClient, url: String): Future[WSResponse] = {
    client.url(url)
      .withFollowRedirects(false)
      .addCookies(DefaultWSCookie("mdtp", authAndSessionCookie))
      .get()
  }

  def createClientRequestPOST(client: WSClient, url: String, json: JsValue): Future[WSResponse] = {
    client.url(url)
      .addCookies(DefaultWSCookie("mdtp", authAndSessionCookie))
      .withHttpHeaders("X-Session-ID" -> "XKSDIL000000022",
        "Csrf-Token" -> "nocheck")
      .withFollowRedirects(false)
      .post(json)
  }

  def testUnauthorisedUser(url: String, optJson: Option[JsValue] = None, requiresSubscription: Boolean = true): Unit = {
    "the user is unauthenticated" - {
      "redirect to gg-signin" in {
         unauthorisedPrecondition
        WsTestClient.withClient { client =>
          val result1 = optJson match {
            case Some(json) => createClientRequestPOST(client, url, json)
            case _ => createClientRequestGet(client, url)
          }

          whenReady(result1) { res =>
            res.status mustBe 303
            res.header(HeaderNames.LOCATION).get must include("/bas-gateway/sign-in")
          }
        }
      }
    }

    "the user is authorised but has an invalid role" - {
      "redirect to sdil home" in {
         authorisedWithInvalidRolePrecondition
        WsTestClient.withClient { client =>
          val result1 = optJson match {
            case Some(json) => createClientRequestPOST(client, url, json)
            case _ => createClientRequestGet(client, url)
          }

          whenReady(result1) { res =>
            res.status mustBe 303
            res.header(HeaderNames.LOCATION).get must include("/soft-drinks-industry-levy")
          }
        }
      }
    }

    "the user is authorised but has an invalid affinity group" - {
      "redirect to sdil home" in {
         authorisedWithInvalidAffinityPrecondition

        WsTestClient.withClient { client =>
          val result1 = optJson match {
            case Some(json) => createClientRequestPOST(client, url, json)
            case _ => createClientRequestGet(client, url)
          }

          whenReady(result1) { res =>
            res.status mustBe 303
            res.header(HeaderNames.LOCATION).get must include("/soft-drinks-industry-levy")
          }
        }
      }
    }
    if (requiresSubscription) {
      "redirect to the index page" - {
        "the user is authorised but has no sdilSubscription" in {
          authorisedWithNoSubscriptionPrecondition
          WsTestClient.withClient { client =>
            val result1 = optJson match {
              case Some(json) => createClientRequestPOST(client, url, json)
              case _ => createClientRequestGet(client, url)
            }

            whenReady(result1) { res =>
              res.status mustBe 303
              res.header(HeaderNames.LOCATION).get must include("/soft-drinks-industry-levy-account-frontend")
            }
          }
        }

        "the user is authorised but has no enrolent" in {
           authorisedButNoEnrolmentsPrecondition
          WsTestClient.withClient { client =>
            val result1 = optJson match {
              case Some(json) => createClientRequestPOST(client, url, json)
              case _ => createClientRequestGet(client, url)
            }

            whenReady(result1) { res =>
              res.status mustBe 303
              res.header(HeaderNames.LOCATION).get must include("/soft-drinks-industry-levy-account-frontend")
            }
          }
        }
      }
    }

    "the user is authorised but has no identifer" - {
      "render the error page" in {
         authorisedButInternalIdPrecondition

        WsTestClient.withClient { client =>
          val result1 = optJson match {
            case Some(json) => createClientRequestPOST(client, url, json)
            case _ => createClientRequestGet(client, url)
          }

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

