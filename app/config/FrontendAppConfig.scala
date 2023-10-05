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

  val host: String = configuration.getString("microservice.services.soft-drinks-industry-levy-account-frontend.host")
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
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${SafeRedirectUrl(host + request.uri).encodedUrl}"

  val basGatewayBaseUrl: String = configuration.baseUrl("bas-gateway")
  val sdilBaseUrl: String = configuration.baseUrl("soft-drinks-industry-levy")
  val directDebitIsTest: Boolean = configuration.getBoolean("direct-debit.isTest")
  val payApiIsTest: Boolean = configuration.getBoolean("pay-api.isTest")
  val directDebitBaseUrl: String = if (directDebitIsTest) {
    host + controllers.testOnly.routes.TestOnlyController.stubDirectDebitInitialise().url
  } else {
    configuration.baseUrl("direct-debit-backend") + "/direct-debit-backend/sdil-frontend/zsdl/journey/start"
  }

  val directDebitEnabled = configuration.getBoolean("direct-debit.isEnabled")

  val payApiUrl: String = if (payApiIsTest) {
    s"$host${controllers.testOnly.routes.TestOnlyController.stubPayApiInitialise().url}"
  } else {
    s"${configuration.baseUrl("pay-api")}/pay-api/bta/sdil/journey/start"
  }


  val loginUrl: String         = s"$basGatewayBaseUrl/bas-gateway/sign-in"
  val loginContinueUrl: String = s"$host/soft-drinks-industry-levy-account-frontend"
  val signOutUrl: String       = s"$basGatewayBaseUrl/bas-gateway/sign-out-without-state"

  private val exitSurveyBaseUrl: String = configuration.baseUrl("feedback-frontend")
  val exitSurveyUrl: String             = s"$exitSurveyBaseUrl/feedback/soft-drinks-industry-levy-account-frontend"

  val timeout: Int   = configuration.getInt("timeout-dialog.timeout")
  val countdown: Int = configuration.getInt("timeout-dialog.countdown")

  val cacheTtl: Int = configuration.getInt("mongodb.timeToLiveInSeconds")

  val sdilGuidance = configuration.getString("sdilGuidance")
  val sdilRegulations = configuration.getString("sdilRegulations")
  val sdilContact = configuration.getString("sdilContact")
  val creditForExportGuidance = configuration.getString("creditForExportGuidance")
  val howToPayGuidance = configuration.getString("howToPayGuidance")
  val sdilContactNumber = configuration.getString("sdilContactNumber")

}

