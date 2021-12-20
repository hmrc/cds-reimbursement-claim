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

package uk.gov.hmrc.cdsreimbursementclaim.services.ccs

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{ImplementedBy, Inject, Singleton}
import reactivemongo.bson.BSONObjectID
import ru.tinkoff.phobos.encoding.XmlEncoder
import uk.gov.hmrc.cdsreimbursementclaim.connectors.CcsConnector
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.ccs._
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{C285ClaimRequest, ClaimSubmitResponse, UploadDocument}
import uk.gov.hmrc.cdsreimbursementclaim.repositories.ccs.CcsSubmissionRepo
import uk.gov.hmrc.cdsreimbursementclaim.services.ccs.DefaultCcsSubmissionService.makeBatchFileInterfaceMetaDataPayload
import uk.gov.hmrc.cdsreimbursementclaim.utils.{Logging, TimeUtils, toUUIDString}
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpResponse}
import uk.gov.hmrc.workitem.{ProcessingStatus, ResultStatus, WorkItem}

import java.util.UUID
import scala.concurrent.Future

@ImplementedBy(classOf[DefaultCcsSubmissionService])
trait CcsSubmissionService {
  def enqueue[A](
    claimRequest: A,
    submitClaimResponse: ClaimSubmitResponse
  )(implicit hc: HeaderCarrier): EitherT[Future, Error, List[WorkItem[CcsSubmissionRequest]]]

  def dequeue: EitherT[Future, Error, Option[WorkItem[CcsSubmissionRequest]]]

  def setProcessingStatus(id: BSONObjectID, status: ProcessingStatus): EitherT[Future, Error, Boolean]

  def setResultStatus(id: BSONObjectID, status: ResultStatus): EitherT[Future, Error, Boolean]

  def submitToCcs(ccsSubmissionPayload: CcsSubmissionPayload)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Error, HttpResponse]

}

@Singleton
class DefaultCcsSubmissionService @Inject() (
  ccsConnector: CcsConnector,
  ccsSubmissionRepo: CcsSubmissionRepo
)(implicit ec: CcsSubmissionPollerExecutionContext)
    extends CcsSubmissionService
    with Logging {

  override def submitToCcs(
    ccsSubmissionPayload: CcsSubmissionPayload
  )(implicit hc: HeaderCarrier): EitherT[Future, Error, HttpResponse] =
    ccsConnector.submitToCcs(
      CcsSubmissionPayload(ccsSubmissionPayload.dec64Body, DefaultCcsSubmissionService.getHeaders(hc))
    )

  @SuppressWarnings(Array("org.wartremover.warts.Any")) // compiler can't infer the type properly on sequence
  override def enqueue[A](
    submitClaimRequest: A,
    submitClaimResponse: ClaimSubmitResponse
  )(implicit hc: HeaderCarrier): EitherT[Future, Error, List[WorkItem[CcsSubmissionRequest]]] = {
    val queueCcsSubmissions: List[EitherT[Future, Error, WorkItem[CcsSubmissionRequest]]] =
      makeBatchFileInterfaceMetaDataPayload(submitClaimRequest, submitClaimResponse)
        .map(data =>
          ccsSubmissionRepo.set(
            CcsSubmissionRequest(XmlEncoder[Envelope].encode(data), DefaultCcsSubmissionService.getHeaders(hc))
          )
        )

    queueCcsSubmissions.sequence
  }

  override def dequeue: EitherT[Future, Error, Option[WorkItem[CcsSubmissionRequest]]] = ccsSubmissionRepo.get

  override def setProcessingStatus(id: BSONObjectID, status: ProcessingStatus): EitherT[Future, Error, Boolean] =
    ccsSubmissionRepo.setProcessingStatus(id, status)

  override def setResultStatus(id: BSONObjectID, status: ResultStatus): EitherT[Future, Error, Boolean] =
    ccsSubmissionRepo.setResultStatus(id, status)

}

object DefaultCcsSubmissionService {

  def makeBatchFileInterfaceMetaDataPayload[A](
    submitClaimRequest: A,
    submitClaimResponse: ClaimSubmitResponse
  ): List[Envelope] = {
    def make(
      referenceNumber: String,
      evidence: UploadDocument,
      batchCount: Long
    ): Envelope =
      Envelope(
        Body(
          BatchFileInterfaceMetadata(
            correlationID = UUID.randomUUID().toString,
            batchID = submitClaimRequest.claim.id,
            batchCount = batchCount,
            batchSize = submitClaimRequest.claim.documents.size.toLong,
            checksum = evidence.upscanSuccess.uploadDetails.checksum,
            sourceLocation = evidence.upscanSuccess.downloadUrl,
            sourceFileName = evidence.upscanSuccess.uploadDetails.fileName,
            sourceFileMimeType = evidence.upscanSuccess.uploadDetails.fileMimeType,
            fileSize = evidence.upscanSuccess.uploadDetails.size,
            properties = PropertiesType(
              List(
                PropertyType("CaseReference", submitClaimResponse.caseNumber),
                PropertyType("Eori", submitClaimRequest.signedInUserDetails.eori.value),
                PropertyType("DeclarationId", referenceNumber),
                PropertyType("DeclarationType", submitClaimRequest.claim.declarantTypeAnswer.toString),
                PropertyType("ApplicationName", "NDRC"),
                PropertyType(
                  "DocumentType",
                  evidence.documentType.map(_.toTPI05Key).getOrElse("")
                ),
                PropertyType("DocumentReceivedDate", TimeUtils.cdsDateTimeFormat.format(evidence.uploadedOn))
              )
            )
          )
        )
      )

    submitClaimRequest.claim.documents.zipWithIndex.map { case (document, index) =>
      make(
        submitClaimRequest.claim.movementReferenceNumber.value,
        document,
        index.toLong + 1
      )
    }.toList
  }

  def getHeaders(headerCarrier: HeaderCarrier): Seq[(String, String)] =
    List(
      headerCarrier.requestId.map(rid => headerCarrier.names.xRequestId -> rid.value),
      headerCarrier.sessionId.map(sid => headerCarrier.names.xSessionId -> sid.value),
      headerCarrier.forwarded.map(f => headerCarrier.names.xForwardedFor -> f.value),
      Some(headerCarrier.names.xRequestChain                          -> headerCarrier.requestChain.value),
      headerCarrier.authorization.map(auth => headerCarrier.names.authorisation -> auth.value),
      headerCarrier.trueClientIp.map(HeaderNames.trueClientIp         -> _),
      headerCarrier.trueClientPort.map(HeaderNames.trueClientPort     -> _),
      headerCarrier.gaToken.map(HeaderNames.googleAnalyticTokenId     -> _),
      headerCarrier.gaUserId.map(HeaderNames.googleAnalyticUserId     -> _),
      headerCarrier.deviceID.map(HeaderNames.deviceID                 -> _),
      headerCarrier.akamaiReputation.map(HeaderNames.akamaiReputation -> _.value)
    ).flattenOption ++ headerCarrier.extraHeaders ++ headerCarrier.otherHeaders

}
