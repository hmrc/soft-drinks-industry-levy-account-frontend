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

import models.{ReturnPeriod, SdilReturn}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.insettext.InsetText
import uk.gov.hmrc.govukfrontend.views.viewmodels.warningtext.WarningText

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneId}

object ServicePageHelper {

  lazy val monthFormatter = DateTimeFormatter.ofPattern("MMMM")
  lazy val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
  lazy val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
  lazy val timeFormatter = DateTimeFormatter.ofPattern("h:mma")

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
      iconFallbackText = "Warning",
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
    val submittedTime = lastReturn.submittedOn.getOrElse(Instant.now).atZone(ZoneId.of("Europe/London")).format(timeFormatter).toLowerCase
    val submittedDate = lastReturn.submittedOn.getOrElse(Instant.now).atZone(ZoneId.of("Europe/London")).format(dateFormatter)
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

}
