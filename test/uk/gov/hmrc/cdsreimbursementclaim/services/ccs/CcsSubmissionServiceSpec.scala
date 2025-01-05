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

package uk.gov.hmrc.cdsreimbursementclaim.services.ccs

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.util.Timeout
import cats.data.EitherT
import cats.syntax.all._
import org.bson.types.ObjectId
import org.scalamock.handlers.CallHandler0
import org.scalamock.handlers.CallHandler1
import org.scalamock.handlers.CallHandler2
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.test.Helpers.await

import uk.gov.hmrc.cdsreimbursementclaim.connectors.CcsConnector
import uk.gov.hmrc.cdsreimbursementclaim.models
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.ccs.CcsSubmissionPayload
import uk.gov.hmrc.cdsreimbursementclaim.models.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.TemporalAccessorOps
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.CcsSubmissionGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.RejectedGoodsClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TPI05RequestGen._
import uk.gov.hmrc.cdsreimbursementclaim.repositories.ccs.CcsSubmissionRepo
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.Failed
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.PermanentlyFailed
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem._

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

@SuppressWarnings(Array("org.wartremover.warts.TraversableOps", "org.wartremover.warts.IterableOps"))
class CcsSubmissionServiceSpec extends AnyWordSpec with Matchers with MockFactory with ScalaCheckPropertyChecks {

  val ccsSubmissionRepositoryMock: CcsSubmissionRepo = mock[CcsSubmissionRepo]
  val ccsConnectorMock: CcsConnector                 = mock[CcsConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val timeout: Timeout = Timeout(FiniteDuration(5, TimeUnit.SECONDS))

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 1)

  def getHeaders(headerCarrier: HeaderCarrier): Seq[(String, String)] =
    List(
      headerCarrier.requestId.map(rid => headerCarrier.names.xRequestId -> rid.value),
      headerCarrier.sessionId.map(sid => headerCarrier.names.xSessionId -> sid.value),
      headerCarrier.forwarded.map(f => headerCarrier.names.xForwardedFor -> f.value),
      Some(headerCarrier.names.xRequestChain -> headerCarrier.requestChain.value),
      headerCarrier.authorization.map(auth => headerCarrier.names.authorisation -> auth.value),
      headerCarrier.trueClientIp.map(HeaderNames.trueClientIp -> _),
      headerCarrier.trueClientPort.map(HeaderNames.trueClientPort -> _),
      headerCarrier.gaToken.map(HeaderNames.googleAnalyticTokenId -> _),
      headerCarrier.gaUserId.map(HeaderNames.googleAnalyticUserId -> _),
      headerCarrier.deviceID.map(HeaderNames.deviceID -> _),
      headerCarrier.akamaiReputation.map(HeaderNames.akamaiReputation -> _.value)
    ).flattenOption ++ headerCarrier.extraHeaders ++ headerCarrier.otherHeaders

