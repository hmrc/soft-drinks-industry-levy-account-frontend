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
import java.time.LocalDateTime
import play.api.libs.json._
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{Format, JsPath, Json, OFormat}


class SdilReturnSpec extends AnyWordSpec with Matchers {

  // Define the SmallProducer case class for the test
  case class SmallProducer(alias: String, sdilRef: String, litreage: (Long, Long))

  object SmallProducer {
    implicit val format: OFormat[SmallProducer] = Json.format[SmallProducer]
  }

  // Define the SdilReturn case class for the test
  case class SdilReturn(
                         ownBrand: (Long, Long) = (0L, 0L),
                         packLarge: (Long, Long) = (0L, 0L),
                         packSmall: List[SmallProducer] = List.empty,
                         importLarge: (Long, Long) = (0L, 0L),
                         importSmall: (Long, Long) = (0L, 0L),
                         `export`: (Long, Long) = (0L, 0L),
                         wastage: (Long, Long) = (0L, 0L),
                         submittedOn: Option[LocalDateTime] = None
                       )

  object SdilReturn {
    implicit val longTupleFormatter: Format[(Long, Long)] = (
      (JsPath \ "lower").format[Long] and
        (JsPath \ "higher").format[Long]
      )((a: Long, b: Long) => (a, b), unlift { (x: (Long, Long)) =>
      Tuple2.unapply(x)
    })

    implicit val returnsFormat: OFormat[SdilReturn] = Json.format[SdilReturn]
  }

  "SdilReturn" should {
    "serialize to JSON correctly" in {
      // Example instance of SdilReturn
      val sdilReturn = SdilReturn(
        ownBrand = (100L, 200L),
        packLarge = (150L, 250L),
        packSmall = List(
          SmallProducer("Alias1", "SD123", (10L, 20L))
        ),
        importLarge = (300L, 400L),
        importSmall = (50L, 60L),
        `export` = (500L, 600L),
        wastage = (70L, 80L),
        submittedOn = Some(LocalDateTime.of(2022, 1, 1, 12, 0, 0, 0))
      )

      // Expected JSON
      val expectedJson = Json.obj(
        "packLarge" -> Json.obj("lower" -> 150L, "higher" -> 250L),
        "export" -> Json.obj("lower" -> 500L, "higher" -> 600L),
        "packSmall" -> Json.arr(Json.obj(
          "alias" -> "Alias1",
          "sdilRef" -> "SD123",
          "litreage" -> Json.arr(10, 20)
        )),
        "ownBrand" -> Json.obj("lower" -> 100L, "higher" -> 200L),
        "importLarge" -> Json.obj("lower" -> 300L, "higher" -> 400L),
        "wastage" -> Json.obj("lower" -> 70L, "higher" -> 80L),
        "submittedOn" -> "2022-01-01T12:00:00",
        "importSmall" -> Json.obj("lower" -> 50L, "higher" -> 60L)
      )

      // Assert that the instance is serialized correctly
      Json.toJson(sdilReturn) mustBe expectedJson
    }

    "deserialize from JSON correctly" in {
      val json = Json.obj(
        "packLarge" -> Json.obj("lower" -> 150L, "higher" -> 250L),
        "export" -> Json.obj("lower" -> 500L, "higher" -> 600L),
        "packSmall" -> Json.arr(Json.obj(
          "alias" -> "Alias1",
          "sdilRef" -> "SD123",
          "litreage" -> Json.arr(10, 20)
        )),
        "ownBrand" -> Json.obj("lower" -> 100L, "higher" -> 200L),
        "importLarge" -> Json.obj("lower" -> 300L, "higher" -> 400L),
        "wastage" -> Json.obj("lower" -> 70L, "higher" -> 80L),
        "submittedOn" -> "2022-01-01T12:00:00",
        "importSmall" -> Json.obj("lower" -> 50L, "higher" -> 60L)
      )

      val expectedReturn = SdilReturn(
        ownBrand = (100L, 200L),
        packLarge = (150L, 250L),
        packSmall = List(
          SmallProducer("Alias1", "SD123", (10L, 20L))
        ),
        importLarge = (300L, 400L),
        importSmall = (50L, 60L),
        `export` = (500L, 600L),
        wastage = (70L, 80L),
        submittedOn = Some(LocalDateTime.of(2022, 1, 1, 12, 0, 0, 0))
      )

      // Deserialize and check equality
      json.as[SdilReturn] mustBe expectedReturn
    }
  }
}
