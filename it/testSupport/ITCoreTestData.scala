/*
 * Copyright 2026 HM Revenue & Customs
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

package testSupport

import models.*
import org.scalatest.TryValues

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object ITCoreTestData extends TryValues {
  val localDate = LocalDate.now()
  val UTR = "0000001611"
  val SDIL_REF = "XKSDIL000000022"

  val deregDate = localDate.minusMonths(6)


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
        Some(LocalDate.of(2019, 8, 19))),
      Site(
        UkAddress(List("87B North Liddle Street", "Guildford"), "GU34 7CM"),
        Some("94"),
        Some("Monster Bottle Ltd"),
        Some(LocalDate.of(2017, 9, 23))),
      Site(
        UkAddress(List("122 Dinsdale Crescent", "Romford"), "RM95 8FQ"),
        Some("27"),
        Some("Super Lemonade Group"),
        Some(LocalDate.of(2017, 4, 23))),
      Site(
        UkAddress(List("105B Godfrey Marchant Grove", "Guildford"), "GU14 8NL"),
        Some("96"),
        Some("Star Products Ltd"),
        Some(LocalDate.of(2017, 2, 11)))
    ),
    warehouseSites = List(),
    contact = Contact(Some("Ava Adams"), Some("Chief Infrastructure Agent"), "04495 206189", "Adeline.Greene@gmail.com"),
    deregDate = None
  )


  val submittedDateTime = LocalDateTime.of(2023, 1, 1, 11, 0)

  val emptyReturn = SdilReturn((0, 0), (0, 0), List.empty, (0, 0), (0, 0), (0, 0), (0, 0), submittedOn = Some(submittedDateTime))


  val aSubscriptionWithDeRegDate = aSubscription.copy(
    deregDate = Some(deregDate))

  val aSmallProducerSubscription = {
    val activity = aSubscription.activity.copy(voluntaryRegistration = true)
    aSubscription.copy(activity = activity)
  }


  def identifier = "some-id"

  implicit val duration: FiniteDuration = 5.seconds

  def packagingSite1 = Site(
    UkAddress(List("33 Rhes Priordy", "East London"), "E73 2RP"),
    None,
    Some("Wild Lemonade Group"),
    None)

  def packagingSiteListWith1 = Map(("78941132", packagingSite1))

  val address45Characters = Site(
    UkAddress(List("29 Station Pl.", "The Railyard", "Cambridge"), "CB1 2FP"),
    None,
    None,
    None)

  val address47Characters = Site(
    UkAddress(List("29 Station Place", "The Railyard", "Cambridge"), "CB1 2FP"),
    Some("10"),
    None,
    None)

  val address49Characters = Site(
    UkAddress(List("29 Station PlaceDr", "The Railyard", "Cambridge"), "CB1 2FP"),
    None,
    None,
    None)

  def packagingSiteListWith3 = Map(("12345678", address45Characters), ("23456789", address47Characters), ("34567890", address49Characters))

  def currentReturnPeriod = ReturnPeriod(LocalDate.now)
  val pendingReturn1 = currentReturnPeriod.previous
  val pendingReturn2 = pendingReturn1.previous
  val pendingReturn3 = pendingReturn2.previous

  val pendingReturns3 = List(
    pendingReturn3,
    pendingReturn2,
    pendingReturn1
  )

  val pendingReturns1 = List(pendingReturn3)

  val nextUrlResponse = NextUrl("http://example.com")

  val rosmRegistrationOrgJson =
    """{"safeId":"fvp41Gm51rswaeiysohztnrqjdfz7cOnael38omHvuH2ye519ncqiXruPqjBbwewiKdmthpsphun",
      |"isEditable":false,"isAnAgent":false,"isAnIndividual":false,"organisation":{"organisationName":"foo"},
      |"address":{"addressLine1":"50","addressLine2":"The Lane","addressLine3":"The Town",
      |"countryCode":"GB","postalCode":"SM32 5IA"},
      |"contactDetails":{"primaryPhoneNumber":"08926 167394","emailAddress":"qovmlk@rlkioorw.com"}}""".stripMargin

  val rosmRegistrationIndividualJson =
    """{"safeId":"fvp41Gm51rswaeiysohztnrqjdfz7cOnael38omHvuH2ye519ncqiXruPqjBbwewiKdmthpsphun",
      |"isEditable":false,"isAnAgent":false,"isAnIndividual":true,"individual":{"firstName":"name1","lastName":"name2"},
      |"address":{"addressLine1":"50","addressLine2":"The Lane","addressLine3":"The Town",
      |"countryCode":"GB","postalCode":"SM32 5IA"},
      |"contactDetails":{"primaryPhoneNumber":"08926 167394","emailAddress":"qovmlk@rlkioorw.com"}}""".stripMargin

  val ROSMRegistrationOrg = RosmRegistration(
    safeId = "fvp41Gm51rswaeiysohztnrqjdfz7cOnael38omHvuH2ye519ncqiXruPqjBbwewiKdmthpsphun",
    organisation = Some(OrganisationDetails("foo")),
    individual = None,
    address = UkAddress(
      lines = List("50", "The Lane", "The Town"),
      postCode = "SM32 5IA"
    )
  )

  val ROSMRegistrationIndividual = RosmRegistration(
    safeId = "fvp41Gm51rswaeiysohztnrqjdfz7cOnael38omHvuH2ye519ncqiXruPqjBbwewiKdmthpsphun",
    organisation = None,
    individual = Some(IndividualDetails("name1", "name2")),
    address = UkAddress(
      lines = List("50", "The Lane", "The Town"),
      postCode = "SM32 5IA"
    )
  )

  val financialItemReturnCharge: ReturnCharge = ReturnCharge(currentReturnPeriod, BigDecimal(123.45))
  val financialItemReturnChargeInterest: ReturnChargeInterest = ReturnChargeInterest(localDate, BigDecimal(-12.45))
  val financialItemCentralAssessment: CentralAssessment = CentralAssessment(localDate, BigDecimal(1))
  val financialItemCentralAssInterest: CentralAsstInterest = CentralAsstInterest(localDate, BigDecimal(-5))
  val financialItemOfficerAssessment: OfficerAssessment = OfficerAssessment(localDate, BigDecimal(2))
  val financialItemOfficerAssInterest: OfficerAsstInterest = OfficerAsstInterest(localDate, BigDecimal(-3))
  val financialItemPaymentOnAccount: PaymentOnAccount = PaymentOnAccount(localDate, "test", BigDecimal(300))
  val financialItemUnknown: Unknown = Unknown(localDate, "test", BigDecimal(300))
  val allFinancialItems: List[FinancialLineItem] = List(financialItemReturnCharge, financialItemReturnChargeInterest, financialItemCentralAssessment,
    financialItemCentralAssInterest, financialItemOfficerAssessment, financialItemOfficerAssInterest, financialItemPaymentOnAccount, financialItemUnknown)
}