  def makeDec64XmlPayload(
    correlationId: String,
    batchId: String,
    batchSize: Long,
    batchCount: Long,
    checksum: String,
    fileSize: Long,
    caseReference: String,
    eori: String,
    declarationId: String,
    declarationType: String,
    applicationName: String,
    documentType: String,
    documentReceivedDate: String,
    sourceLocation: String,
    sourceFileName: String,
    sourceFileMimeType: String,
    destinationSystem: String
  ): String =
    s"""
       |<?xml version='1.0' encoding='UTF-8'?>
       |<ans1:Envelope xmlns:ans1="http://schemas.xmlsoap.org/soap/envelope/">
       |    <ans1:Body>
       |        <ans2:BatchFileInterfaceMetadata xmlns:ans2="http://www.hmrc.gsi.gov.uk/mdg/batchFileInterfaceMetadataSchema">
       |            <ans2:sourceSystem>TPI</ans2:sourceSystem>
       |            <ans2:sourceSystemType>AWS</ans2:sourceSystemType>
       |            <ans2:interfaceName>DEC64</ans2:interfaceName>
       |            <ans2:interfaceVersion>1.0.0</ans2:interfaceVersion>
       |            <ans2:correlationID>$correlationId</ans2:correlationID>
       |            <ans2:batchID>$batchId</ans2:batchID>
       |            <ans2:batchSize>$batchSize</ans2:batchSize>
       |            <ans2:batchCount>$batchCount</ans2:batchCount>
       |            <ans2:checksum>$checksum</ans2:checksum>
       |            <ans2:checksumAlgorithm>SHA-256</ans2:checksumAlgorithm>
       |            <ans2:fileSize>$fileSize</ans2:fileSize>
       |            <ans2:compressed>false</ans2:compressed>
       |            <ans2:properties>
       |                <ans2:property>
       |                    <ans2:name>CaseReference</ans2:name>
       |                    <ans2:value>$caseReference</ans2:value>
       |                </ans2:property>
       |                <ans2:property>
       |                    <ans2:name>Eori</ans2:name>
       |                    <ans2:value>$eori</ans2:value>
       |                </ans2:property>
       |                <ans2:property>
       |                    <ans2:name>DeclarationId</ans2:name>
       |                    <ans2:value>$declarationId</ans2:value>
       |                </ans2:property>
       |                <ans2:property>
       |                    <ans2:name>DeclarationType</ans2:name>
       |                    <ans2:value>$declarationType</ans2:value>
       |                </ans2:property>
       |                <ans2:property>
       |                    <ans2:name>ApplicationName</ans2:name>
       |                    <ans2:value>$applicationName</ans2:value>
       |                </ans2:property>
       |                <ans2:property>
       |                    <ans2:name>DocumentType</ans2:name>
       |                    <ans2:value>$documentType</ans2:value>
       |                </ans2:property>
       |                <ans2:property>
       |                    <ans2:name>DocumentReceivedDate</ans2:name>
       |                    <ans2:value>$documentReceivedDate</ans2:value>
       |                </ans2:property>
       |            </ans2:properties>
       |            <ans2:sourceLocation>$sourceLocation</ans2:sourceLocation>
       |            <ans2:sourceFileName>$sourceFileName</ans2:sourceFileName>
       |            <ans2:sourceFileMimeType>$sourceFileMimeType</ans2:sourceFileMimeType>
       |            <ans2:destinations>
       |                <ans2:destination>
       |                    <ans2:destinationSystem>$destinationSystem</ans2:destinationSystem>
       |                </ans2:destination>
       |            </ans2:destinations>
       |        </ans2:BatchFileInterfaceMetadata>
       |    </ans1:Body>
       |</ans1:Envelope>
       |""".stripMargin
      .filter(_ >= ' ')
      .replaceAll(">[\\s\r\n]*<", "><")

  def mockCcsSubmissionRequestGet()(
    response: Either[Error, Option[WorkItem[CcsSubmissionRequest]]]
  ): CallHandler0[EitherT[Future, models.Error, Option[WorkItem[CcsSubmissionRequest]]]] =
    (() => ccsSubmissionRepositoryMock.get)
      .expects()
      .returning(EitherT.fromEither[Future](response))

  def mockCcsSubmissionRequest()(
    response: Either[Error, WorkItem[CcsSubmissionRequest]]
  ): CallHandler1[CcsSubmissionRequest, EitherT[Future, models.Error, WorkItem[CcsSubmissionRequest]]] =
    (ccsSubmissionRepositoryMock
      .set(_: CcsSubmissionRequest))
      .expects(*)
      .returning(EitherT.fromEither[Future](response))

  def mockSetProcessingStatus(id: ObjectId, status: ProcessingStatus)(
    response: Either[Error, Boolean]
  ): CallHandler2[ObjectId, ProcessingStatus, EitherT[Future, models.Error, Boolean]] =
    (ccsSubmissionRepositoryMock
      .setProcessingStatus(_: ObjectId, _: ProcessingStatus))
      .expects(id, status)
      .returning(EitherT.fromEither[Future](response))

