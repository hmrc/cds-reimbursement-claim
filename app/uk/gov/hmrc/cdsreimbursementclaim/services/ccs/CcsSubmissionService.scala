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
import configs.ConfigReader
import configs.syntax._
import play.api.Configuration
import reactivemongo.bson.BSONObjectID
import ru.tinkoff.phobos.encoding.XmlEncoder
import uk.gov.hmrc.cdsreimbursementclaim.connectors.CcsConnector
import uk.gov.hmrc.cdsreimbursementclaim.models.ccs._
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{SubmitClaimRequest, SubmitClaimResponse, SupportingEvidence}
import uk.gov.hmrc.cdsreimbursementclaim.models.{EntryNumber, Error, MRN, UserDetails}
import uk.gov.hmrc.cdsreimbursementclaim.repositories.ccs.CcsSubmissionRepo
import uk.gov.hmrc.cdsreimbursementclaim.utils.{Logging, toUUIDString}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.workitem.{ProcessingStatus, ResultStatus, WorkItem}

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.Future

@ImplementedBy(classOf[DefaultCcsSubmissionService])
trait CcsSubmissionService {
  def enqueue(
    claimRequest: SubmitClaimRequest,
    submitClaimResponse: SubmitClaimResponse
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
  ccsSubmissionRepo: CcsSubmissionRepo,
  configuration: Configuration
)(implicit ec: CcsSubmissionPollerExecutionContext)
    extends CcsSubmissionService
    with Logging {

  def getCcsMetaConfig[A : ConfigReader](key: String): A =
    configuration.underlying
      .get[A](s"ccs.$key")
      .value

  override def submitToCcs(
    ccsSubmissionPayload: CcsSubmissionPayload
  )(implicit hc: HeaderCarrier): EitherT[Future, Error, HttpResponse] =
    ccsConnector.submitToCcs(CcsSubmissionPayload(ccsSubmissionPayload.dec64Body, hc.headers))

  @SuppressWarnings(Array("org.wartremover.warts.Any")) // compiler can't infer the type properly on sequence
  override def enqueue(
    submitClaimRequest: SubmitClaimRequest,
    submitClaimResponse: SubmitClaimResponse
  )(implicit hc: HeaderCarrier): EitherT[Future, Error, List[WorkItem[CcsSubmissionRequest]]] = {
    //TODO: refactor
    val r = submitClaimRequest.completeClaim.evidences
    val a = submitClaimRequest.userDetails
    val k = submitClaimRequest.completeClaim.movementReferenceNumber
    val i = submitClaimRequest.completeClaim.correlationId

    val foo: List[EitherT[Future, Error, WorkItem[CcsSubmissionRequest]]] =
      makeDec64Payload(i, k, submitClaimResponse.caseNumber, a, r)
        .map(p =>
          ccsSubmissionRepo.set(CcsSubmissionRequest(XmlEncoder[BatchFileInterfaceMetadata].encode(p), hc.headers))
        )
    foo.sequence
  }

  override def dequeue: EitherT[Future, Error, Option[WorkItem[CcsSubmissionRequest]]] =
    ccsSubmissionRepo.get

  override def setProcessingStatus(id: BSONObjectID, status: ProcessingStatus): EitherT[Future, Error, Boolean] =
    ccsSubmissionRepo.setProcessingStatus(id, status)

  override def setResultStatus(id: BSONObjectID, status: ResultStatus): EitherT[Future, Error, Boolean] =
    ccsSubmissionRepo.setResultStatus(id, status)

  // need to validate the data before enqueing
  private def makeDec64Payload(
    correlationId: UUID,
    movementReferenceNumber: Either[EntryNumber, MRN],
    caseReference: String,
    userDetails: UserDetails,
    supportingEvidences: List[SupportingEvidence]
  ): List[BatchFileInterfaceMetadata] = {

    def innerLogin(
      referenceNumber: String,
      documentType: String,
      uploadTime: LocalDateTime,
      batchCount: Long,
      batchSize: Long,
      checkSum: String,
      url: String,
      fileName: String,
      mimeType: String,
      fileSize: Long
    ): BatchFileInterfaceMetadata = {
      val properties =
        PropertiesType.generateMandatoryList(
          caseReference,
          userDetails.eori.value,
          referenceNumber,
          documentType,
          uploadTime
        )

      BatchFileInterfaceMetadata(
        correlationID = correlationId,
        batchID = correlationId,
        batchCount = batchCount,
        batchSize = batchSize,
        checksum = checkSum,
        sourceLocation = url,
        sourceFileName = fileName,
        sourceFileMimeType = mimeType,
        fileSize = fileSize,
        properties = properties,
        destinations = Destinations(
          List(
            Destination(
              "CDS"
            )
          )
        )
      )

    }
    movementReferenceNumber match {
      case Left(entryNumber) =>
        val batchSize: Int = supportingEvidences.size
        supportingEvidences.zipWithIndex.map { case (evidence, index) =>
          innerLogin(
            entryNumber.value,
            evidence.documentType.toString, //FIXME
            evidence.uploadedOn,
            index.toLong,
            batchSize.toLong,
            evidence.upscanSuccess.uploadDetails.checksum,
            evidence.upscanSuccess.downloadUrl,
            evidence.upscanSuccess.uploadDetails.fileName,
            evidence.upscanSuccess.uploadDetails.fileMimeType,
            evidence.upscanSuccess.uploadDetails.size
          )
        }
      case Right(mrn)        =>
        val batchSize = supportingEvidences.size
        supportingEvidences.zipWithIndex.map { case (evidence, index) =>
          innerLogin(
            mrn.value,
            evidence.documentType.toString, //FIXME
            evidence.uploadedOn,
            index.toLong,
            batchSize.toLong,
            evidence.upscanSuccess.uploadDetails.checksum,
            evidence.upscanSuccess.downloadUrl,
            evidence.upscanSuccess.uploadDetails.fileName,
            evidence.upscanSuccess.uploadDetails.fileMimeType,
            evidence.upscanSuccess.uploadDetails.size
          )
        }
    }
  }
}
