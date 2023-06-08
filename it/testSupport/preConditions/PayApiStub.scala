package testSupport.preConditions

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import testSupport.ITCoreTestData._

case class PayApiStub()
                     (implicit builder: PreconditionBuilder) {


  def successCall() = {
    stubFor(
      post(urlPathEqualTo("/pay-api/bta/sdil/journey/start"))
        .willReturn(
          ok(
            Json.toJson(nextUrlResponse).toString()
          )
        )
    )
    builder

  }

  def failureCall = {
    post(urlPathEqualTo("/pay-api/bta/sdil/journey/start"))
      .willReturn(
          serverError()
    )
    builder
  }

}
