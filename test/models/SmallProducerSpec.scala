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

import models.SmallProducer
import play.api.libs.json._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers

class SmallProducerSpec extends AnyWordSpec with Matchers {

  "SmallProducer JSON serialization" should {

    "serialize a SmallProducer object to JSON" in {
      val smallProducer = SmallProducer("TestAlias", "XYZ123", (1000L, 2000L))
      val expectedJson = Json.parse(
        """
          {
            "alias": "TestAlias",
            "sdilRef": "XYZ123",
            "litreage": [1000, 2000]
          }
        """.stripMargin
      )

      Json.toJson(smallProducer) mustBe expectedJson
    }

    "deserialize JSON to a SmallProducer object" in {
      val json = Json.parse(
        """
          {
            "alias": "TestAlias",
            "sdilRef": "XYZ123",
            "litreage": [1000, 2000]
          }
        """.stripMargin
      )

      json.as[SmallProducer] mustBe SmallProducer("TestAlias", "XYZ123", (1000L, 2000L))
    }
  }
}
