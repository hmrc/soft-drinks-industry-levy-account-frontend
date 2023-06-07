package testSupport.preConditions

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import testSupport.ITCoreTestData._

case class DirectDebitStub()
(implicit builder: PreconditionBuilder) {


  def successCall() = {
    stubFor(
      post(urlPathEqualTo("/direct-debit-backend"))
        .willReturn(
          ok(
            Json.toJson(directDebitResponse).toString()
          )
        )
    )
    builder

  }

  def failureCall = {
    post(urlPathEqualTo("/direct-debit-backend"))
      .willReturn(
          serverError()
    )
    builder
  }

}
