package testSupport.preConditions

import com.github.tomakehurst.wiremock.client.WireMock._
import models.{ReturnPeriod, SdilReturn}
import play.api.libs.json.Json
import testSupport.ITCoreTestData._

case class SdilBackendStub()
                          (implicit builder: PreconditionBuilder)
{


  def retrieveSubscription(identifier: String, refNum: String) = {
    stubFor(
      get(
        urlPathMatching(s"/subscription/$identifier/$refNum"))
        .willReturn(
          ok(Json.toJson(aSubscription).toString())))
    builder
  }

  def retrieveSubscriptionWithDeRegDate(identifier: String, refNum: String) = {
    stubFor(
      get(
        urlEqualTo(s"/subscription/$identifier/$refNum"))
        .willReturn(
          ok(Json.toJson(aSubscriptionWithDeRegDate).toString())))
    builder
  }

  def retrieveSubscriptionNone(identifier: String, refNum: String) = {
    stubFor(
      get(
        urlPathMatching(s"/subscription/$identifier/$refNum"))
        .willReturn(
          notFound()))
    builder
  }


  def retrieveSubscriptionError(identifier: String, refNum: String) = {
    stubFor(
      get(
        urlPathMatching(s"/subscription/$identifier/$refNum"))
        .willReturn(
          serverError()))
    builder
  }

  def retrievePendingReturns(utr: String, pendingReturns: List[ReturnPeriod]) = {
    stubFor(
      get(
        urlPathMatching(s"/returns/$utr/pending"))
        .willReturn(
          ok(Json.toJson(pendingReturns).toString())))
    builder
  }

  def retrievePendingReturnsError(utr: String) = {
    stubFor(
      get(
        urlPathMatching(s"/returns/$utr/pending"))
        .willReturn(
          serverError()))
    builder
  }

  def retrieveReturn(utr: String, period: ReturnPeriod, resp: Option[SdilReturn]) = {
    val uri = s"/returns/$utr/year/${period.year}/quarter/${period.quarter}"
    val response = resp match {
      case Some(sdilReturn) => ok(Json.toJson(sdilReturn).toString())
      case None => notFound()
    }
    stubFor(
      get(
        urlPathMatching(uri))
        .willReturn(
          response))
    builder
  }

  def retrieveReturnError(utr: String, period: ReturnPeriod) = {
    val uri = s"/returns/$utr/year/${period.year}/quarter/${period.quarter}"
    stubFor(
      get(
        urlPathMatching(uri))
        .willReturn(
          serverError()))
    builder
  }

}

