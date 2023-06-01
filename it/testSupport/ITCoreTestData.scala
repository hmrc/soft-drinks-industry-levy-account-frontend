package testSupport

import models._
import org.scalatest.TryValues

import java.time.{LocalDate, LocalDateTime, ZoneOffset}
import scala.concurrent.duration.DurationInt

object ITCoreTestData extends TryValues {
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

  val emptyReturn = SdilReturn((0, 0), (0, 0), List.empty, (0, 0), (0, 0), (0, 0), (0, 0), submittedOn = Some(submittedDateTime.toInstant(ZoneOffset.UTC)))


  val aSubscriptionWithDeRegDate = aSubscription.copy(
    deregDate = Some(LocalDate.of(2022, 2, 11)))


  def identifier = "some-id"

  implicit val duration = 5.seconds

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

  val pendingReturns1 = List(pendingReturn1)
}
