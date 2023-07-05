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

package testSupport

import controllers.ControllerITTestHelper
import models._
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers.{convertToAnyMustWrapper, include}
import play.api.i18n.Messages

import java.time.LocalDate
import java.time.format.DateTimeFormatter

trait TransactionHistoryITHelper extends ControllerITTestHelper {

  lazy val dateFormatter = DateTimeFormatter.ofPattern("d MMM")
  lazy val monthFormatter = DateTimeFormatter.ofPattern("MMMM")
  lazy val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
  lazy val fullDateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")


  val year = 2022
  val year2 = 2021
  val year3 = 2020
  val date1 = LocalDate.of(year, 12, 1)
  val date2 = LocalDate.of(year, 6, 20)
  val date3 = LocalDate.of(year, 1, 30)
  val date4 = LocalDate.of(year2, 12, 1)
  val date5 = LocalDate.of(year2, 6, 20)
  val date6 = LocalDate.of(year2, 1, 30)
  val date7 = LocalDate.of(year3, 12, 1)
  val date8 = LocalDate.of(year3, 6, 20)
  val date9 = LocalDate.of(year3, 1, 30)
  val fi1 = PaymentOnAccount(date1, "test", BigDecimal(132.00))
  val fi2 = ReturnCharge(ReturnPeriod.apply(date2), BigDecimal(-120.00))
  val fi3 = ReturnChargeInterest(date3, BigDecimal(-12.00))
  val fi4 = Unknown(date4, "test", BigDecimal(300.00))
  val fi5 = CentralAssessment(date5, BigDecimal(-100.00))
  val fi6 = CentralAsstInterest(date6, BigDecimal(-10.00))
  val fi7 = OfficerAssessment(date7, BigDecimal(-130.00))
  val fi8 = OfficerAsstInterest(date8, BigDecimal(-13.00))
  val fi9 = ReturnCharge(ReturnPeriod.apply(date9), BigDecimal(-47.00))

  val balanceHistory1Item = List(fi1)
  val balanceHistoryDuplicates = List(fi1, fi1, fi1)
  val balanceHistoryMultiItemsSameYear = List(fi3, fi2, fi1)
  val balanceHistoryMultiItemsDiffYears = List(fi9, fi8, fi7, fi6, fi5, fi4, fi3, fi2, fi1)
  val balanceHistoryMultiItemsDiffYearsNotOrder = List(fi9, fi1, fi5, fi6, fi7, fi2, fi3, fi4, fi8)

  val transitionHistoryItems1Item = Map(year -> List(TransactionHistoryItem(fi1, fi1.amount)))
  val transitionHistoryItemsSameYear = {
    val expectedTransactionHistoryItems = List(
      TransactionHistoryItem(fi1, BigDecimal(0.00)),
      TransactionHistoryItem(fi2, BigDecimal(-132.00)),
      TransactionHistoryItem(fi3, fi3.amount)
    )
    Map(year -> expectedTransactionHistoryItems)
  }

  val transitionHistoryItemsDiffYears = {
    val expectedTransactionHistoryItemsForYear1 = List(
      TransactionHistoryItem(fi1, BigDecimal(0)),
      TransactionHistoryItem(fi2, BigDecimal(-132.00)),
      TransactionHistoryItem(fi3, fi3.amount)
    )

    val expectedTransactionHistoryItemsForYear2 = List(
      TransactionHistoryItem(fi4, BigDecimal(0.00)),
      TransactionHistoryItem(fi5, BigDecimal(-300.00)),
      TransactionHistoryItem(fi6, BigDecimal(-200.00))
    )

    val expectedTransactionHistoryItemsForYear3 = List(
      TransactionHistoryItem(fi7, BigDecimal(-190.00)),
      TransactionHistoryItem(fi8, BigDecimal(-60.00)),
      TransactionHistoryItem(fi9, BigDecimal(-47.00))
    )

    Map(
      year -> expectedTransactionHistoryItemsForYear1,
      year2 -> expectedTransactionHistoryItemsForYear2,
      year3 -> expectedTransactionHistoryItemsForYear3
    )
  }


