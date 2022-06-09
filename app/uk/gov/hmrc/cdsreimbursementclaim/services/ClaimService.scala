/*
 * Copyright 2022 HM Revenue & Customs
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
import cats.syntax.option._
import com.google.inject.ImplementedBy
import play.api.http.Status.OK
import play.api.libs.json.{Format, Json}
import play.api.mvc.Request
import uk.gov.hmrc.cdsreimbursementclaim.connectors.ClaimConnector
import uk.gov.hmrc.cdsreimbursementclaim.metrics.Metrics
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.audit.{SubmitClaimEvent, SubmitClaimResponseEvent}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{C285ClaimRequest, ClaimSubmitResponse, MultipleRejectedGoodsClaim, RejectedGoodsClaim, RejectedGoodsClaimRequest, ScheduledRejectedGoodsClaim, SecuritiesClaimRequest}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.{EisSubmitClaimRequest, EisSubmitClaimResponse}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.email.EmailRequest
import uk.gov.hmrc.cdsreimbursementclaim.services.audit.AuditService
import uk.gov.hmrc.cdsreimbursementclaim.services.tpi05.ClaimToTPI05Mapper
import uk.gov.hmrc.cdsreimbursementclaim.utils.HttpResponseOps.HttpResponseOps
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@ImplementedBy(classOf[DefaultClaimService])
trait ClaimService {

  def submitC285Claim(
    c285ClaimRequest: C285ClaimRequest
  )(implicit
    hc: HeaderCarrier,
    request: Request[_],
    tpi05Binder: ClaimToTPI05Mapper[C285ClaimRequest]
  ): EitherT[Future, Error, ClaimSubmitResponse]

  def submitRejectedGoodsClaim[Claim <: RejectedGoodsClaim](
    rejectedGoodsClaimRequest: RejectedGoodsClaimRequest[Claim]
  )(implicit
    hc: HeaderCarrier,
    request: Request[_],
    tpi05Binder: ClaimToTPI05Mapper[(Claim, List[DisplayDeclaration])],
    claimRequestFormat: Format[RejectedGoodsClaimRequest[Claim]]
  ): EitherT[Future, Error, ClaimSubmitResponse]

  def submitMultipleRejectedGoodsClaim(claimRequest: RejectedGoodsClaimRequest[MultipleRejectedGoodsClaim])(implicit
    hc: HeaderCarrier,
    request: Request[_],
    tpi05Binder: ClaimToTPI05Mapper[(MultipleRejectedGoodsClaim, List[DisplayDeclaration])],
    claimRequestFormat: Format[RejectedGoodsClaimRequest[MultipleRejectedGoodsClaim]]
  ): EitherT[Future, Error, ClaimSubmitResponse]

  def submitScheduledRejectedGoodsClaim(
    rejectedGoodsClaimRequest: RejectedGoodsClaimRequest[ScheduledRejectedGoodsClaim]
  )(implicit
    hc: HeaderCarrier,
    request: Request[_],
    claimRequestFormat: Format[RejectedGoodsClaimRequest[ScheduledRejectedGoodsClaim]]
  ): EitherT[Future, Error, ClaimSubmitResponse]

  def submitSecuritiesClaim(
    securitiesClaimRequest: SecuritiesClaimRequest
  )(implicit
    hc: HeaderCarrier,
    request: Request[_],
    claimRequestFormat: Format[SecuritiesClaimRequest]
  ): EitherT[Future, Error, ClaimSubmitResponse]
}

@Singleton
class DefaultClaimService @Inject() (
  claimConnector: ClaimConnector,
  declarationService: DeclarationService,
  emailService: EmailService,
  auditService: AuditService,
  metrics: Metrics
)(implicit ec: ExecutionContext)
    extends ClaimService
    with Logging {

  def submitC285Claim(c285ClaimRequest: C285ClaimRequest)(implicit
    hc: HeaderCarrier,
    request: Request[_],
    tpi05Binder: ClaimToTPI05Mapper[C285ClaimRequest]
  ): EitherT[Future, Error, ClaimSubmitResponse] =
    proceed(c285ClaimRequest, c285ClaimRequest)

  def submitRejectedGoodsClaim[Claim <: RejectedGoodsClaim](claimRequest: RejectedGoodsClaimRequest[Claim])(implicit
    hc: HeaderCarrier,
    request: Request[_],
    tpi05Binder: ClaimToTPI05Mapper[(Claim, List[DisplayDeclaration])],
    claimRequestFormat: Format[RejectedGoodsClaimRequest[Claim]]
  ): EitherT[Future, Error, ClaimSubmitResponse] =
    declarationService
      .getDeclaration(claimRequest.claim.leadMrn)
      .subflatMap(_.toRight(Error(s"Could not retrieve display declaration")))
      .flatMap(declaration => proceed((claimRequest.claim, List(declaration)), claimRequest))

  def combineDeclarations(
    mrn: MRN,
    maybeDeclaration: EitherT[Future, Error, Option[DisplayDeclaration]],
    declarations: EitherT[Future, Error, List[DisplayDeclaration]]
  ): EitherT[Future, Error, List[DisplayDeclaration]] =
    for {
      decs <- declarations
      dec  <- maybeDeclaration.subflatMap(aaa => aaa.toRight(Error(s"Could not retrieve display declaration $mrn")))
    } yield dec :: decs

  def obtainAcc14Declarations(mrns: List[MRN])(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Error, List[DisplayDeclaration]] =
    mrns.foldLeft(EitherT.rightT[Future, Error](List[DisplayDeclaration]())) { case (declarations, mrn) =>
      val maybeDeclaration = declarationService.getDeclaration(mrn)
      combineDeclarations(mrn, maybeDeclaration, declarations)
    }

  def submitMultipleRejectedGoodsClaim(
    claimRequest: RejectedGoodsClaimRequest[MultipleRejectedGoodsClaim]
  )(implicit
    hc: HeaderCarrier,
    request: Request[_],
    tpi05Binder: ClaimToTPI05Mapper[(MultipleRejectedGoodsClaim, List[DisplayDeclaration])],
    claimRequestFormat: Format[RejectedGoodsClaimRequest[MultipleRejectedGoodsClaim]]
  ): EitherT[Future, Error, ClaimSubmitResponse] =
    obtainAcc14Declarations(claimRequest.claim.movementReferenceNumbers)
      .flatMap(declarations => proceed((claimRequest.claim, declarations), claimRequest))

  def submitScheduledRejectedGoodsClaim(
    claimRequest: RejectedGoodsClaimRequest[ScheduledRejectedGoodsClaim]
  )(implicit
    hc: HeaderCarrier,
    request: Request[_],
    claimRequestFormat: Format[RejectedGoodsClaimRequest[ScheduledRejectedGoodsClaim]]
  ): EitherT[Future, Error, ClaimSubmitResponse] =
    declarationService
      .getDeclaration(claimRequest.claim.leadMrn)
      .subflatMap(_.toRight(Error(s"Could not retrieve display declaration")))
      .flatMap(declaration => proceed((claimRequest.claim, declaration), claimRequest))

  def submitSecuritiesClaim(
    securitiesClaimRequest: SecuritiesClaimRequest
  )(implicit
    hc: HeaderCarrier,
    request: Request[_],
    claimRequestFormat: Format[SecuritiesClaimRequest]
  ): EitherT[Future, Error, ClaimSubmitResponse] =
    EitherT.left(Future.successful(Error(s"Not implemented"))) //TODO: 1718

  private def proceed[R, A](claimRequest: R, auditable: A)(implicit
    hc: HeaderCarrier,
    request: Request[_],
    tpi05Binder: ClaimToTPI05Mapper[R],
    auditableFormat: Format[A]
  ): EitherT[Future, Error, ClaimSubmitResponse] = for {
    eisSubmitRequest       <- EitherT
                                .fromEither[Future](EisSubmitClaimRequest(claimRequest))
                                .leftMap(e => Error(s"could not make TPIO5 payload. Cause: $e"))
    _                      <- auditClaimBeforeSubmit(eisSubmitRequest)
    returnHttpResponse     <- submitClaimAndAudit(auditable, eisSubmitRequest)
    eisSubmitClaimResponse <- EitherT.fromEither[Future](
                                returnHttpResponse.parseJSON[EisSubmitClaimResponse]().leftMap(Error(_))
                              )
    claimResponse          <- prepareSubmitClaimResponse(eisSubmitClaimResponse)
    _                      <- createEmailRequest(eisSubmitRequest)
                                .flatMap(emailService.sendClaimConfirmationEmail(_, claimResponse))
                                .leftFlatMap { e =>
                                  logger.warn("could not send claim submission confirmation email or audit event", e)
                                  EitherT.pure[Future, Error](())
                                }
  } yield claimResponse

  private def createEmailRequest(eisSubmitClaimRequest: EisSubmitClaimRequest): EitherT[Future, Error, EmailRequest] =
    EitherT.fromOption[Future](
      EmailRequest(eisSubmitClaimRequest.postNewClaimsRequest.requestDetail),
      Error("Cannot create Email request because required fields missing")
    )

  private def auditClaimBeforeSubmit(eisSubmitClaimRequest: EisSubmitClaimRequest)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): EitherT[Future, Error, Unit] = EitherT.fromEither[Future](
    eisSubmitClaimRequest.postNewClaimsRequest.requestDetail.claimantEORI.some
      .toRight(Error("Claimant EORI is missing"))
      .map { eori =>
        auditService.sendEvent(
          auditType = "SubmitClaim",
          detail = SubmitClaimEvent(eisSubmitClaimRequest, eori),
          transactionName = "submit-claim"
        )
      }
  )

  private def submitClaimAndAudit[A](
    submitClaimRequest: A,
    eisSubmitClaimRequest: EisSubmitClaimRequest
  )(implicit hc: HeaderCarrier, request: Request[_], requestFormat: Format[A]): EitherT[Future, Error, HttpResponse] = {
    val timer = metrics.submitClaimTimer.time()
    claimConnector
      .submitClaim(
        eisSubmitClaimRequest
      )
      .subflatMap { httpResponse =>
        timer.close()
        auditSubmitClaimResponse(
          httpResponse.status,
          httpResponse.body,
          submitClaimRequest,
          eisSubmitClaimRequest
        )

        Either.cond(
          httpResponse.status === OK,
          httpResponse, {
            metrics.submitClaimErrorCounter.inc()
            Error(s"call to submit claim came back with status ${httpResponse.status}")
          }
        )
      }
  }

  private def auditSubmitClaimResponse[A](
    responseHttpStatus: Int,
    responseBody: String,
    submitClaimRequest: A,
    eisSubmitClaimRequest: EisSubmitClaimRequest
  )(implicit hc: HeaderCarrier, request: Request[_], f: Format[A]): Unit = {
    val responseJson =
      Try(Json.parse(responseBody))
        .getOrElse(Json.parse(s"""{ "body" : "could not parse body as JSON: $responseBody" }"""))
    val requestJson  = Json.toJson(eisSubmitClaimRequest)
    auditService.sendEvent(
      "SubmitClaimResponse",
      SubmitClaimResponseEvent(
        responseHttpStatus,
        responseJson,
        requestJson,
        submitClaimRequest
      ),
      "submit-claim-response"
    )
  }

  private def prepareSubmitClaimResponse(
    response: EisSubmitClaimResponse
  ): EitherT[Future, Error, ClaimSubmitResponse] =
    EitherT.fromEither[Future] {
      response.postNewClaimsResponse.responseCommon.errorMessage match {
        case Some(error) =>
          Left(
            Error(
              s"""submission of claim failed : $error | ${response.postNewClaimsResponse.responseCommon.returnParameters
                .map(e => e.mkString("; "))}"""
            )
          )
        case None        =>
          response.postNewClaimsResponse.responseCommon.CDFPayCaseNumber match {
            case Some(caseNumber) => Right(ClaimSubmitResponse(caseNumber))
            case None             => Left(Error("No case number returned in response"))
          }
      }
    }
}
