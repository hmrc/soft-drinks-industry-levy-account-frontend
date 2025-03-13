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

package models

import org.scalatest.*
import play.api.libs.json.*
import java.time.LocalDate
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SetupPayApiRequestSpec extends AnyWordSpec with Matchers {

  "SetupPayApiRequest" should {
    "serialize to JSON correctly" in {
      val request = SetupPayApiRequest(
        reference = "order12345",
        amountInPence = 1000,
        dueDate = Some(LocalDate.of(2025, 3, 15)),
        returnUrl = "https://example.com/return",
        backUrl = "https://example.com/cancel"
      )

      val expectedJson = Json.obj(
        "reference" -> "order12345",
        "amountInPence" -> 1000,
        "dueDate" -> "2025-03-15",
        "returnUrl" -> "https://example.com/return",
        "backUrl" -> "https://example.com/cancel"
      )

      Json.toJson(request) mustBe expectedJson
    }

    "deserialize from JSON correctly" in {
      val json = Json.obj(
        "reference" -> "order12345",
        "amountInPence" -> 1000,
        "dueDate" -> "2025-03-15",
        "returnUrl" -> "https://example.com/return",
        "backUrl" -> "https://example.com/cancel"
      )

      val expectedRequest = SetupPayApiRequest(
        reference = "order12345",
        amountInPence = 1000,
        dueDate = Some(LocalDate.of(2025, 3, 15)),
        returnUrl = "https://example.com/return",
        backUrl = "https://example.com/cancel"
      )

      json.as[SetupPayApiRequest] mustBe expectedRequest
    }
  }
}

