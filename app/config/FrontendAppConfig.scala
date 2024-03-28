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

  val accountBaseUrl: String = configuration.baseUrl("soft-drinks-industry-levy-account-frontend")
  val appName: String = configuration.getString("appName")
  lazy val homePage: String = configuration.getString("microservice.services.home-page-url")

  private val contactHost = configuration.getString("contact-frontend.host")
  private val contactFormServiceIdentifier = "soft-drinks-industry-levy-account-frontend"
  val returnsBaseUrl = configuration.baseUrl("soft-drinks-industry-levy-returns-frontend")
  val registrationBaseUrl = configuration.baseUrl("soft-drinks-industry-levy-registration-frontend")
  val variationsBaseUrl = configuration.baseUrl("soft-drinks-industry-levy-variations-frontend")


  def startReturnUrl(year: Int, quarter: Int, isNilReturn: Boolean) = {
    s"$returnsBaseUrl/soft-drinks-industry-levy-returns-frontend/submit-return/year/$year/quarter/$quarter/nil-return/$isNilReturn"
  }

  val makeAChangeUrl = s"$variationsBaseUrl/soft-drinks-industry-levy-variations-frontend/select-change"
  val correctAReturnUrl = s"$variationsBaseUrl/soft-drinks-industry-levy-variations-frontend/correct-return/select"

  val startRegistrationUrl: String = {
    s"$registrationBaseUrl/soft-drinks-industry-levy-registration/start"
  }
  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${SafeRedirectUrl(accountBaseUrl + request.uri).encodedUrl}"

  private val basGatewayBaseUrl: String = configuration.baseUrl("bas-gateway")
  val sdilBaseUrl: String = configuration.baseUrl("soft-drinks-industry-levy")
  private val directDebitIsTest: Boolean = configuration.getBoolean("direct-debit.isTest")
  private val payApiIsTest: Boolean = configuration.getBoolean("pay-api.isTest")
  private val directDebitBaseUrl: String = configuration.baseUrl("direct-debit-backend")
  private val payApiBaseUrl: String = configuration.baseUrl("pay-api")
  val directDebitUrl: String = if (directDebitIsTest) {
    directDebitBaseUrl + controllers.testOnly.routes.TestOnlyController.stubDirectDebitInitialise().url
  } else {
    directDebitBaseUrl + "/direct-debit-backend/sdil-frontend/zsdl/journey/start"
  }

  val directDebitEnabled: Boolean = configuration.getBoolean("direct-debit.isEnabled")

  val payApiUrl: String = if (payApiIsTest) {
    s"$payApiBaseUrl${controllers.testOnly.routes.TestOnlyController.stubPayApiInitialise().url}"
  } else {
    s"$payApiBaseUrl/pay-api/bta/sdil/journey/start"
  }


  val loginUrl: String         = s"$basGatewayBaseUrl/bas-gateway/sign-in"
  val loginContinueUrl: String = s"$accountBaseUrl/soft-drinks-industry-levy-account-frontend"
  val signOutUrl: String       = s"$basGatewayBaseUrl/bas-gateway/sign-out-without-state"

  private val exitSurveyBaseUrl: String = configuration.baseUrl("feedback-frontend")
  val exitSurveyUrl: String             = s"$exitSurveyBaseUrl/feedback/soft-drinks-industry-levy-account-frontend"

  val timeout: Int   = configuration.getInt("timeout-dialog.timeout")
  val countdown: Int = configuration.getInt("timeout-dialog.countdown")

  val cacheTtl: Int = configuration.getInt("mongodb.timeToLiveInSeconds")

  val sdilGuidance: String = configuration.getString("sdilGuidance")
  val sdilRegulations: String = configuration.getString("sdilRegulations")
  val sdilContact: String = configuration.getString("sdilContact")
  val creditForExportGuidance: String = configuration.getString("creditForExportGuidance")
  val howToPayGuidance: String = configuration.getString("howToPayGuidance")
  val sdilContactNumber: String = configuration.getString("sdilContactNumber")

}