  def validatePage(body: String, expectedTransactionHistory: Map[Int, List[TransactionHistoryItem]]) = {
    val page = Jsoup.parse(body)
    page.title must include(Messages("Transaction history"))
    page.getElementsByClass("govuk-caption-m").text() mustBe "Super Lemonade Plc"
    page.getElementsByTag("h1").text() mustBe "Transaction history"
    val tabs = page.getElementsByClass("govuk-tabs").first()
    tabs.getElementsByClass("govuk-tabs__title").first().text() mustBe "Contents"
    val tabList = tabs.getElementsByClass("govuk-tabs__list").first().getElementsByTag("li")
    tabList.size() mustBe expectedTransactionHistory.size
    expectedTransactionHistory.zipWithIndex.foreach{
      case((year, transactionHistoryItems), index) =>
        val tabLink = tabList.get(index).getElementsByTag("a").first()
        tabLink.text() mustBe year.toString
        tabLink.attr("href") mustBe s"#year-$year"
        val panel = tabs.getElementById(s"year-$year")

        val expectedPanelClassName = if(index == 0) {"govuk-tabs__panel"} else {"govuk-tabs__panel govuk-tabs__panel--hidden"}
        panel.className() mustBe expectedPanelClassName
        panel.getElementsByClass("govuk-heading-m").text() mustBe year.toString
        val table = panel.getElementsByClass("govuk-table").first()
        val tableHeaders = table.getElementsByClass("govuk-table__header")
        tableHeaders.size() mustBe 5
        tableHeaders.get(0).text() mustBe "Date"
        tableHeaders.get(1).text() mustBe "Transaction"
        tableHeaders.get(2).text() mustBe "Credits"
        tableHeaders.get(3).text() mustBe "Debits"
        tableHeaders.get(4).text() mustBe "Balance"
        val tableRows = table.getElementsByClass("govuk-table__body").first().getElementsByTag("tr")
        tableRows.size() mustBe transactionHistoryItems.size
        transactionHistoryItems.zipWithIndex.foreach{
          case (transactionHistoryItem, index1) =>
            val tableRow = tableRows.get(index1)
            val rowValues = tableRow.getElementsByTag("td")
            rowValues.get(0).text() mustBe expectedDateField(transactionHistoryItem)
            rowValues.get(1).text() mustBe expectedTransactionField(transactionHistoryItem)
            rowValues.get(2).text() mustBe expectedCredit(transactionHistoryItem)
            rowValues.get(3).text() mustBe expectedDebit(transactionHistoryItem)
            rowValues.get(4).text() mustBe formatPounds(transactionHistoryItem.balance)
        }
    }

    val homeLink = page.getElementById("returnToServicePage")
    homeLink.text() mustBe "You can go to your Soft Drinks Industry Levy account"
    homeLink.attr("href") mustBe "/soft-drinks-industry-levy-account-frontend/home"
  }

  private def expectedDateField(transactionHistoryItem: TransactionHistoryItem): String = {
    transactionHistoryItem.finincialLineItem.date.format(dateFormatter)
  }

  private def expectedTransactionField(transactionHistoryItem: TransactionHistoryItem): String = {
    transactionHistoryItem.finincialLineItem match {
      case fli: Unknown => fli.title
      case fli: ReturnCharge =>
        val fromMonth = fli.period.start.format(monthFormatter)
        val endPeriod = fli.period.end.format(monthYearFormatter)
        s"Return for $fromMonth to $endPeriod"
      case fli: ReturnChargeInterest =>
        val formattedDate = fli.date.format(fullDateFormatter)
        s"Interest Charged up to $formattedDate"
      case _: CentralAssessment => "Central Assessment"
      case _: CentralAsstInterest => "Interest on Central Assessment"
      case _: OfficerAssessment => "Officers Assessment"
      case _: OfficerAsstInterest => "Interest on Officers Assessment"
      case _: PaymentOnAccount => "Payment on account"
    }
  }

  private def expectedCredit(transactionHistoryItem: TransactionHistoryItem): String = {
    if (transactionHistoryItem.finincialLineItem.amount > 0) {
      formatPounds(transactionHistoryItem.finincialLineItem.amount)
    } else {
      "£0.00"
    }
  }

  private def expectedDebit(transactionHistoryItem: TransactionHistoryItem): String = {
    if (transactionHistoryItem.finincialLineItem.amount < 0) {
      s"${formatPounds(transactionHistoryItem.finincialLineItem.amount)}"
    } else {
      "£0.00"
    }
  }

  def formatPounds(bd: BigDecimal): String = f"£$bd%,.2f".replace("£-", "−£")

}
