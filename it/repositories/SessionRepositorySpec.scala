package repositories

import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import java.time.Instant
import java.util.concurrent.TimeUnit

class SessionRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with OptionValues with GuiceOneAppPerSuite with FutureAwaits with DefaultAwaitTimeout with BeforeAndAfterEach {

  val repository: SessionRepository = app.injector.instanceOf[SessionRepository]
  def cacheMap = CacheMap("foo", Map("bar" -> Json.obj("wizz" -> "bang")))

  override def beforeEach(): Unit = {
    await(repository.collection.deleteMany(BsonDocument()).toFuture())
    super.beforeEach()
  }

  "indexes" - {
    "are correct" in {
      repository.indexes.toList.toString() mustBe Seq(
        IndexModel(
          ascending("lastUpdated"),
          IndexOptions()
            .name("session-cache-expiry")
            .expireAfter(
              900,
              TimeUnit.SECONDS
            )
        )
      ).toString
    }
  }

  ".upsert" - {
    "insert successfully when nothing exists in DB" in {
      val timeBeforeTest = Instant.now().toEpochMilli
      await(repository.upsert(cacheMap))
      val timeAfterTest = Instant.now().toEpochMilli

      val updatedRecord = repository.collection.find(Filters.equal("_id", "foo")).toFuture()

      val lastUpdated = await(updatedRecord).head.lastUpdated.toEpochMilli
      assert(lastUpdated > timeBeforeTest || lastUpdated == timeBeforeTest)
      assert(lastUpdated < timeAfterTest || lastUpdated == timeAfterTest)
    }
    "upsert a record that already exists successfully" in {
      val timeBeforeTest = Instant.now().toEpochMilli

      await(repository.collection.countDocuments().head()) mustBe 0
      await(repository.upsert(cacheMap))
      await(repository.collection.countDocuments().head()) mustBe 1

      val updatedCacheMap = CacheMap("foo", Map("bar" -> Json.obj("wizz" -> "bang2")))
      await(repository.upsert(updatedCacheMap))
      val updatedRecord = repository.collection.find(Filters.equal("_id", "foo")).toFuture()

      val timeAfterTest = Instant.now().toEpochMilli

      val lastUpdated = await(updatedRecord).head.lastUpdated.toEpochMilli

      assert(lastUpdated > timeBeforeTest || lastUpdated == timeBeforeTest)
      assert(lastUpdated < timeAfterTest || lastUpdated == timeAfterTest)
    }
  }

  ".removeRecord" - {
    "remove a record successfully" in {
      await(repository.upsert(cacheMap))
      await(repository.collection.countDocuments().head()) mustBe 1

      await(repository.removeRecord(cacheMap.id))
      await(repository.collection.countDocuments().head()) mustBe 0
    }
  }
  ".get" - {
    "get a record successfully" in {
      await(repository.upsert(cacheMap))

      await(repository.collection.countDocuments().head()) mustBe 1

      val result = await(repository.get(cacheMap.id))
      result.get mustBe cacheMap
    }
  }
  ".updateLastUpdated" - {
    "update last updated successfully" in {
      val timeBeforeTest = Instant.now().toEpochMilli
      await(repository.upsert(cacheMap))
      await(repository.collection.countDocuments().head()) mustBe 1
      val result = await(repository.updateLastUpdated("foo"))
      result mustBe true

      val timeAfterTest = Instant.now().toEpochMilli

      val updatedRecord = repository.collection.find(Filters.equal("_id", "foo")).toFuture()

      val lastUpdated = await(updatedRecord).head.lastUpdated.toEpochMilli

      assert(lastUpdated > timeBeforeTest || lastUpdated == timeBeforeTest)
      assert(lastUpdated < timeAfterTest || lastUpdated == timeAfterTest)
    }
  }


}
