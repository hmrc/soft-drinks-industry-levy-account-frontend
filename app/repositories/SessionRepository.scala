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

import config.FrontendAppConfig
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model._
import play.api.libs.json.Format
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}

@Singleton
class SessionRepository @Inject()(
                                   mongoComponent: MongoComponent,
                                   appConfig: FrontendAppConfig)
                                 (implicit ec: ExecutionContext, encryption: Encryption)
  extends PlayMongoRepository[DatedCacheMap](
    collectionName = "session-cache",
    mongoComponent = mongoComponent,
    domainFormat   = DatedCacheMap.MongoFormats.formats,
    indexes        = Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("session-cache-expiry")
          .expireAfter(appConfig.cacheTtl.toLong, TimeUnit.SECONDS)
      )
    ),
    replaceIndexes = false
  ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat


  def upsert(cm: CacheMap): Future[Boolean] = {
    val cmUpdated = DatedCacheMap(cm.id, cm.data)
    val options = ReplaceOptions().upsert(true)
    collection
      .replaceOne(equal("_id", cm.id), cmUpdated, options)
      .toFuture()
      .map { result =>
        result.wasAcknowledged()
      }
  }

  def removeRecord(id: String): Future[Boolean] = {
    collection.deleteOne(equal("_id", id)).toFuture().map(_.getDeletedCount > 0)
  }

  def get(id: String): Future[Option[CacheMap]] = {
    collection.find(equal("_id", id)).headOption().map { datedCacheMap =>
      datedCacheMap.map { value =>
        CacheMap(value._id, value.data)
      }
    }
  }

  def updateLastUpdated(id: String): Future[Boolean] = {
    collection
      .updateOne(
        equal("_id", id),
        set("lastUpdated", Instant.now())
      )
      .toFuture()
      .map { result =>
        result.wasAcknowledged()
      }
  }
}
