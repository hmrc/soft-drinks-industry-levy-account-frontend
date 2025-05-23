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

package controllers.actions

import base.SpecBase
import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.SoftDrinksIndustryLevyConnector
import controllers.routes
import handlers.ErrorHandler
import play.api.mvc.{AnyContent, BodyParsers, Request, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase {

  class Harness(authAction: AuthenticatedAction) {
    def onPageLoad() = authAction { implicit request: Request[AnyContent] => Results.Ok }
  }

  "Auth Action" - {

    "when the user hasn't logged in" - {

      "must redirect the user to unauthorisedController " in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val errorHandler = application.injector.instanceOf[ErrorHandler]
          val sdilConnector = application.injector.instanceOf[SoftDrinksIndustryLevyConnector]
          val ec = application.injector.instanceOf[ExecutionContext]

          val authAction = new AuthenticatedAuthenticatedAction(new FakeFailingAuthConnector(new UnsupportedCredentialRole),
            bodyParsers, sdilConnector, errorHandler)(ec, appConfig)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "the user's session has expired" - {

      "must redirect the unauthenticated controller " in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val errorHandler = application.injector.instanceOf[ErrorHandler]
          val sdilConnector = application.injector.instanceOf[SoftDrinksIndustryLevyConnector]
          val ec = application.injector.instanceOf[ExecutionContext]

          val authAction = new AuthenticatedAuthenticatedAction(new FakeFailingAuthConnector(new UnsupportedCredentialRole),
            bodyParsers, sdilConnector, errorHandler)(ec, appConfig)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "the user doesn't have sufficient enrolments" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val errorHandler = application.injector.instanceOf[ErrorHandler]
          val sdilConnector = application.injector.instanceOf[SoftDrinksIndustryLevyConnector]
          val ec = application.injector.instanceOf[ExecutionContext]

          val authAction = new AuthenticatedAuthenticatedAction(new FakeFailingAuthConnector(new UnsupportedCredentialRole),
            bodyParsers, sdilConnector, errorHandler)(ec, appConfig)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "the user doesn't have sufficient confidence level" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val errorHandler = application.injector.instanceOf[ErrorHandler]
          val sdilConnector = application.injector.instanceOf[SoftDrinksIndustryLevyConnector]
          val ec = application.injector.instanceOf[ExecutionContext]

          val authAction = new AuthenticatedAuthenticatedAction(new FakeFailingAuthConnector(new UnsupportedCredentialRole),
            bodyParsers, sdilConnector, errorHandler)(ec, appConfig)
            val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "the user used an unaccepted auth provider" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val errorHandler = application.injector.instanceOf[ErrorHandler]
          val sdilConnector = application.injector.instanceOf[SoftDrinksIndustryLevyConnector]
          val ec = application.injector.instanceOf[ExecutionContext]

          val authAction = new AuthenticatedAuthenticatedAction(new FakeFailingAuthConnector(new UnsupportedCredentialRole),
            bodyParsers, sdilConnector, errorHandler)(ec, appConfig)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "the user has an unsupported affinity group" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val errorHandler = application.injector.instanceOf[ErrorHandler]
          val sdilConnector = application.injector.instanceOf[SoftDrinksIndustryLevyConnector]
          val ec = application.injector.instanceOf[ExecutionContext]

          val authAction = new AuthenticatedAuthenticatedAction(new FakeFailingAuthConnector(new UnsupportedCredentialRole),
            bodyParsers, sdilConnector, errorHandler)(ec, appConfig)
            val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }

    "the user has an unsupported credential role" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder().build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]
          val errorHandler = application.injector.instanceOf[ErrorHandler]
          val sdilConnector = application.injector.instanceOf[SoftDrinksIndustryLevyConnector]
          val ec = application.injector.instanceOf[ExecutionContext]

          val authAction = new AuthenticatedAuthenticatedAction(new FakeFailingAuthConnector(new UnsupportedCredentialRole),
            bodyParsers, sdilConnector, errorHandler)(ec, appConfig)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }
  }
}

class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}
