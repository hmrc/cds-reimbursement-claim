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
import uk.gov.hmrc.cdsreimbursementclaim.models.{BatchFileInterfaceMetadata, Dec64Body, Error, FrontendSubmitClaim, PropertiesType, SubmitClaimRequest, SubmitClaimResponse}
import uk.gov.hmrc.cdsreimbursementclaim.services.{FileUploadQueue, SubmitClaimService}
import uk.gov.hmrc.cdsreimbursementclaim.services.upscan.UpscanService
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging.LoggerOps
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

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
      _             <- EitherT.rightT[Future, Error](process(frontendClaim, claimResponse))
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

  def process(claimRequest: FrontendSubmitClaim, claimResponse: SubmitClaimResponse): Unit = {
    //val uploadReferences = claimRequest.files.map(_.uploadReference)
    //upscanService.readUpscanUploads(uploadReferences).map(_ => ())
    println(claimRequest)
    println(claimResponse)
    println(fileUploadQueue)
    println(upscanService)
  }
//    val uploadReferences = claimRequest.files.map(_.uploadReference)
//    for {
//      uploads <- upscanService.readUpscanUploads(uploadReferences)
//      messages <- EitherT.rightT[Future,Error]((0 until uploads.size).map(i =>
//                  createDec64Request(
//                    uploads(i),
//                    claimRequest,
//                    claimResponse,
//                    claimRequest.files(i).documentType,
//                    i + 1,
//                    uploads.size
//                  )
//                ))
//      _ <- messages.map(a => a.map(b => b) )
//
//        _ <- uploads.foreach(u => (u))
//
//    } yield ()
//    println(claimResponse)
//  }
  //  def process(
//    claimRequest: FrontendSubmitClaim,
//    claimResponse: SubmitClaimResponse
//  ) = {
//    val uploadReferences = claimRequest.files.map(_.uploadReference)
//    upscanService.readUpscanUploads(uploadReferences)
//    println(claimResponse)
//      .map { uploads =>
//        println(uploads + "" + claimResponse)
//        (0 until uploads.size).foreach(i =>
//          createDec64Request(
//            uploads(i),
//            claimRequest,
//            claimResponse,
//            claimRequest.files(i).documentType,
//            i + 1,
//            uploads.size
//          )
//        )
//        ()
//      }
//  }

//  def uploadFile(file:Dec64Body)(implicit hc:HeaderCarrier):Unit = {
//    fileUploadService.upload(file).value   match {  //TODO If Future.failed or Either.Left send to WorkItemRepo
//      case Success(e) => 1
//      case Failure(ex) => 1
//    }
//  }

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
      checksum      <- Either.fromOption(upscanSuccess.uploadDetails.get("checksum"), Error("checksum was not found"))
      fileName      <- Either.fromOption(upscanSuccess.uploadDetails.get("fileName"), Error("fileName was not found"))
      fileMimeType  <-
        Either.fromOption(upscanSuccess.uploadDetails.get("fileMimeType"), Error("fileMimeType was not found"))
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
