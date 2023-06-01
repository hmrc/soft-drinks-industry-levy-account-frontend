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

package repositories

import models.ReturnPeriod

object SessionKeys {

  val SUBSCRIPTION = "SUBSCRIPTION"
  val ROSM_REGISTRATION = "ROSM_REGISTRATION"
  def pendingReturn(utr: String) = s"PENDING_RETURNS_UTR_$utr"
  def previousSubmittedReturn(utr: String, returnPeriod: ReturnPeriod) = {
    val year = returnPeriod.year
    val quarter = returnPeriod.quarter
    s"PREVIOUS_SUBMITTED_RETURNS_UTR_${utr}_YEAR${year}_QUARTER_${quarter}"
  }
}