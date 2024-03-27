package testSupport

import com.github.tomakehurst.wiremock.WireMockServer
import models.SetupPayApiRequest
import play.api.libs.json.{JsObject, JsString, Json}

import scala.jdk.CollectionConverters._

object PayAPITestHelper {

  def requestedBodyMatchesExpected(wireMockServer: WireMockServer, bodyExpected: SetupPayApiRequest): Boolean = {
    val requestMadeToPayApi = wireMockServer.getAllServeEvents.asScala.toList.map(_.getRequest).filter(_.getUrl.contains("/api/init")).head
    val jsonBodyOfRequest =  Json.parse(requestMadeToPayApi.getBodyAsString).as[JsObject]
    val jsonBodyOfExpectedPost = Json.toJson(bodyExpected).as[JsObject]

    jsonBodyOfRequest == jsonBodyOfExpectedPost
  }
}
