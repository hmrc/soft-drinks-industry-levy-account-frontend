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

import cats.data.EitherT
import config.FrontendAppConfig
import errors.UnexpectedResponseFromSDIL
import models.FinancialLineItem.formatter
import models._
import repositories.{ SessionCache, SessionKeys }
import service.AccountResult
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{ HeaderCarrier, HttpReads, HttpResponse, StringContextOps }
import utilities.GenericLogger

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

class SoftDrinksIndustryLevyConnector @Inject() (
  val http: HttpClientV2,
  frontendAppConfig: FrontendAppConfig,
  sdilSessionCache: SessionCache,
  genericLogger: GenericLogger
)(implicit ec: ExecutionContext) {

  lazy val sdilUrl: String = frontendAppConfig.sdilBaseUrl

  private val logger = genericLogger.logger

  private class RawHttpReads extends HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse): HttpResponse = response
  }

  private val rawHttpReads = new RawHttpReads

  private def outboundHeaderCarrier(hc: HeaderCarrier): HeaderCarrier =
    HeaderCarrier(
      requestId = hc.requestId,
      sessionId = hc.sessionId
    )

  private def sdilContext(
    path: String,
    status: Option[Int] = None,
    startTime: Option[Long] = None
  ): String =
    Seq(
      Some(s"path=$path"),
      status.map(st => s"status=$st"),
      startTime.map(st => s"durationMs=${System.currentTimeMillis() - st}")
    ).flatten.mkString(" ")

  private def executeGet[A](operation: String, path: String)(implicit hc: HeaderCarrier, rds: HttpReads[A]): Future[A] = {
    val urlString = s"$sdilUrl$path"
    val startTime = System.currentTimeMillis()
    logger.info(
      s"SDIL $operation request ${sdilContext(path, startTime = Some(startTime))}"
    )
    http
      .get(url"$urlString")(using outboundHeaderCarrier(hc))
      .execute[HttpResponse](using rawHttpReads, ec)
      .map { response =>
        logger.info(
          s"SDIL $operation response ${sdilContext(path, status = Some(response.status), startTime = Some(startTime))}"
        )
        rds.read("GET", urlString, response)
      }
      .recoverWith { case NonFatal(e) =>
        logger.error(
          s"SDIL $operation failure ${sdilContext(path, startTime = Some(startTime))} error=${e.getMessage}",
          e
        )
        Future.failed(e)
      }
  }

  def retrieveSubscription(identifierValue: String, identifierType: String, internalId: String)(implicit
    hc: HeaderCarrier
  ): AccountResult[Option[RetrievedSubscription]] = EitherT {
    sdilSessionCache.fetchEntry[OptRetrievedSubscription](internalId, SessionKeys.SUBSCRIPTION).flatMap {
      case Some(optSubscription) => Future.successful(Right(optSubscription.optRetrievedSubscription))
      case None =>
        executeGet[Option[RetrievedSubscription]](
          operation = "retrieveSubscription",
          path = s"/subscription/$identifierType/$identifierValue"
        )
          .flatMap { optRetrievedSubscription =>
            sdilSessionCache
              .save(internalId, SessionKeys.SUBSCRIPTION, OptRetrievedSubscription(optRetrievedSubscription))
              .map { _ =>
                Right(optRetrievedSubscription)
              }

          }
          .recover { case NonFatal(_) =>
            Left(UnexpectedResponseFromSDIL)
          }
    }
  }

  def returns_pending(internalId: String, utr: String)(implicit hc: HeaderCarrier): AccountResult[List[ReturnPeriod]] =
    EitherT {
      sdilSessionCache.fetchEntry[List[ReturnPeriod]](internalId, SessionKeys.pendingReturn(utr)).flatMap {
        case Some(pendingReturns) => Future.successful(Right(pendingReturns))
        case None =>
          executeGet[List[ReturnPeriod]](
            operation = "returns_pending",
            path = s"/returns/$utr/pending"
          )
            .flatMap { pendingReturns =>
              sdilSessionCache
                .save[List[ReturnPeriod]](internalId, SessionKeys.pendingReturn(utr), pendingReturns)
                .map(_ => Right(pendingReturns))
            }
            .recover { case NonFatal(_) =>
              Left(UnexpectedResponseFromSDIL)
            }
      }
    }

  def returns_variable(internalId: String, utr: String)(implicit hc: HeaderCarrier): AccountResult[List[ReturnPeriod]] =
    EitherT {
      sdilSessionCache.fetchEntry[List[ReturnPeriod]](internalId, SessionKeys.variableReturn(utr)).flatMap {
        case Some(pendingReturns) => Future.successful(Right(pendingReturns))
        case None =>
          executeGet[List[ReturnPeriod]](
            operation = "returns_variable",
            path = s"/returns/$utr/variable"
          )
            .flatMap { pendingReturns =>
              sdilSessionCache
                .save[List[ReturnPeriod]](internalId, SessionKeys.variableReturn(utr), pendingReturns)
                .map(_ => Right(pendingReturns))
            }
            .recover { case NonFatal(_) =>
              Left(UnexpectedResponseFromSDIL)
            }
      }
    }

  def returns_get(utr: String, period: ReturnPeriod, internalId: String)(implicit
    hc: HeaderCarrier
  ): AccountResult[Option[SdilReturn]] = EitherT {
    sdilSessionCache
      .fetchEntry[OptPreviousSubmittedReturn](internalId, SessionKeys.previousSubmittedReturn(utr, period))
      .flatMap {
        case Some(optPreviousReturn) => Future.successful(Right(optPreviousReturn.optReturn))
        case None =>
          executeGet[Option[SdilReturn]](
            operation = "returns_get",
            path = s"/returns/$utr/year/${period.year}/quarter/${period.quarter}"
          )
            .flatMap { optReturn =>
              sdilSessionCache
                .save[OptPreviousSubmittedReturn](
                  internalId,
                  SessionKeys.previousSubmittedReturn(utr, period),
                  OptPreviousSubmittedReturn(optReturn)
                )
                .map(_ => Right(optReturn))
            }
            .recover { case NonFatal(_) =>
              Left(UnexpectedResponseFromSDIL)
            }
      }
  }

  def balance(
    sdilRef: String,
    withAssessment: Boolean,
    internalId: String
  )(implicit hc: HeaderCarrier): AccountResult[BigDecimal] = EitherT {
    sdilSessionCache.fetchEntry[BigDecimal](internalId, SessionKeys.balance(withAssessment)).flatMap {
      case Some(b) =>
        Future.successful(Right(b))
      case None =>
        executeGet[BigDecimal](
          operation = "balance",
          path = s"/balance/$sdilRef/$withAssessment"
        )
          .flatMap { b =>
            sdilSessionCache
              .save[BigDecimal](internalId, SessionKeys.balance(withAssessment), b)
              .map(_ => Right(b))

          }
          .recover { case NonFatal(_) =>
            Left(UnexpectedResponseFromSDIL)
          }
    }
  }

  def balanceHistory(
    sdilRef: String,
    withAssessment: Boolean,
    internalId: String
  )(implicit hc: HeaderCarrier): AccountResult[List[FinancialLineItem]] = EitherT {
    sdilSessionCache
      .fetchEntry[List[FinancialLineItem]](internalId, SessionKeys.balanceHistory(withAssessment))
      .flatMap {
        case Some(b) => Future.successful(Right(b))
        case None =>
          executeGet[List[FinancialLineItem]](
            operation = "balanceHistory",
            path = s"/balance/$sdilRef/history/all/$withAssessment"
          )
            .flatMap { bh =>
              sdilSessionCache
                .save[List[FinancialLineItem]](internalId, SessionKeys.balanceHistory(withAssessment), bh)
                .map(_ => Right(bh))

            }
            .recover { case NonFatal(_) =>
              Left(UnexpectedResponseFromSDIL)
            }
      }
  }

  def checkDirectDebitStatus(sdilRef: String)(implicit hc: HeaderCarrier): AccountResult[Boolean] = EitherT {
    executeGet[DisplayDirectDebitResponse](
      operation = "checkDirectDebitStatus",
      path = s"/check-direct-debit-status/$sdilRef"
    )
      .map { result =>
        Right(result.directDebitMandateFound)
      }
      .recover { case NonFatal(_) =>
        Left(UnexpectedResponseFromSDIL)
      }
  }
}
