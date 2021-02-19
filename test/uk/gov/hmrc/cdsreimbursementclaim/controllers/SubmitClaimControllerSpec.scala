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

import java.time.{Instant, LocalDateTime}

import cats.data.EitherT
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.cdsreimbursementclaim.Fake
import uk.gov.hmrc.cdsreimbursementclaim.models.upscan.UpscanCallBack.{UploadDetails, UpscanSuccess}
import uk.gov.hmrc.cdsreimbursementclaim.models.upscan.{UploadReference, UpscanCallBack, UpscanUpload}
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error, FrontendSubmitClaim, SubmitClaimRequest, SubmitClaimResponse, WorkItemPayload}
import uk.gov.hmrc.cdsreimbursementclaim.services.upscan.UpscanService
import uk.gov.hmrc.cdsreimbursementclaim.services.{FileUploadQueue, SubmitClaimService}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.workitem.WorkItem

import scala.concurrent.Future

class SubmitClaimControllerSpec extends AnyWordSpec with Matchers with MockFactory with DefaultAwaitTimeout {

  implicit val ec           = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc           = HeaderCarrier()
  implicit val materializer = NoMaterializer
  val httpClient            = mock[HttpClient]
  val authAction            = Fake.login(Fake.user, LocalDateTime.of(2020, 1, 1, 15, 47, 20))
  val eisService            = mock[SubmitClaimService]
  val upscanService         = mock[UpscanService]
  val fileUploadQueue       = mock[FileUploadQueue]
  private val fakeRequest = {
    import uk.gov.hmrc.cdsreimbursementclaim.models.GenerateFrontendSubmitClaim._
    FakeRequest(
      "POST",
      "/",
      FakeHeaders(Seq(HeaderNames.HOST -> "localhost")),
      Json.toJson(sample[FrontendSubmitClaim])
    )
  }
  val controller            =
    new SubmitClaimController(
      authAction,
      eisService,
      upscanService,
      fileUploadQueue,
      Helpers.stubControllerComponents()
    )

  def mockEisResponse(response: EitherT[Future, Error, SubmitClaimResponse]) =
    (eisService
      .submitClaim(_: SubmitClaimRequest)(_: HeaderCarrier))
      .expects(*, *)
      .returning(response)

  def mockUpscan(response: EitherT[Future, Error, List[UpscanUpload]]) =
    (upscanService.readUpscanUploads(_: List[UploadReference])).expects(*).returning(response)

  def mockUploadQueue(workItem: Future[WorkItem[WorkItemPayload]]) =
    (fileUploadQueue
      .queueRequest(_: String)(_: HeaderCarrier))
      .expects(*, *)
      .returning(workItem)

  def getClaimResponse(caseNumber: Option[String], correlationId: Option[String]) = {
    import uk.gov.hmrc.cdsreimbursementclaim.models.GenerateSubmitClaim._
    val submitClaimResponse = sample[SubmitClaimResponse]
    submitClaimResponse.copy(postNewClaimsResponse =
      submitClaimResponse.postNewClaimsResponse.copy(responseCommon =
        submitClaimResponse.postNewClaimsResponse.responseCommon
          .copy(CDFPayCaseNumber = caseNumber, correlationId = correlationId)
      )
    )
  }

  val upscanSuccess = UpscanSuccess(
    "reference-123",
    "uploaded-123",
    "downloadUrl-123",
    UploadDetails("fileName", "fileMimeType", Instant.now(), "checksum", 1000L)
  )

  def getUpscanUpload(upscanCallBack: Option[UpscanCallBack]) = {
    import uk.gov.hmrc.cdsreimbursementclaim.models.GenerateUpscan._
    sample[UpscanUpload].copy(upscanCallBack = upscanCallBack)
  }

  val workItem = {
    import uk.gov.hmrc.cdsreimbursementclaim.models.GenerateWorkItem._
    sample[WorkItem[WorkItemPayload]]
  }

  "POST" should {
    "return 200" in {
      mockUploadQueue(Future.successful(workItem))
      val claimResponse = getClaimResponse(Some("CaseNumber-12345"), Some("correlationID-12345"))
      mockEisResponse(EitherT.right(Future.successful(claimResponse)))
      val upscanUpload  = getUpscanUpload(Some(upscanSuccess))
      mockUpscan(EitherT.rightT(List(upscanUpload)))
      val result        = controller.claim()(fakeRequest)
      status(result)                                shouldBe Status.OK
      contentAsJson(result).as[SubmitClaimResponse] shouldBe claimResponse
    }

    "fail if no caseNumber returned" in {
      val claimResponse = getClaimResponse(None, Some("correlationID-12345"))
      mockEisResponse(EitherT.right(Future.successful(claimResponse)))
      val upscanUpload  = getUpscanUpload(Some(upscanSuccess))
      mockUpscan(EitherT.rightT(List(upscanUpload)))
      val result        = controller.claim()(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "fail if no correlationId returned" in {
      val claimResponse = getClaimResponse(Some("CaseNumber-12345"), None)
      mockEisResponse(EitherT.right(Future.successful(claimResponse)))
      val upscanUpload  = getUpscanUpload(Some(upscanSuccess))
      mockUpscan(EitherT.rightT(List(upscanUpload)))
      val result        = controller.claim()(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "fail if no Upscan callback returned" in {
      val claimResponse = getClaimResponse(Some("CaseNumber-12345"), Some("correlationID-12345"))
      mockEisResponse(EitherT.right(Future.successful(claimResponse)))
      val upscanUpload  = getUpscanUpload(None)
      mockUpscan(EitherT.rightT(List(upscanUpload)))
      val result        = controller.claim()(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "fail if work item repo queue fails" in {
      mockUploadQueue(Future.failed(new Exception("Boom")))
      val claimResponse = getClaimResponse(Some("CaseNumber-12345"), Some("correlationID-12345"))
      mockEisResponse(EitherT.right(Future.successful(claimResponse)))
      val upscanUpload  = getUpscanUpload(Some(upscanSuccess))
      mockUpscan(EitherT.rightT(List(upscanUpload)))
      val result        = controller.claim()(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 500 on any error" in {
      mockEisResponse(EitherT.left(Future.successful(Error("Resource Unavailable"))))
      val result = controller.claim()(fakeRequest)
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

  }
}
