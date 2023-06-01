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

import base.SpecBase

class RetrievedActivitySpec extends SpecBase {

  val retrievedActivityAllFalse = RetrievedActivity(false, false, false, false, false)

  "isLiable" - {
    "should return true when " - {
      "not a small producer" -{
        "and is a large producer" in {
          val rActivity = retrievedActivityAllFalse.copy(largeProducer = true)
          rActivity.isLiable mustBe true
        }
        "and is a contract packer" in {
          val rActivity = retrievedActivityAllFalse.copy(contractPacker = true)
          rActivity.isLiable mustBe true
        }
        "and is a importer" in {
          val rActivity = retrievedActivityAllFalse.copy(importer = true)
          rActivity.isLiable mustBe true
        }
        "and is a large producer, contract packer and importer" in {
          val rActivity = retrievedActivityAllFalse.copy(importer = true, largeProducer = true, contractPacker = true)
          rActivity.isLiable mustBe true
        }
      }
    }

    "return false when" - {
      "a small producer" - {
        "and is a large producer, contract packer and importer" in {
          val rActivity = retrievedActivityAllFalse.copy(importer = true, largeProducer = true, contractPacker = true, smallProducer = true)
          rActivity.isLiable mustBe false
        }
        "and not a large producer, contract packer or importer" in {
          val rActivity = retrievedActivityAllFalse.copy(smallProducer = true)
          rActivity.isLiable mustBe false
        }
      }
    }
  }

  "isVoluntaryMandatory" - {
    "return true" - {
      "when a small producer" - {
        "and a contract packer" in {
          val rActivity = retrievedActivityAllFalse.copy(contractPacker = true, smallProducer = true)
          rActivity.isVoluntaryMandatory mustBe true
        }

        "and a importer" in {
          val rActivity = retrievedActivityAllFalse.copy(importer = true, smallProducer = true)
          rActivity.isVoluntaryMandatory mustBe true
        }

        "a importer and contract packer" in {
          val rActivity = retrievedActivityAllFalse.copy(importer = true, smallProducer = true, contractPacker = true)
          rActivity.isVoluntaryMandatory mustBe true
        }
      }
    }
    "return false" - {
      "when a small producer and large producer" in {
        val rActivity = retrievedActivityAllFalse.copy(largeProducer = true, smallProducer = true)
        rActivity.isVoluntaryMandatory mustBe false
      }
      "when not a small producer" - {
        "and a contract packer" in {
          val rActivity = retrievedActivityAllFalse.copy(contractPacker = true)
          rActivity.isVoluntaryMandatory mustBe false
        }

        "and a importer" in {
          val rActivity = retrievedActivityAllFalse.copy(importer = true)
          rActivity.isVoluntaryMandatory mustBe false
        }

        "and importer and contract packer" in {
          val rActivity = retrievedActivityAllFalse.copy(importer = true)
          rActivity.isVoluntaryMandatory mustBe false
        }
      }
    }
  }

}
