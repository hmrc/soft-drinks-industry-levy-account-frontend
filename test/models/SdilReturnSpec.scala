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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._

class SdilReturnSpec extends AnyWordSpec with Matchers {

  case class SdilReturn(packSmall: List[SmallProducer])

  object SdilReturn {
    implicit val returnsFormat: OFormat[SdilReturn] = Json.format[SdilReturn]
  }

  "SdilReturn JSON format" should {
    "serialize to JSON correctly" in {
      val returnObj = SdilReturn(List(SmallProducer("TestAlias", "SD123", (100L, 200L))))
      val expectedJson = Json.obj(
        "packSmall" -> Json.arr(Json.obj(
          "alias" -> "TestAlias",
          "sdilRef" -> "SD123",
          "litreage" -> Json.arr(100L, 200L)
        ))
      )
      Json.toJson(returnObj) mustBe expectedJson
    }

    "deserialize from JSON correctly" in {
      val json = Json.obj(
        "packSmall" -> Json.arr(Json.obj(
          "alias" -> "TestAlias",
          "sdilRef" -> "SD123",
          "litreage" -> Json.arr(100L, 200L)
        ))
      )
      val expectedReturn = SdilReturn(List(SmallProducer("TestAlias", "SD123", (100L, 200L))))
      json.as[SdilReturn] mustBe expectedReturn
    }
  }
}
