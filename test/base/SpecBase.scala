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

package base

import cats.data.EitherT
import cats.implicits._
import controllers.actions._
import errors.AccountErrors
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import service.AccountResult
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait SpecBase
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience {

  def createSuccessAccountResult[T](result: T): AccountResult[T] =
    EitherT.right[AccountErrors](Future.successful(result))

  def createFailureAccountResult[T](error: AccountErrors): AccountResult[T] =
    EitherT.left(Future.successful(error))

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  implicit lazy val messagesAPI = applicationBuilder().build().injector.instanceOf[MessagesApi]
  implicit lazy val messagesProvider = MessagesImpl(Lang("en"), messagesAPI)
  implicit lazy val hc: HeaderCarrier = new HeaderCarrier()
  implicit lazy val ec: ExecutionContext = applicationBuilder().build().injector.instanceOf[ExecutionContext]

  protected def applicationBuilder(): GuiceApplicationBuilder = {
    val bodyParsers = stubControllerComponents().parsers.defaultBodyParser
    new GuiceApplicationBuilder()
      .overrides(
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(Some(TestData.aSubscription), bodyParsers))
      )
  }

  protected def registeredApplicationBuilder(): GuiceApplicationBuilder = {
    val bodyParsers = stubControllerComponents().parsers.defaultBodyParser
    new GuiceApplicationBuilder()
      .overrides(
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(Some(TestData.aSubscription), bodyParsers)),
        bind[RegisteredAction].to[RegisteredActionImp]
      )
  }
}
