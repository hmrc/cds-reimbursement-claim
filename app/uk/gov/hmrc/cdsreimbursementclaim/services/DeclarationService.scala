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

package uk.gov.hmrc.cdsreimbursementclaim.services

import cats.data.EitherT
import cats.instances.future._
import cats.instances.int._
import cats.syntax.either._
import cats.syntax.eq._
import com.google.inject.ImplementedBy
import play.api.http.Status
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.connectors.DeclarationConnector
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.GetDeclarationError
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.ISO8601DateTime
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.request.{DeclarationRequest, OverpaymentDeclarationDisplayRequest, RequestCommon, RequestDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.DeclarationResponse
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.DeclarationErrorResponse
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.{CorrelationId, MRN}
import uk.gov.hmrc.cdsreimbursementclaim.utils.HttpResponseOps._
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultDeclarationService])
trait DeclarationService {
  def getDeclaration(mrn: MRN, securityReason: Option[String] = None)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Error, Option[DisplayDeclaration]]

  def getDeclarationWithErrorCodes(mrn: MRN, securityReason: Option[String] = None)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, GetDeclarationError, DisplayDeclaration]
}

@Singleton
class DefaultDeclarationService @Inject() (
  declarationConnector: DeclarationConnector,
  declarationTransformerService: DeclarationTransformerService
)(implicit ec: ExecutionContext)
    extends DeclarationService
    with Logging {

  def getDeclaration(mrn: MRN, securityReason: Option[String])(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Error, Option[DisplayDeclaration]] = {
    val declarationRequest = DeclarationRequest(
      OverpaymentDeclarationDisplayRequest(
        RequestCommon(
          Platform.MDTP,
          ISO8601DateTime.now,
          CorrelationId.compact
        ),
        RequestDetail(
          mrn.value,
          securityReason
        )
      )
    )

    declarationConnector
      .getDeclaration(declarationRequest)
      .subflatMap { response =>
        if (response.status === Status.OK) {
          for {
            declarationResponse     <- response.parseJSON[DeclarationResponse]().leftMap(Error(_))
            maybeDisplayDeclaration <- declarationTransformerService.toDeclaration(declarationResponse)
          } yield maybeDisplayDeclaration
        } else {
          logger.warn(s"could not get declaration: http status: ${response.status.toString}")
          Left(Error("call to get declaration failed"))
        }
      }
  }

  def getDeclarationWithErrorCodes(mrn: MRN, securityReason: Option[String])(implicit
    hc: HeaderCarrier
  ): EitherT[Future, GetDeclarationError, DisplayDeclaration] = {
    val declarationRequest = DeclarationRequest(
      OverpaymentDeclarationDisplayRequest(
        RequestCommon(
          Platform.MDTP,
          ISO8601DateTime.now,
          CorrelationId.compact
        ),
        RequestDetail(
          mrn.value,
          securityReason
        )
      )
    )

    declarationConnector
      .getDeclaration(declarationRequest)
      .leftMap(_ => GetDeclarationError.unexpectedError)
      .subflatMap { response =>
        if (response.status === Status.OK) {
          val maybeDisplayDeclaration = for {
            declarationResponse     <-
              response.parseJSON[DeclarationResponse]().leftMap(_ => GetDeclarationError.unexpectedError)
            maybeDisplayDeclaration <- declarationTransformerService.toDeclaration(declarationResponse)
          } yield maybeDisplayDeclaration

          maybeDisplayDeclaration match {
            case Right(None)                     => Left(GetDeclarationError.unexpectedError)
            case Right(Some(displayDeclaration)) => Right(displayDeclaration)
            case Left(_)                         => Left(GetDeclarationError.unexpectedError)
          }
        } else if (response.status === Status.BAD_REQUEST) {
          response
            .parseJSON[DeclarationErrorResponse]() match {
            case Left(_)              => GetDeclarationError.unexpectedError.asLeft[DisplayDeclaration]
            case Right(errorResponse) =>
              {
                errorResponse.errorDetail.sourceFaultDetail.detail.toList match {
                  case first :: Nil if first.startsWith("072") => GetDeclarationError.invalidReasonForSecurity
                  case first :: Nil if first.startsWith("086") => GetDeclarationError.declarationNotFound
                  case _                                       => GetDeclarationError.unexpectedError
                }
              }.asLeft[DisplayDeclaration]
          }
        } else {
          logger.warn(s"could not get declaration: http status: ${response.status.toString}")
          Left(GetDeclarationError.unexpectedError)
        }
      }
  }
}
