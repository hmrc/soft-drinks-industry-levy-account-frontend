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

import base.SpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.i18n.Messages
import play.api.test.Helpers._
import play.twirl.api.Html

import scala.jdk.CollectionConverters._

trait ViewSpecHelper extends SpecBase {

  def doc(result: Html): Document = Jsoup.parse(contentAsString(result))

  def docFromElements(result: Elements): Document = Jsoup.parse(result.html())

  def testBackLink(doc: Document, href: String = "#") = {
    val bLink = "govuk-back-link"
    "contain the correct back link" in {
      val backLink = doc.getElementsByClass(bLink)
      backLink.text mustBe Messages("site.back")
      backLink.get(0).attr("href") mustBe href
    }
  }

  def testNoBackLink(doc: Document) = {
    val bLink = "govuk-back-link"
    "should not contain a back link" in {
      val backLink = doc.getElementsByClass(bLink)
      backLink.size() mustEqual 0
    }
  }

  def testDetails(doc: Document, expectedDetails: Map[String, String]) = {
    val expectedNumberOfDetails = expectedDetails.size
    val details = doc.getElementsByClass("govuk-details")
    "should include " + expectedNumberOfDetails + " detail sections" in {
      details.size() mustEqual expectedNumberOfDetails
    }
    expectedDetails.zipWithIndex.foreach { case ((detailsSummary, detailsContent), index) =>
      "should include the " + detailsSummary + " details summary with expected content" in {
        val detail = details.get(index)
        detail.getElementsByClass("govuk-details__summary-text").text() mustBe detailsSummary
        detail.getElementsByClass("govuk-details__text").text() mustBe detailsContent
      }
    }
  }

  def validateTimeoutDialog(doc: Document): Unit = {
    val timeoutDialog = doc
      .select("meta")
      .asScala
      .toList
      .collectFirst {
        case element if element.attr("name") == "hmrc-timeout-dialog" => element
      }
    "contain the timeout dialog" - {
      "that has the expected keep alive and signout urls" in {
        timeoutDialog.isDefined mustBe true
        timeoutDialog.get.attr("data-keep-alive-url") must include("/refresh-session")
        timeoutDialog.get.attr("data-sign-out-url") mustBe controllers.auth.routes.AuthController.signOut.url
      }
    }
  }

  def validateNoTimeoutDialog(doc: Document): Unit = {
    val timeoutDialog = doc
      .select("meta")
      .asScala
      .toList
      .collectFirst {
        case element if element.attr("name") == "hmrc-timeout-dialog" => element
      }
    "not contain the timeout dialog" in {
      timeoutDialog.isDefined mustBe false
    }
  }

  def validateTechnicalHelpLinkPresent(doc: Document): Unit = {
    val technicalHelpLink = doc
      .getElementsByClass("govuk-link hmrc-report-technical-issue ")
    "contain a technical help link" - {
      "that has the expected text" in {
        technicalHelpLink
          .text() mustBe "Is this page not working properly? (opens in new tab)"
      }
      "has expected href" in {
        assert(
          technicalHelpLink
            .attr("href")
            .contains("/contact/report-technical-problem")
        )
      }
    }
  }

  def validateAccessibilityStatementLinkPresent(doc: Document): Unit = {
    val footerItems = doc.getElementsByClass("govuk-footer__inline-list-item")
    "accessibility statement exists, text and link are correct" in {
      footerItems.html() must include("/accessibility-statement/soft-drinks-industry-levy-registration-frontend?referrerUrl=%2F")
      footerItems.eachText() must contain("Accessibility statement")
    }
  }
}