  def mockSetResultStatus(id: ObjectId, status: ResultStatus)(
    response: Either[Error, Boolean]
  ): CallHandler2[ObjectId, ResultStatus, EitherT[Future, models.Error, Boolean]] =
    (ccsSubmissionRepositoryMock
      .setResultStatus(_: ObjectId, _: ResultStatus))
      .expects(id, status)
      .returning(EitherT.fromEither[Future](response))

  def mockDec64Submission(ccsSubmissionPayload: CcsSubmissionPayload)(
    response: Either[Error, HttpResponse]
  ): CallHandler2[CcsSubmissionPayload, HeaderCarrier, EitherT[Future, Error, HttpResponse]] =
    (ccsConnectorMock
      .submitToCcs(_: CcsSubmissionPayload)(_: HeaderCarrier))
      .expects(ccsSubmissionPayload, *)
      .returning(EitherT[Future, Error, HttpResponse](Future.successful(response)))

  val actorSystem: ActorSystem                                                          = ActorSystem()
  implicit val ccsSubmissionPollerExecutionContext: CcsSubmissionPollerExecutionContext =
    new CcsSubmissionPollerExecutionContext(actorSystem)

  val ccsSubmissionService = new DefaultCcsSubmissionService(ccsConnectorMock, ccsSubmissionRepositoryMock)

