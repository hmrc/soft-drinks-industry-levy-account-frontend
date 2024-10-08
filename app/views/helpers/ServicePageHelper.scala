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

package views.helpers

import config.FrontendAppConfig
import controllers.routes
import models.{RetrievedSubscription, ReturnPeriod, SdilReturn}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.insettext.InsetText
import uk.gov.hmrc.govukfrontend.views.viewmodels.warningtext.WarningText

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneId}

object ServicePageHelper {

  lazy val monthFormatter = DateTimeFormatter.ofPattern("MMMM")
  lazy val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
  lazy val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
  lazy val timeFormatter = DateTimeFormatter.ofPattern("h:mma")

  def createVoluntaryRegistrationInsetMessage(implicit messages: Messages): InsetText = {
    val htmlMessage = s"<p>${messages("servicePage.voluntaryOnly.p1")}</p>" +
      s"<p>${messages("servicePage.voluntaryOnly.p2")}" +
      s" <a href = ${routes.ServicePageController.makeAChange.url}>${messages("servicePage.voluntaryOnly.link")}</a>" +
      s" ${messages("servicePage.voluntaryOnly.p3")}</p>"

    InsetText(
      id = Some("voluntaryOnly"),
      content = HtmlContent(htmlMessage)
    )
  }

  def createWarningForOverdueReturns(pendingReturns: List[ReturnPeriod],
                                     orgName: String)
                                    (implicit messages: Messages): WarningText = {
    val content = if(pendingReturns.size == 1) {
      val pendingReturn = pendingReturns.head
      val startMonth = pendingReturn.start.format(monthFormatter)
      val endDate = pendingReturn.end.format(monthYearFormatter)
      val deadline = pendingReturn.deadline.format(dateFormatter)
      messages("servicePage.returnsOverdue1.warning", orgName, startMonth, endDate, deadline)
    } else {
      messages("servicePage.returnsOverdue.warning", pendingReturns.size)
    }
    WarningText(
      iconFallbackText = Some("Warning"),
      content = Text(content)
    )
  }

  def returnsPendingBulletMessage(pendingReturn: ReturnPeriod)
                                 (implicit messages: Messages): String = {

    val startMonth = pendingReturn.start.format(monthFormatter)
    val endDate = pendingReturn.end.format(monthYearFormatter)
    messages("overdueReturn.bullet", startMonth, endDate)
  }

  def noReturnsPendingMessage(lastReturn: SdilReturn)
                             (implicit messages: Messages): InsetText = {
    val currentReturnPeriod = ReturnPeriod(LocalDate.now)
    val lastPeriodStart = currentReturnPeriod.previous.start.format(monthFormatter)
    val lastPeriodEnd = currentReturnPeriod.previous.end.format(monthYearFormatter)
    val submittedOn = lastReturn.submittedOn.map(_.atZone(ZoneId.of("Europe/London"))).getOrElse(Instant.now().atZone(ZoneId.of("Europe/London")))
    val submittedTime = submittedOn.format(timeFormatter).toLowerCase
    val submittedDate = submittedOn.format(dateFormatter)
    val currentPeriodStart = currentReturnPeriod.start.format(monthFormatter)
    val currentPeriodEnd = currentReturnPeriod.end.format(monthYearFormatter)
    val nextReturnDueDate = currentReturnPeriod.deadline.format(dateFormatter)
    InsetText(
      id = Some("lastReturnInset"),
      content = Text(
        messages("noOverdueReturns.paragraph",
          lastPeriodStart,
          lastPeriodEnd,
          submittedTime,
          submittedDate,
          currentPeriodStart,
          currentPeriodEnd,
          nextReturnDueDate
        )
      )
    )
  }

  def finalReturnSentMessage(lastReturn: SdilReturn)
                     (implicit messages: Messages): InsetText = {
    val currentReturnPeriod = ReturnPeriod(LocalDate.now)
    val lastPeriodStart = currentReturnPeriod.previous.start.format(monthFormatter)
    val lastPeriodEnd = currentReturnPeriod.previous.end.format(monthYearFormatter)
    val submittedOn = lastReturn.submittedOn.map(_.atZone(ZoneId.of("Europe/London"))).getOrElse(Instant.now().atZone(ZoneId.of("Europe/London")))
    val submittedTime = submittedOn.format(timeFormatter).toLowerCase
    val submittedDate = submittedOn.format(dateFormatter)
    InsetText(
      id = Some("finalReturnCompleted"),
      content = Text(
        messages("finalReturnCompleted",
          lastPeriodStart,
          lastPeriodEnd,
          submittedTime,
          submittedDate
        )
      )
    )
  }

  def finalReturnRequiredMessage(deRegDate: LocalDate)
                             (implicit messages: Messages) = {

    val deregReturnPeriod = ReturnPeriod(deRegDate)
    val deregPeriodStart = deregReturnPeriod.previous.start.format(monthYearFormatter)
    val deregPeriodEnd = deregReturnPeriod.previous.end.format(monthYearFormatter)
    messages("finalReturnRequired.dereg.paragraph", deregPeriodStart, deregPeriodEnd)
  }

  def payBy(periodsDue: List[ReturnPeriod])(implicit messages: Messages): String = {
    val payByDate = periodsDue.map(_.deadline).min(Ordering.fromLessThan[LocalDate]((a, b) => a.isBefore(b)))
    val formattedDate = payByDate.format(dateFormatter)
    messages("balance.need-to-pay-by", formattedDate)

  }

  def otherPaymentsContent(sdilReference: String)(implicit messages: Messages, config: FrontendAppConfig): String = {
    val guidanceLink = s"""<a class="govuk-link" href="${config.howToPayGuidance}" target="_blank">${messages("howToPay.link")}</a>"""
    s"${messages("howToPay.details.1")} <b>$sdilReference</b>.<br>$guidanceLink"
  }

  def formatAbsPounds(bd: BigDecimal): String = f"£${bd.abs}%,.2f"

  def businessAddress(subscription: RetrievedSubscription): InsetText = {
    val address = subscription.address
    val formattedAddress = s"${subscription.orgName}<br>${address.lines.mkString("<br>")}<br>${address.postCode}"
    InsetText(
      id = Some("businessAddress"),
      content = HtmlContent(formattedAddress)
    )
  }

  def getDeregisteredContent(deregDate: LocalDate)(implicit messages: Messages): String = {
    val formattedDeregDate = deregDate.format(dateFormatter)
    val formattedAccessToDate = deregDate.plusYears(7L).format(dateFormatter)
    messages("servicePage.deregistered.content", formattedDeregDate, formattedAccessToDate)
  }

  def deregContentForPendingP1(deregDate: LocalDate)(implicit messages: Messages): String = {
    val deregReturnPeriod = ReturnPeriod(deregDate)
    val formattedDeregStart = deregReturnPeriod.start.format(dateFormatter)
    val formattedDeregEnd = deregReturnPeriod.end.format(dateFormatter)
    val deregReturnPeriodNext = deregReturnPeriod.next
    val formattedDeregNextStart = deregReturnPeriodNext.start.format(dateFormatter)
    val formattedDeregNextEnd = deregReturnPeriodNext.end.format(dateFormatter)

    messages("servicePage.deregistered.pending.content.p1",
      formattedDeregStart,
      formattedDeregEnd,
      formattedDeregNextStart,
      formattedDeregNextEnd
    )
  }

}
