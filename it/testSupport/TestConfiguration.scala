package testSupport

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{configureFor, reset, resetAllScenarios}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import controllers.actions._
import org.scalatest.concurrent.{IntegrationPatience, PatienceConfiguration}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite, TestSuite}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{CookieHeaderEncoding, MessagesControllerComponents, Session, SessionCookieBaker}
import play.api.test.Helpers._
import play.api.{Application, Environment, Mode}
import repositories.{SessionCache, SessionRepository}
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto
import uk.gov.hmrc.play.health.HealthController
import utilities.GenericLogger

import java.time.{Clock, ZoneOffset}
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

trait TestConfiguration
  extends GuiceOneServerPerSuite
    with IntegrationPatience
    with PatienceConfiguration
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  me: Suite with TestSuite =>

  val wiremockHost: String = "localhost"
  val wiremockPort: Int = Port.randomAvailable

  val baseUrl = s"http://localhost:$port/soft-drinks-industry-levy-account-frontend"
  val testOnlyBaseUrl = s"http://localhost:$port/test-only"

  val sessionId = "sessionId-eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"
  val xSessionId: (String, String) = "X-Session-ID" -> sessionId
  val xRequestId: (String, String) = "X-Request-ID" -> sessionId
  val AUTHORIZE_HEADER_VALUE =
    "Bearer BXQ3/Treo4kQCZvVcCqKPhhpBYpRtQQKWTypn1WBfRHWUopu5V/IFWF5phY/fymAP1FMqQR27MmCJxb50Hi5GD6G3VMjMtSLu7TAAIuqDia6jByIpXJpqOgLQuadi7j0XkyDVkl0Zp/zbKtHiNrxpa0nVHm3+GUC4H2h4Ki8OjP9KwIkeIPK/mMlBESjue4V"

  val sessionBaker: SessionCookieBaker = app.injector.instanceOf[SessionCookieBaker]
  val cookieHeaderEncoding: CookieHeaderEncoding = app.injector.instanceOf[CookieHeaderEncoding]
  val sessionCookieCrypto: SessionCookieCrypto = app.injector.instanceOf[SessionCookieCrypto]

  def createSessionCookieAsString(sessionData: Map[String, String]): String = {
    val sessionCookie = sessionBaker.encodeAsCookie(Session(sessionData))
    val encryptedSessionCookieValue =
      sessionCookieCrypto.crypto.encrypt(PlainText(sessionCookie.value)).value
    val encryptedSessionCookie =
      sessionCookie.copy(value = encryptedSessionCookieValue)
    cookieHeaderEncoding.encodeCookieHeader(Seq(encryptedSessionCookie))
  }
  val authData = Map("authToken" -> AUTHORIZE_HEADER_VALUE)
  val sessionAndAuth  = Map("authToken" -> AUTHORIZE_HEADER_VALUE, "sessionId" -> sessionId)

  lazy val sessionRepository: SessionRepository = app.injector.instanceOf[SessionRepository]
  lazy val sessionCache: SessionCache = app.injector.instanceOf[SessionCache]

  val authCookie: String = createSessionCookieAsString(authData).substring(5)
  val authAndSessionCookie: String = createSessionCookieAsString(sessionAndAuth).substring(5)
  abstract override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(
      timeout = Span(4, Seconds),
      interval = Span(50, Millis))

  lazy val config = Map(
    s"microservice.services.auth.host" -> s"$wiremockHost",
    s"microservice.services.auth.port" -> s"$wiremockPort",
    s"microservice.services.bas-gateway.host" -> s"$wiremockHost",
    s"microservice.services.bas-gateway.port" -> s"$wiremockPort",
    s"microservice.services.soft-drinks-industry-levy-returns-frontend.host" -> s"$wiremockHost",
    s"microservice.services.soft-drinks-industry-levy-returns-frontend.port" -> s"$wiremockPort",
    s"microservice.services.soft-drinks-industry-levy-registration-frontend.host" -> s"$wiremockHost",
    s"microservice.services.soft-drinks-industry-levy-registration-frontend.port" -> s"$wiremockPort",
    s"microservice.services.soft-drinks-industry-levy.host" -> s"$wiremockHost",
    s"microservice.services.soft-drinks-industry-levy.port" -> s"$wiremockPort",
    s"microservice.services.direct-debit-backend.host" -> s"$wiremockHost",
    s"microservice.services.direct-debit-backend.port" -> s"$wiremockPort",
    s"microservice.services.pay-api.host" -> s"$wiremockHost",
    s"microservice.services.pay-api.port" -> s"$wiremockPort",
    s"direct-debit.isTest" -> "false",
    "play.filters.csrf.header.bypassHeaders.X-Requested-With" -> "*",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "json.encryption.key" -> "fqpLDZ4sumDsekHkeEBlCA==",
    "json.encryption.previousKeys" -> "[]",
    "play.http.router" -> "testOnlyDoNotUseInAppConf.Routes",
    "microservice.services.home-page-url" -> "http://www.example.com/home",
    "creditForExportGuidance" -> "https://www.gov.uk/guidance/soft-drinks-industry-levy-credit-for-exported-lost-or-destroyed-drinks-notice-4"
  )

  override implicit lazy val app: Application = appBuilder().build()

  def configParams: Map[String, Any] = Map()

  protected def appBuilder(): GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .in(Environment.simple(mode = Mode.Dev))
      .configure(config ++ configParams)
      .overrides(
        bind[AuthenticatedAction].to[AuthenticatedAuthenticatedAction],
        bind[RegisteredAction].to[RegisteredActionImp],
        bind[Clock].toInstance(Clock.systemDefaultZone().withZone(ZoneOffset.UTC))
      )
  }

  app.injector.instanceOf[HealthController]

  val wireMockServer = new WireMockServer(wireMockConfig().port(wiremockPort))

  override def beforeAll() = {
    wireMockServer.stop()
    wireMockServer.start()
    configureFor(wiremockHost, wiremockPort)
  }

  override def beforeEach() = {
    await(sessionRepository.collection.drop().toFuture())
    resetAllScenarios()
    reset()
  }

  override protected def afterAll(): Unit = {
    wireMockServer.stop()
  }

  override def afterEach(): Unit = {
    wireMockServer.getAllServeEvents.asScala.toList
      .sortBy(_.getRequest.getLoggedDate)
      .map(_.getRequest).foreach(r => s"${r.getLoggedDate.toInstant.toEpochMilli}\t${r.getMethod}\t${r.getUrl}")
  }

  implicit lazy val messagesAPI = app.injector.instanceOf[MessagesApi]
  implicit lazy val messagesProvider = MessagesImpl(Lang("en"), messagesAPI)
  val genericLogger = app.injector.instanceOf[GenericLogger]
  lazy val mcc = app.injector.instanceOf[MessagesControllerComponents]
  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
}
