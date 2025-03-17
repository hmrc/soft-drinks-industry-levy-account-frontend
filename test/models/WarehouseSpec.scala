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

import play.api.libs.json._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers

class WarehouseSpec extends AnyWordSpec with Matchers {

  "Warehouse JSON serialization" should {

    "serialize a Warehouse object to JSON" in {
      val warehouse = Warehouse(Some("Test Trading Name"), UkAddress(List("123 Test Street", "Test City"), "AB12 3CD"))
      val expectedJson = Json.parse(
        """
          {
            "address":{
              "lines":["123 Test Street","Test City"],
              "postCode":"AB12 3CD"
            },
              "tradingName":"Test Trading Name"
          }
        """.stripMargin
      )

      Json.toJson(warehouse) mustBe expectedJson
    }

    "deserialize JSON to a Warehouse object" in {
      val json = Json.parse(
        """
          {
            "address":{
              "lines":["123 Test Street","Test City"],
              "postCode":"AB12 3CD"
            },
              "tradingName":"Test Trading Name"
          }
        """.stripMargin
      )

      json.as[Warehouse] mustBe Warehouse(Some("Test Trading Name"), UkAddress(List("123 Test Street", "Test City"), "AB12 3CD"))
    }

    "handle missing tradingName correctly" in {
      val json = Json.parse(
        """
          {
            "address":{
              "lines":["123 Test Street","Test City"],
              "postCode":"AB12 3CD"
            }
          }
        """.stripMargin
      )

      json.as[Warehouse] mustBe Warehouse(None, UkAddress(List("123 Test Street", "Test City"), "AB12 3CD"))
    }
  }
}
