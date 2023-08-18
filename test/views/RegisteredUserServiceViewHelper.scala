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

import models.ReturnPeriod
import org.jsoup.nodes.Document

trait RegisteredUserServiceViewHelper extends ServiceViewHelper {


  def warningMessageForPendingReturns(pendingReturns: List[ReturnPeriod]): String = {
    if (pendingReturns.size == 1) {
      val pendingReturn = pendingReturns.head
      val startMonth = pendingReturn.start.format(monthFormatter)
      val endDate = pendingReturn.end.format(monthYearFormatter)
      val deadline = pendingReturn.deadline.format(dateFormatter)
      s"! Warning The return for Super Lemonade Plc from $startMonth to $endDate is due by $deadline."
    } else {
      s"! Warning You have ${pendingReturns.size} overdue returns"
    }
  }

  def overdueBulletMessage(returnPeriod: ReturnPeriod): String = {
    val startMonth = returnPeriod.start.format(monthFormatter)
    val endDate = returnPeriod.end.format(monthYearFormatter)
    s"$startMonth to $endDate"
  }

  def getExpectedBalanceMessage(page: Document, balance: BigDecimal, interest: BigDecimal): String = {
    val formattedBalance = f"£${balance.abs}%,.2f"
    val formattedInterest = f"£${interest.abs}%,.2f"
    if (balance == 0) {
      "Your balance is £0."
    } else if (balance > 0) {
      s"You are ${formattedBalance} in credit."
    } else {
      if (interest < 0) {
        s"Your balance is ${formattedBalance} including ${formattedInterest} of interest."
      } else {
        s"Your balance is ${formattedBalance}."
      }
    }
  }

}
