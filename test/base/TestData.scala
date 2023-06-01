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

package base

import models.{Contact, RetrievedActivity, RetrievedSubscription, ReturnPeriod, SdilReturn, ServicePageViewModel, Site, UkAddress}

import java.time.{LocalDate, LocalDateTime, ZoneOffset}

object TestData {

  val currentReturnPeriod = ReturnPeriod(LocalDate.now)
  val pendingReturn1 = currentReturnPeriod.previous
  val pendingReturn2 = pendingReturn1.previous
  val pendingReturn3 = pendingReturn2.previous

  val pendingReturns3 = List(
    pendingReturn3,
    pendingReturn2,
    pendingReturn1
  )

  val pendingReturns1 = List(pendingReturn1)

  val UTR = "0000001611"
  val SDIL_REF = "XKSDIL000000022"

  val aSubscription = RetrievedSubscription(
    utr = UTR,
    sdilRef = SDIL_REF,
    orgName = "Super Lemonade Plc",
    address = UkAddress(List("63 Clifton Roundabout", "Worcester"), "WR53 7CX"),
    activity = RetrievedActivity(smallProducer = false, largeProducer = true, contractPacker = false, importer = false, voluntaryRegistration = false),
    liabilityDate = LocalDate.of(2018, 4, 19),
    productionSites = List(
      Site(
        UkAddress(List("33 Rhes Priordy", "East London"), "E73 2RP"),
        Some("88"),
        Some("Wild Lemonade Group"),
        Some(LocalDate.of(2018, 2, 26))),
      Site(
        UkAddress(List("117 Jerusalem Court", "St Albans"), "AL10 3UJ"),
        Some("87"),
        Some("Highly Addictive Drinks Plc"),
        Some(LocalDate.of(2019, 8, 19)))
    ),
    warehouseSites = List(),
    contact = Contact(Some("Ava Adams"), Some("Chief Infrastructure Agent"), "04495 206189", "Adeline.Greene@gmail.com"),
    deregDate = None
  )

  val submittedDateTime = LocalDateTime.of(2023, 1, 1, 11, 0)

  val emptyReturn = SdilReturn((0, 0), (0, 0), List.empty, (0, 0), (0, 0), (0, 0), (0, 0), submittedOn = Some(submittedDateTime.toInstant(ZoneOffset.UTC)))

  val servicePageViewModel3PendingReturns = ServicePageViewModel(
    pendingReturns3,
    aSubscription,
    None
  )

  val servicePageViewModel1PendingReturns = ServicePageViewModel(
    pendingReturns1,
    aSubscription,
    None
  )

  val servicePageViewModelWithLastReturn = ServicePageViewModel(
    List.empty,
    aSubscription,
    Some(emptyReturn)
  )

  val servicePageViewModelWithNoReturnInfo = ServicePageViewModel(
    List.empty,
    aSubscription,
    None
  )

}