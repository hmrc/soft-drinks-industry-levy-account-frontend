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
import java.time.LocalDate

class ServicePageViewModelSpec extends AnyWordSpec with Matchers {

  val testSubscription = RetrievedSubscription(
    utr = "1234567890",
    sdilRef = "XYZ123",
    orgName = "Test Org",
    address = UkAddress(
      lines = List("123 Street"),
      postCode = "AB1 2CD"
    ),
    activity = RetrievedActivity(
      smallProducer = true,
      largeProducer = false,
      contractPacker = false,
      importer = false,
      voluntaryRegistration = false
    ),
    liabilityDate = LocalDate.of(2022, 1, 1),
    productionSites = List(
      Site(
        address = UkAddress(
          lines = List("456 Street"),
          postCode = "CD3 4EF"
        ), None, None, None
      )
    ),
    warehouseSites = List.empty,
    contact = Contact(
      name = Some("Test User"),
      None,
      phoneNumber = ("0123456789"),
      email = ("test@example.com")
    ),
    deregDate = Some(LocalDate.of(2024, 3, 13))
  )


  "RegisteredUserServicePageViewModel" should {

    "create an instance with correct values" in {
      val model = RegisteredUserServicePageViewModel(
        overdueReturns = List.empty,
        sdilSubscription = testSubscription,
        optLastReturn = None,
        balance = 100.50,
        interest = 2.75,
        optHasExistingDD = Some(true)
      )

      model.optHasExistingDD mustBe Some(true)
    }

    "support equality checks" in {
      val model1 = RegisteredUserServicePageViewModel(List.empty, testSubscription)
      val model2 = RegisteredUserServicePageViewModel(List.empty, testSubscription)

      model1 mustBe model2
    }
  }

  "DeregisteredUserServicePageViewModel" should {

    "create an instance correctly" in {
      val model = DeregisteredUserServicePageViewModel(
        sdilSubscription = testSubscription,
        deregDate = LocalDate.of(2024, 3, 13),
        hasVariableReturns = true,
        optLastReturn = None,
        balance = 50.25,
        needsToSendFinalReturn = false
      )


      model.needsToSendFinalReturn mustBe false
    }

    "match correctly in a pattern match" in {
      val model: ServicePageViewModel = DeregisteredUserServicePageViewModel(
        sdilSubscription = testSubscription,
        deregDate = LocalDate.of(2024, 3, 13),
        hasVariableReturns = false,
        needsToSendFinalReturn = true
      )

      model match {
        case _: RegisteredUserServicePageViewModel  => fail("Should not match RegisteredUserServicePageViewModel")
        case _: DeregisteredUserServicePageViewModel => succeed
        case _ => fail("Should match one of the known models")
      }
    }
  }
}

