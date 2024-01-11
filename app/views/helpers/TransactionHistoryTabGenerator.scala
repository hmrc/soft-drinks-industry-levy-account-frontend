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

import com.google.inject.Inject
import models.{FinancialLineItem, ReturnCharge, ReturnChargeInterest, TransactionHistoryItem, Unknown}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.html.components.GovukTable
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{Content, HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.{HeadCell, Table, TableRow}
import uk.gov.hmrc.govukfrontend.views.viewmodels.tabs.{TabItem, TabPanel, Tabs}

import java.time.format.DateTimeFormatter

class TransactionHistoryTabGenerator @Inject()(govukTable: GovukTable) {

  lazy val dateFormatter = DateTimeFormatter.ofPattern("d MMM")
  lazy val monthFormatter = DateTimeFormatter.ofPattern("MMMM")
  lazy val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
  lazy val fullDateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")



  def generateTabs(transactionHistoryForYears: Map[Int, List[TransactionHistoryItem]])
                  (implicit messages: Messages): Tabs = {
    val tabItems: Seq[TabItem] = transactionHistoryForYears.map {
      case (year, transactionHistoryItems) =>
        val panelH2 = s"<h2 class=\"govuk-heading-m\">${year.toString}</h2>"
        val panelTable = s"${govukTable(getTableHistoryForYear(transactionHistoryItems))}"
        TabItem(
          id = Some(s"year-${year.toString}"),
          label = year.toString,
          panel = TabPanel(
            HtmlContent(s"$panelH2 $panelTable")
          )
        )
    }.toSeq
    Tabs(items = tabItems, idPrefix = Some("year"))
  }


  def getTableHistoryForYear(transactionHistoryItemsForYear: List[TransactionHistoryItem])
                            (implicit messages: Messages): Table = {
    val tableRows = transactionHistoryItemsForYear.map(getTableRowForTransactionItem(_))

    Table(
      rows = tableRows,
      head = Some(tableHeaders)
    )
  }

  def getTableRowForTransactionItem(transactionHistoryItem: TransactionHistoryItem)(implicit messages: Messages): Seq[TableRow] = {
    Seq(
      TableRow(
        content = Text(transactionHistoryItem.finincialLineItem.date.format(dateFormatter))
      ),
      TableRow(
        content = getTransaction(transactionHistoryItem.finincialLineItem)
      ),
      TableRow(
        content = HtmlContent(getCredit(transactionHistoryItem))
      ),
      TableRow(
        content = HtmlContent(getDebit(transactionHistoryItem))
      ),
      TableRow(
        content = HtmlContent(formatPounds(transactionHistoryItem.balance))
      )
    )
  }

  def tableHeaders(implicit messages: Messages) = Seq(
    HeadCell(
      content = Text(messages("transactionHistory.table.date"))
    ),
    HeadCell(
      content = Text(messages("transactionHistory.table.transaction"))
    ),
    HeadCell(
      content = Text(messages("transactionHistory.table.credits"))
    ),
    HeadCell(
      content = Text(messages("transactionHistory.table.debits"))
    ),
    HeadCell(
      content = Text(messages("transactionHistory.table.balance"))
    )
  )

  private def getTransaction(item: FinancialLineItem)
                            (implicit messages: Messages): Content = {
    item match {
      case fli: Unknown => Text(fli.messageKey)
      case fli: ReturnCharge =>
        val message = messages(s"transactionHistory.transaction.${fli.messageKey}")
        val fromMonth = fli.period.start.format(monthFormatter)
        val endPeriod = fli.period.end.format(monthYearFormatter)
        val hint = s"""<div class ="govuk-hint">${messages(s"transactionHistory.transaction.${fli.messageKey}.hint", fromMonth, endPeriod)}</div>"""
        HtmlContent(s"$message <br/>$hint")

      case fli: ReturnChargeInterest =>
        val message = messages(s"transactionHistory.transaction.${fli.messageKey}")
        val formattedDate = fli.date.format(fullDateFormatter)
        val hint = s"""<div class ="govuk-hint">${messages(s"transactionHistory.transaction.${fli.messageKey}.hint", formattedDate)}</div>"""
        HtmlContent(s"$message <br/>$hint")
      case fli => Text(messages(s"transactionHistory.transaction.${fli.messageKey}"))
    }
  }

  private def getCredit(transactionHistoryItem: TransactionHistoryItem): String = {
    if(transactionHistoryItem.finincialLineItem.amount > 0) {
      formatPounds(transactionHistoryItem.finincialLineItem.amount)
    } else {
      "£0.00"
    }
  }

  private def getDebit(transactionHistoryItem: TransactionHistoryItem): String = {
    if (transactionHistoryItem.finincialLineItem.amount < 0) {
      s"<span style=\"white-space: nowrap\">${formatPounds(transactionHistoryItem.finincialLineItem.amount)}</span>"
    } else {
      "£0.00"
    }
  }

  def formatPounds(bd: BigDecimal): String = f"<span style=\"white-space: nowrap\">£$bd%,.2f<span style=\"white-space: nowrap\">".replace("£-", "&minus;£")



}
