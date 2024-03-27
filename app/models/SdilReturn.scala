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

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{Format, JsPath, Json, OFormat}

import java.time.Instant


case class SdilReturn(
                       ownBrand: (Long, Long) = (0L, 0L),
                       packLarge: (Long, Long) = (0L, 0L),
                       packSmall: List[SmallProducer] = List.empty,
                       importLarge: (Long, Long) = (0L, 0L),
                       importSmall: (Long, Long) = (0L, 0L),
                       `export`: (Long, Long) = (0L, 0L),
                       wastage: (Long, Long) = (0L, 0L),
                       submittedOn: Option[Instant] = None
                     )

object SdilReturn {

  implicit val longTupleFormatter: Format[(Long, Long)] = (
    (JsPath \ "lower").format[Long] and
      (JsPath \ "higher").format[Long]
    )((a: Long, b: Long) => (a, b), unlift({ x: (Long, Long) =>
    Tuple2.unapply(x)
  }))

  implicit val smallProducerJson: OFormat[SmallProducer] = Json.format[SmallProducer]
  implicit val returnsFormat: OFormat[SdilReturn] = Json.format[SdilReturn]
}
