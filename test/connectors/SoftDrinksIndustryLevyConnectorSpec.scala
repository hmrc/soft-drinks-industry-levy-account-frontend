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

package connectors

import base.TestData.{SDIL_REF, aSubscription}
import models.{OptRetrievedSubscription, RetrievedSubscription}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{clearInvocations, verify, when}
import play.api.libs.json.{JsValue, Json}
import repositories.{CacheMap, SessionCache, SessionKeys}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpResponse, RequestId, SessionId}
import utilities.GenericLogger

import java.net.URL
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class SoftDrinksIndustryLevyConnectorSpec extends HttpClientV2Helper {

  private val mockSessionCache = mock[SessionCache]
  private val connector = new SoftDrinksIndustryLevyConnector(
    http = mockHttp,
    frontendAppConfig = application1.injector.instanceOf[config.FrontendAppConfig],
    sdilSessionCache = mockSessionCache,
    genericLogger = application1.injector.instanceOf[GenericLogger]
  )

  private def correlationHeaderCarrier(requestIdValue: String, sessionIdValue: String): HeaderCarrier =
    HeaderCarrier(
      authorization = Some(Authorization("Bearer incoming-token")),
      sessionId = Some(SessionId(sessionIdValue)),
      requestId = Some(RequestId(requestIdValue)),
      deviceID = Some("device-id-1"),
      otherHeaders = Seq("X-Test-Header" -> "should-not-forward")
    )

  private def assertSanitisedCorrelationIds(outboundHc: HeaderCarrier, incomingHc: HeaderCarrier): Unit = {
    outboundHc.requestId mustBe incomingHc.requestId
    outboundHc.sessionId mustBe incomingHc.sessionId
    outboundHc.authorization mustBe None
    outboundHc.deviceID mustBe None
    outboundHc.otherHeaders mustBe Seq.empty
  }

  private def jsonResponse(status: Int, json: JsValue): HttpResponse =
    HttpResponse(status, Json.stringify(json))

  "SoftDrinksIndustryLevyConnector" - {

    "preserve correlation ids and strip custom headers in outbound GET HeaderCarrier for retrieveSubscription" in {
      val incomingHc = correlationHeaderCarrier("request-id-account-get-1", "session-id-account-get-1")
      clearInvocations(mockHttp)

      when(mockSessionCache.fetchEntry[OptRetrievedSubscription](any(), any())(using any()))
        .thenReturn(Future.successful(None))
      when(requestBuilderExecute[HttpResponse])
        .thenReturn(Future.successful(jsonResponse(200, Json.toJson(aSubscription))))
      when(mockSessionCache.save[OptRetrievedSubscription](any(), any(), any())(using any())).thenReturn(
        Future.successful(
          CacheMap("test", Map(SessionKeys.SUBSCRIPTION -> Json.toJson(OptRetrievedSubscription(Some(aSubscription)))))
        )
      )

      Await.result(connector.retrieveSubscription(SDIL_REF, "sdil", "id")(using incomingHc).value, 1.second)

      val hcCaptor = ArgumentCaptor.forClass(classOf[HeaderCarrier])
      verify(mockHttp).get(any[URL])(using hcCaptor.capture())

      assertSanitisedCorrelationIds(hcCaptor.getValue, incomingHc)
    }
  }
}
