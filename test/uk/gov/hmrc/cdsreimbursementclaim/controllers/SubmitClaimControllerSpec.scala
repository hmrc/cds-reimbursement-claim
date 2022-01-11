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

package uk.gov.hmrc.cdsreimbursementclaim.controllers

import cats.data.EitherT
import org.scalamock.handlers.CallHandler4
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Headers, Request, WrappedRequest}
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.cdsreimbursementclaim.Fake
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.AuthenticatedRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{C285ClaimRequest, ClaimSubmitResponse, RejectedGoodsClaimRequest}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.C285ClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.CcsSubmissionGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.RejectedGoodsClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TPI05RequestGen._
import uk.gov.hmrc.cdsreimbursementclaim.services.ClaimService
import uk.gov.hmrc.cdsreimbursementclaim.services.ccs.{CcsSubmissionRequest, CcsSubmissionService, ClaimToDec64FilesMapper}
import uk.gov.hmrc.cdsreimbursementclaim.services.tpi05.CE1779ClaimToTPI05Mapper.CE1779ClaimData
import uk.gov.hmrc.cdsreimbursementclaim.services.tpi05.ClaimToTPI05Mapper
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.workitem.WorkItem

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmitClaimControllerSpec extends ControllerSpec {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val mockClaimService: ClaimService                 = mock[ClaimService]
  val mockCcsSubmissionService: CcsSubmissionService = mock[CcsSubmissionService]

  val ccsSubmissionRequestWorkItem: WorkItem[CcsSubmissionRequest] = sample[WorkItem[CcsSubmissionRequest]]

  val request = new AuthenticatedRequest(
    Fake.user,
    LocalDateTime.now(),
    headerCarrier,
    FakeRequest()
  )

  private lazy val controller = new SubmitClaimController(
    authenticate = Fake.login(Fake.user, LocalDateTime.of(2020, 1, 1, 15, 47, 20)),
    mockClaimService,
    mockCcsSubmissionService,
    Helpers.stubControllerComponents()
  )

  def mockC285ClaimSubmission(request: C285ClaimRequest)(
    response: Either[Error, ClaimSubmitResponse]
  ): CallHandler4[C285ClaimRequest, HeaderCarrier, Request[_], ClaimToTPI05Mapper[C285ClaimRequest], EitherT[
    Future,
    Error,
    ClaimSubmitResponse
  ]] =
    (mockClaimService
      .submitC285Claim(_: C285ClaimRequest)(_: HeaderCarrier, _: Request[_], _: ClaimToTPI05Mapper[C285ClaimRequest]))
      .expects(request, *, *, *)
      .returning(EitherT.fromEither[Future](response))

  def mockRejectedGoodsClaimSubmission(request: RejectedGoodsClaimRequest)(
    response: Either[Error, ClaimSubmitResponse]
  ): CallHandler4[RejectedGoodsClaimRequest, HeaderCarrier, Request[_], ClaimToTPI05Mapper[CE1779ClaimData], EitherT[
    Future,
    Error,
    ClaimSubmitResponse
  ]] =
    (mockClaimService
      .submitRejectedGoodsClaim(_: RejectedGoodsClaimRequest)(
        _: HeaderCarrier,
        _: Request[_],
        _: ClaimToTPI05Mapper[CE1779ClaimData]
      ))
      .expects(request, *, *, *)
      .returning(EitherT.fromEither[Future](response))

  def mockCcsRequestEnqueue[A](
    submitClaimRequest: A,
    submitClaimResponse: ClaimSubmitResponse
  ): CallHandler4[A, ClaimSubmitResponse, HeaderCarrier, ClaimToDec64FilesMapper[A], EitherT[Future, Error, List[
    WorkItem[CcsSubmissionRequest]
  ]]] =
    (mockCcsSubmissionService
      .enqueue(_: A, _: ClaimSubmitResponse)(
        _: HeaderCarrier,
        _: ClaimToDec64FilesMapper[A]
      ))
      .expects(submitClaimRequest, submitClaimResponse, *, *)
      .returning(EitherT.pure(List(ccsSubmissionRequestWorkItem)))

  def fakeRequestWithJsonBody(body: JsValue): WrappedRequest[JsValue] =
    request.withHeaders(Headers.apply(CONTENT_TYPE -> JSON)).withBody(body)

  "The controller" should {

    "succeed returning claim reference number" when {

      "handling C285 claim request" in {
        val submitClaimRequest  = sample[C285ClaimRequest]
        val submitClaimResponse = sample[ClaimSubmitResponse]

        inSequence {
          mockC285ClaimSubmission(submitClaimRequest)(Right(submitClaimResponse))
          mockCcsRequestEnqueue(submitClaimRequest, submitClaimResponse)
        }

        val result = controller.submitC285Claim()(fakeRequestWithJsonBody(Json.toJson(submitClaimRequest)))
        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(submitClaimResponse)
      }

      "handling C&E1779 claim request" in {
        val submitClaimRequest  = sample[RejectedGoodsClaimRequest]
        val submitClaimResponse = sample[ClaimSubmitResponse]

        inSequence {
          mockRejectedGoodsClaimSubmission(submitClaimRequest)(Right(submitClaimResponse))
          mockCcsRequestEnqueue(submitClaimRequest, submitClaimResponse)
        }

        val result = controller.submitRejectedGoodsClaim()(fakeRequestWithJsonBody(Json.toJson(submitClaimRequest)))
        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(submitClaimResponse)
      }
    }

    "fail" when {

      "submission of the C285 claim failed" in {
        val submitClaimRequest = sample[C285ClaimRequest]

        inSequence {
          mockC285ClaimSubmission(submitClaimRequest)(Left(Error("boom!")))
        }

        val result = controller.submitC285Claim()(fakeRequestWithJsonBody(Json.toJson(submitClaimRequest)))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "submission of the C&E1779 claim failed" in {
        val submitClaimRequest = sample[RejectedGoodsClaimRequest]

        inSequence {
          mockRejectedGoodsClaimSubmission(submitClaimRequest)(Left(Error("boom!")))
        }

        val result = controller.submitRejectedGoodsClaim()(fakeRequestWithJsonBody(Json.toJson(submitClaimRequest)))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
