/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.cdsreimbursementclaim.controllers

import cats.data.EitherT
import cats.syntax.either._
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.cdsreimbursementclaim.models.SubmitClaimRequest.{MrnDetails, PostNewClaimsRequest, RequestCommon, RequestDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.upscan.UpscanCallBack.UpscanSuccess
import uk.gov.hmrc.cdsreimbursementclaim.models.upscan.UpscanUpload
import uk.gov.hmrc.cdsreimbursementclaim.models.{BatchFileInterfaceMetadata, Dec64Body, Error, FrontendSubmitClaim, HeadlessEnvelope, PropertiesType, SubmitClaimRequest, SubmitClaimResponse, WorkItemPayload}
import uk.gov.hmrc.cdsreimbursementclaim.services.{FileUploadQueue, SubmitClaimService}
import uk.gov.hmrc.cdsreimbursementclaim.services.upscan.UpscanService
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging.LoggerOps
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.workitem.WorkItem

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton()
class SubmitClaimController @Inject() (
  eisService: SubmitClaimService,
  upscanService: UpscanService,
  fileUploadQueue: FileUploadQueue,
  cc: ControllerComponents
)(implicit
  ec: ExecutionContext
) extends BackendController(cc)
    with Logging {

  def claim(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    (for {
      frontendClaim <- EitherT.fromEither[Future](Try(request.body.as[FrontendSubmitClaim]).toEither.leftMap(Error(_)))
      claimResponse <- eisService.submitClaim(createSubmitClaim(frontendClaim))
      _             <- createAndStoreDec64Requests(frontendClaim, claimResponse)
    } yield claimResponse)
      .fold(
        e => {
          logger.warn(s"could not submit claim", e)
          InternalServerError
        },
        response => Ok(Json.toJson(response))
      )
  }

  def createSubmitClaim(frontendSubmitClaim: FrontendSubmitClaim): SubmitClaimRequest = {
    val requestCommon        = RequestCommon()
    val mrnDetails           = MrnDetails(frontendSubmitClaim.declarationId)
    val requestDetail        = RequestDetail(frontendSubmitClaim.eori).copy(MRNDetails = Some(List(mrnDetails)))
    val postNewClaimsRequest = PostNewClaimsRequest(requestCommon, requestDetail)
    SubmitClaimRequest(postNewClaimsRequest)
  }

  def createAndStoreDec64Requests(claimRequest: FrontendSubmitClaim, claimResponse: SubmitClaimResponse)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Error, List[WorkItem[WorkItemPayload]]] = {
    val uploadReferences = claimRequest.files.map(_.uploadReference)
    for {
      uploads  <- upscanService.readUpscanUploads(uploadReferences)
      messages <- EitherT.rightT[Future, Error]((0 until uploads.size).toList.map { i =>
                    createDec64Request(
                      uploads(i),
                      claimRequest,
                      claimResponse,
                      claimRequest.files(i).documentType,
                      i + 1,
                      uploads.size
                    ).map(body => fileUploadQueue.queueRequest(Dec64Body.soapEncoder.encode(HeadlessEnvelope(body))))
                  })
      result   <- {
        messages.partition(_.isLeft) match {
          case (Nil, responses) =>
            val listOfResponses = for (Right(j) <- responses) yield j
            EitherT(
              Future
                .sequence(listOfResponses)
                .map(a => Right(a))
                .recover { case err => Left(Error(err)) }
            )
          case (errors, _)      =>
            val onlyErrors = for (Left(s) <- errors) yield s
            EitherT.leftT[Future, List[WorkItem[WorkItemPayload]]](Error(onlyErrors.map(_.message).mkString("\r\n")))
        }
      }
    } yield result
  }

  def createDec64Request(
    upscanUpload: UpscanUpload,
    claimRequest: FrontendSubmitClaim,
    claimResponse: SubmitClaimResponse,
    documentType: String,
    batchCount: Int,
    batchSize: Int
  ): Either[Error, Dec64Body] =
    (for {
      eori          <- Right(claimRequest.eori)
      declarationId <- Right(claimRequest.declarationId)
      caseReference <- Either.fromOption(
                         claimResponse.postNewClaimsResponse.responseCommon.CDFPayCaseNumber,
                         Error("caseReference was not found")
                       )
      correlationID <- Either.fromOption(
                         claimResponse.postNewClaimsResponse.responseCommon.correlationID,
                         Error("correlationId was not found")
                       )
      upscanSuccess <- Either.fromOption(
                         upscanUpload.upscanCallBack.collect { case us: UpscanSuccess => us },
                         Error("Upscan upload failed")
                       )
      checksum      <- Right(upscanSuccess.uploadDetails.checksum)
      fileName      <- Right(upscanSuccess.uploadDetails.fileName)
      fileMimeType  <- Right(upscanSuccess.uploadDetails.fileMimeType)
    } yield {
      val properties =
        PropertiesType.generateMandatoryList(
          caseReference,
          eori,
          declarationId,
          documentType,
          upscanUpload.uploadedOn
        )
      Dec64Body(
        BatchFileInterfaceMetadata(
          correlationID = correlationID,
          batchID = Some(correlationID),
          batchCount = Some(batchCount),
          batchSize = Some(batchSize),
          checksum = checksum,
          sourceLocation = upscanSuccess.downloadUrl,
          sourceFileName = fileName,
          sourceFileMimeType = Some(fileMimeType),
          properties = Some(properties)
        )
      )
    })
      .leftMap { err => logger.warn(err.message); err }

}
