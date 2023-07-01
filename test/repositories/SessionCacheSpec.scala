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

import base.SpecBase
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsBoolean, JsString, Json}

import scala.concurrent.Future

class SessionCacheSpec extends SpecBase with MockitoSugar {

  val mockSessionRepository = mock[SessionRepository]
  val cascadeUpsert = applicationBuilder().build().injector.instanceOf[CascadeUpsert]

  val sessionCache = new SessionCache(mockSessionRepository, cascadeUpsert)

  "save" - {
    "when there is no record in the database should" - {
      "upsert the data to mongo" in {
        val dataToUpsert = CacheMap("sessionId", Map("test" -> JsString("abc")))
        when(mockSessionRepository.get("sessionId")).thenReturn(Future.successful(None))
        when(mockSessionRepository.upsert(dataToUpsert)).thenReturn(Future.successful(true))

        val res = sessionCache.save[String]("sessionId", "test", "abc")

        whenReady(res) { result =>
          result mustBe dataToUpsert
        }
      }
    }

    "when there is data in database should" - {
      "override the value then upsert" - {
        "the key already exists" in {
          val initialData = CacheMap("sessionId", Map("test" -> JsString("abc")))
          val dataToUpsert = CacheMap("sessionId", Map("test" -> JsString("efg")))

          when(mockSessionRepository.get("sessionId")).thenReturn(Future.successful(Some(initialData)))
          when(mockSessionRepository.upsert(dataToUpsert)).thenReturn(Future.successful(true))

          val res = sessionCache.save("sessionId","test", "efg")

          whenReady(res) { result =>
            result mustBe dataToUpsert
          }
        }
      }

      "add the value to map and then upsert" - {
        "when the key does not already exists" in {
          val initialData = CacheMap("sessionId", Map("test" -> JsString("abc")))
          val dataToUpsert = CacheMap(
            "sessionId",
            Map("test" -> JsString("abc"), "test1" -> JsString("efg"))
          )
          when(mockSessionRepository.get("sessionId")).thenReturn(Future.successful(Some(initialData)))
          when(mockSessionRepository.upsert(dataToUpsert)).thenReturn(Future.successful(true))

          val res = sessionCache.save("sessionId", "test1", "efg")

          whenReady(res) { result =>
            result mustBe dataToUpsert
          }
        }
      }
    }

    "when a mongo failure occurs" - {
      "should return an empty cache map" in {
        val initialData = CacheMap("sessionId", Map("test" -> JsString("abc")))

        val dataToUpsert = CacheMap(
          "sessionId",
          Map("test" -> JsString("abc"), "test1" -> JsString("efg"))
        )
        when(mockSessionRepository.get("sessionId")).thenReturn(Future.successful(Some(initialData)))
        when(mockSessionRepository.upsert(dataToUpsert)).thenReturn(Future.failed(new Exception("error")))
        val res = sessionCache.save("sessionId", "test1", "efg")

        whenReady(res) { result =>
          result mustBe CacheMap("sessionId", Map.empty)
        }
      }
    }
  }

  "removeRecord" - {
    "remove all the data for session id and return true" in {
      when(mockSessionRepository.removeRecord("sessionId")).thenReturn(Future.successful(false))

      val res = sessionCache.removeRecord("sessionId")

      whenReady(res) { result =>
        result mustBe false
      }
    }

    "when a mongo failure occurs" - {
      "should return false" in {
        when(mockSessionRepository.removeRecord("sessionId")).thenReturn(Future.failed(new Exception("error")))
        val res = sessionCache.removeRecord("sessionId")

        whenReady(res) { result =>
          result mustBe false
        }
      }
    }
  }
  "fetch" - {
    "when the session cache is empty" - {
      "should return None" in {
        when(mockSessionRepository.get("sessionId")).thenReturn(Future.successful(None))

        val res = sessionCache.fetch("sessionId")

        whenReady(res) { result =>
          result mustBe None
        }
      }
    }

    "the session cache contains data" - {
      "should return the data" in {
        val data = CacheMap(
          "sessionId",
          Map("test" -> JsString("abc"), "test1" -> JsString("efg"))
        )
        when(mockSessionRepository.get("sessionId")).thenReturn(Future.successful(Some(data)))

        val res = sessionCache.fetch("sessionId")

        whenReady(res) { result =>
          result mustBe Some(data)
        }
      }
    }

    "when a mongo failure occurs" - {
      "should return None" in {
        when(mockSessionRepository.get("sessionId")).thenReturn(Future.failed(new Exception("error")))
        val res = sessionCache.fetch("sessionId")

        whenReady(res) { result =>
          result mustBe None
        }
      }
    }
  }

  "fetchEntry" - {
    "when the session cache is empty" - {
      "should return None" in {
        when(mockSessionRepository.get("sessionId")).thenReturn(Future.successful(None))

        val res = sessionCache.fetchEntry[String]("sessionId", "test1")

        whenReady(res) { result =>
          result mustBe None
        }
      }
    }

    "the session cache contains data" - {
      "should return the data" in {
        val data = CacheMap(
          "sessionId",
          Map("test" -> JsString("abc"), "test1" -> JsString("efg"))
        )
        when(mockSessionRepository.get("sessionId")).thenReturn(Future.successful(Some(data)))

        val res = sessionCache.fetchEntry[String]("sessionId", "test")

        whenReady(res) { result =>
          result mustBe Some("abc")
        }
      }
    }

    "when a mongo failure occurs" - {
      "should return None" in {
        val data = CacheMap(
          "sessionId",
          Map("test" -> Json.obj("abc" -> JsBoolean(true)), "test1" -> JsString("efg"))
        )
        when(mockSessionRepository.get("sessionId")).thenReturn(Future.successful(Some(data)))

        val res = sessionCache.fetchEntry[String]("sessionId", "test")

        whenReady(res) { result =>
          result mustBe None
        }
      }
    }
  }

  "extendSession" - {
    "extend the session and return true" in {
      when(mockSessionRepository.updateLastUpdated("sessionId")).thenReturn(Future.successful(true))
      val res = sessionCache.extendSession("sessionId")

      whenReady(res) { result =>
        result mustBe true
      }
    }

    "when a mongo failure occurs" - {
      "should return false" in {
        when(mockSessionRepository.updateLastUpdated("sessionId")).thenReturn(Future.failed(new Exception("error")))
        val res = sessionCache.extendSession("sessionId")

        whenReady(res) { result =>
          result mustBe false
        }
      }
    }
  }
}
