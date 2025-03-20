/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._

class CacheMapSpec extends AnyWordSpec with Matchers {

  val greeting: JsValue = Json.toJson("hello")

  "CacheMap JSON format" should {
    "serialize to JSON correctly" in {
      val returnObj = CacheMap("1", Map("test" -> greeting))
      val expectedJson = Json.obj(
        "id" -> "1",
        "data" -> Json.obj("test" -> greeting)
      )

      Json.toJson(returnObj) mustBe expectedJson
    }

    "deserialize from JSON correctly" in {
      val json = Json.obj(
        "id" -> "1",
        "data" -> Json.obj("test" -> greeting)
      )

      val expectedObj = CacheMap("1", Map("test" -> greeting))

      json.as[CacheMap] mustBe expectedObj
    }
  }
}