  "Ccs Submission Service" when {

    "the submission poller requests a work item" must {
      "dequeue the next work item" in forAll { (workItem: WorkItem[CcsSubmissionRequest]) =>
        mockCcsSubmissionRequestGet()(Right(Some(workItem)))
        await(ccsSubmissionService.dequeue.value) shouldBe Right(Some(workItem))
      }
    }

    "a ccs submission request is made" must {

      "enqueue the Single Rejected Goods claim request" in forAll {
        (submitRequest: RejectedGoodsClaimRequest[SingleRejectedGoodsClaim], submitResponse: ClaimSubmitResponse) =>
          whenever(submitRequest.claim.supportingEvidences.nonEmpty) {
            val evidence              = submitRequest.claim.supportingEvidences.head
            val singleDocumentRequest =
              submitRequest.copy(claim = submitRequest.claim.copy(supportingEvidences = evidence :: Nil))

            val dec64payload = makeDec64XmlPayload(
              correlationId = UUID.randomUUID().toString,
              batchId = UUID.randomUUID().toString,
              batchSize = singleDocumentRequest.claim.supportingEvidences.size.toLong,
              batchCount = singleDocumentRequest.claim.supportingEvidences.size.toLong,
              checksum = evidence.checksum,
              fileSize = evidence.size,
              caseReference = submitResponse.caseNumber,
              eori = singleDocumentRequest.claim.claimantInformation.eori.value,
              declarationId = singleDocumentRequest.claim.leadMrn.value,
              declarationType = "MRN",
              applicationName = "NDRC",
              documentType = evidence.documentType.toDec64DisplayString,
              documentReceivedDate = evidence.uploadedOn.toCdsDateTime,
              sourceLocation = evidence.downloadUrl,
              sourceFileName = evidence.fileName,
              sourceFileMimeType = evidence.fileMimeType,
              destinationSystem = "CDFPay"
            )

            val workItem = WorkItem(
              id = new ObjectId(),
              receivedAt = Instant.now(),
              updatedAt = Instant.now(),
              availableAt = Instant.now(),
              status = ToDo,
              failureCount = 0,
              item = CcsSubmissionRequest(payload = dec64payload, headers = getHeaders(hc))
            )

            mockCcsSubmissionRequest()(Right(workItem))

            await(ccsSubmissionService.enqueue(singleDocumentRequest, submitResponse).value).isRight should be(true)
          }
      }
    }

    "enqueue the Multiple Rejected Goods claim request" in forAll {
      (submitRequest: RejectedGoodsClaimRequest[MultipleRejectedGoodsClaim], submitResponse: ClaimSubmitResponse) =>
        whenever(submitRequest.claim.supportingEvidences.nonEmpty) {
          val evidence              = submitRequest.claim.supportingEvidences.head
          val singleDocumentRequest =
            submitRequest.copy(claim = submitRequest.claim.copy(supportingEvidences = evidence :: Nil))

          val dec64payload = makeDec64XmlPayload(
            correlationId = UUID.randomUUID().toString,
            batchId = UUID.randomUUID().toString,
            batchSize = singleDocumentRequest.claim.supportingEvidences.size.toLong,
            batchCount = singleDocumentRequest.claim.supportingEvidences.size.toLong,
            checksum = evidence.checksum,
            fileSize = evidence.size,
            caseReference = submitResponse.caseNumber,
            eori = singleDocumentRequest.claim.claimantInformation.eori.value,
            declarationId = singleDocumentRequest.claim.leadMrn.value,
            declarationType = "MRN",
            applicationName = "NDRC",
            documentType = evidence.documentType.toDec64DisplayString,
            documentReceivedDate = evidence.uploadedOn.toCdsDateTime,
            sourceLocation = evidence.downloadUrl,
            sourceFileName = evidence.fileName,
            sourceFileMimeType = evidence.fileMimeType,
            destinationSystem = "CDFPay"
          )

          val workItem = WorkItem(
            id = new ObjectId(),
            receivedAt = Instant.now(),
            updatedAt = Instant.now(),
            availableAt = Instant.now(),
            status = ToDo,
            failureCount = 0,
            item = CcsSubmissionRequest(payload = dec64payload, headers = getHeaders(hc))
          )

          mockCcsSubmissionRequest()(Right(workItem))

          await(ccsSubmissionService.enqueue(singleDocumentRequest, submitResponse).value).isRight should be(true)
        }
    }

    "the submission poller updates the processing status" must {
      "return true to indicate that the status has been updated" in forAll {
        (workItem: WorkItem[CcsSubmissionRequest]) =>
          mockSetProcessingStatus(workItem.id, Failed)(Right(true))
          await(ccsSubmissionService.setProcessingStatus(workItem.id, Failed).value) shouldBe Right(true)
      }
    }

    "the submission poller updates the complete status" must {
      "return true to indicate that the status has been updated" in forAll {
        (workItem: WorkItem[CcsSubmissionRequest]) =>
          mockSetResultStatus(workItem.id, PermanentlyFailed)(Right(true))
          await(ccsSubmissionService.setResultStatus(workItem.id, PermanentlyFailed).value) shouldBe Right(true)
      }
    }

    "a ccs file submission request is made" must {

      "return an error" when {

        "there is an issue with the DEC-64 service" in forAll { (ccsSubmissionPayload: CcsSubmissionPayload) =>
          inSequence {
            mockDec64Submission(ccsSubmissionPayload.copy(headers = getHeaders(hc)))(
              Left(Error("dec-64 service error"))
            )
          }

          await(
            ccsSubmissionService
              .submitToCcs(ccsSubmissionPayload)
              .value
          ).isLeft shouldBe true
        }
      }

      "return a http no content success status when the file has been successfully submitted to the DEC-64 service" in {
        forAll { (ccsSubmissionPayload: CcsSubmissionPayload) =>
          val response = HttpResponse(204, "Success")
          inSequence {
            mockDec64Submission(ccsSubmissionPayload.copy(headers = getHeaders(hc)))(Right(response))
          }
          await(
            ccsSubmissionService
              .submitToCcs(
                ccsSubmissionPayload
              )
              .value
          ) shouldBe Right(
            response
          )
        }
      }
    }
  }
}
