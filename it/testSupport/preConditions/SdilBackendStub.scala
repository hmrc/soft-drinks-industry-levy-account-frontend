package testSupport.preConditions

import com.github.tomakehurst.wiremock.client.WireMock._
import models.{DisplayDirectDebitResponse, FinancialLineItem, ReturnPeriod, SdilReturn}
import play.api.libs.json.Json
import testSupport.ITCoreTestData._

case class SdilBackendStub()
                          (implicit builder: PreconditionBuilder)
{


  def retrieveSubscription(identifier: String, refNum: String) = {
    stubFor(
      get(
        urlPathEqualTo(s"/subscription/$identifier/$refNum"))
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

  def retrieveSubscriptionSmallProducer(identifier: String, refNum: String) = {
    stubFor(
      get(
        urlEqualTo(s"/subscription/$identifier/$refNum"))
        .willReturn(
          ok(Json.toJson(aSmallProducerSubscription).toString())))
    builder
  }

  def retrieveSubscriptionNone(identifier: String, refNum: String) = {
    stubFor(
      get(
        urlPathEqualTo(s"/subscription/$identifier/$refNum"))
        .willReturn(
          notFound()))
    builder
  }


  def retrieveSubscriptionError(identifier: String, refNum: String) = {
    stubFor(
      get(
        urlPathEqualTo(s"/subscription/$identifier/$refNum"))
        .willReturn(
          serverError()))
    builder
  }

  def retrieveROSM(utr: String, resp: String) = {
    stubFor(
      get(
        urlPathEqualTo(s"/rosm-registration/lookup/$utr"))
        .willReturn(
          ok(resp)))
    builder
  }

  def retrieveROSMNone(utr: String) = {
    stubFor(
      get(
        urlPathEqualTo(s"/rosm-registration/lookup/$utr"))
        .willReturn(
          notFound()))
    builder
  }


  def retrieveROSMError(utr: String) = {
    stubFor(
      get(
        urlPathEqualTo(s"/rosm-registration/lookup/$utr"))
        .willReturn(
          serverError()))
    builder
  }

  def retrievePendingReturns(utr: String, pendingReturns: List[ReturnPeriod]) = {
    stubFor(
      get(
        urlPathEqualTo(s"/returns/$utr/pending"))
        .willReturn(
          ok(Json.toJson(pendingReturns).toString())))
    builder
  }

  def retrievePendingReturnsError(utr: String) = {
    stubFor(
      get(
        urlPathEqualTo(s"/returns/$utr/pending"))
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
        urlPathEqualTo(uri))
        .willReturn(
          response))
    builder
  }

  def retrieveReturnError(utr: String, period: ReturnPeriod) = {
    val uri = s"/returns/$utr/year/${period.year}/quarter/${period.quarter}"
    stubFor(
      get(
        urlPathEqualTo(uri))
        .willReturn(
          serverError()))
    builder
  }

  def balance(sdilRef: String, withAssessment: Boolean, balance: BigDecimal = BigDecimal(1000)) = {
    stubFor(
      get(
        urlPathMatching(s"/balance/$sdilRef/$withAssessment"))
        .willReturn(
          ok(Json.toJson(balance).toString())))
    builder
  }

  def balancefailure(sdilRef: String, withAssessment: Boolean) = {
    stubFor(
      get(
        urlPathMatching(s"/balance/$sdilRef/$withAssessment"))
        .willReturn(
          serverError()))
    builder
  }

  def balanceHistory(sdilRef: String, withAssessment: Boolean, finincialItems: List[FinancialLineItem]) = {
    stubFor(
      get(
        urlPathMatching(s"/balance/$sdilRef/history/all/$withAssessment"))
        .willReturn(
          ok(Json.toJson(finincialItems).toString())))
    builder
  }

  def balanceHistoryfailure(sdilRef: String, withAssessment: Boolean) = {
    stubFor(
      get(
        urlPathMatching(s"/balance/$sdilRef/history/all/$withAssessment"))
        .willReturn(
          serverError()))
    builder
  }

  def checkDirectDebitStatus(sdilRef: String, hasDD: Boolean) = {
    stubFor(
      get(
        urlPathMatching(s"/check-direct-debit-status/$sdilRef"))
        .willReturn(
          ok(Json.toJson(DisplayDirectDebitResponse(hasDD)).toString())))
    builder
  }

  def checkDirectDebitStatusfailure(sdilRef: String) = {
    stubFor(
      get(
        urlPathMatching(s"/check-direct-debit-status/$sdilRef"))
        .willReturn(
          serverError()))
    builder
  }

}

