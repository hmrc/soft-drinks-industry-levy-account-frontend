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

package config

import com.google.inject.{Inject, Singleton}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class FrontendAppConfig @Inject() (configuration: ServicesConfig) {

  val host: String = configuration.getConfString("soft-drinks-industry-levy-account-frontend.host",
    throw new Exception("missing config soft-drinks-industry-levy-account-frontend.host"))
  val appName: String = configuration.getString("appName")

  private val contactHost = configuration.getConfString("contact-frontend.host",
    throw new Exception("missing config contact-frontend.host"))
  private val contactFormServiceIdentifier = "soft-drinks-industry-levy-account-frontend"
  val returnsBaseUrl = configuration.baseUrl("soft-drinks-industry-levy-returns-frontend")


  def startReturnUrl(year: Int, quarter: Int, isNilReturn: Boolean) = {
    s"$returnsBaseUrl/soft-drinks-industry-levy-returns-frontend/submit-return/year/$year/quarter/$quarter/nil-return/$isNilReturn"
  }
  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${SafeRedirectUrl(host + request.uri).encodedUrl}"

  val basGatewayBaseUrl: String = configuration.baseUrl("bas-gateway")
  val sdilBaseUrl: String = configuration.baseUrl("soft-drinks-industry-levy")

  val loginUrl: String         = s"$basGatewayBaseUrl/bas-gateway/sign-in"
  val loginContinueUrl: String = s"$host/soft-drinks-industry-levy-account-frontend"
  val signOutUrl: String       = s"$basGatewayBaseUrl/bas-gateway/sign-out-without-state"

  private val exitSurveyBaseUrl: String = configuration.baseUrl("feedback-frontend")
  val exitSurveyUrl: String             = s"$exitSurveyBaseUrl/feedback/soft-drinks-industry-levy-account-frontend"

  val timeout: Int   = configuration.getInt("timeout-dialog.timeout")
  val countdown: Int = configuration.getInt("timeout-dialog.countdown")

  val cacheTtl: Int = configuration.getInt("mongodb.timeToLiveInSeconds")

  val creditForExportGuidance = configuration.getString("creditForExportGuidance")
}
