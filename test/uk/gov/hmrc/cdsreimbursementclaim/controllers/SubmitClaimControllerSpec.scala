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

package uk.gov.hmrc.cdsreimbursementclaim.controllers

import cats.data.EitherT
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{Format, JsValue, Json}
import play.api.mvc.{Headers, Request}
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.cdsreimbursementclaim.Fake
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.AuthenticatedUserRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.C285ClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.CcsSubmissionGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Dec64UploadRequestGen.arbitraryDec64UploadRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.OverpaymentsSingleClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.RejectedGoodsClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.SecuritiesClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TPI05RequestGen._
import uk.gov.hmrc.cdsreimbursementclaim.services.ClaimService
import uk.gov.hmrc.cdsreimbursementclaim.services.ccs.{CcsSubmissionRequest, CcsSubmissionService, ClaimToDec64Mapper}
import uk.gov.hmrc.cdsreimbursementclaim.services.email.{ClaimToEmailMapper, OverpaymentsSingleClaimToEmailMapper}
import uk.gov.hmrc.cdsreimbursementclaim.services.tpi05.{ClaimToTPI05Mapper, OverpaymentsSingleClaimToTPI05Mapper}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext"))
class SubmitClaimControllerSpec extends ControllerSpec with ScalaCheckPropertyChecks {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 1)

  val mockClaimService: ClaimService                 = mock[ClaimService]
  val mockCcsSubmissionService: CcsSubmissionService = mock[CcsSubmissionService]

  val ccsSubmissionRequestWorkItem: WorkItem[CcsSubmissionRequest] = sample[WorkItem[CcsSubmissionRequest]]

  val request = new AuthenticatedUserRequest(
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

  private def mockC285ClaimSubmission(request: C285ClaimRequest)(
    response: Either[Error, ClaimSubmitResponse]
  ) =
    (
      mockClaimService
        .submitC285Claim(_: C285ClaimRequest)(
          _: HeaderCarrier,
          _: Request[_],
          _: ClaimToTPI05Mapper[C285ClaimRequest],
          _: ClaimToEmailMapper[C285ClaimRequest]
        )
      )
      .expects(request, *, *, *, *)
      .returning(EitherT.fromEither[Future](response))

  private def mockSingleOverpaymentsClaimSubmission(request: SingleOverpaymentsClaimRequest)(
    response: Either[Error, ClaimSubmitResponse]
  ) =
    (
      mockClaimService
        .submitSingleOverpaymentsClaim(_: SingleOverpaymentsClaimRequest)(
          _: HeaderCarrier,
          _: Request[_],
          _: OverpaymentsSingleClaimToTPI05Mapper,
          _: OverpaymentsSingleClaimToEmailMapper
        )
      )
      .expects(request, *, *, *, *)
      .returning(EitherT.fromEither[Future](response))

  private def mockRejectedGoodsClaimSubmission[Claim <: RejectedGoodsClaim](request: RejectedGoodsClaimRequest[Claim])(
    response: Either[Error, ClaimSubmitResponse]
  ) =
    (
      mockClaimService
        .submitRejectedGoodsClaim(_: RejectedGoodsClaimRequest[Claim])(
          _: HeaderCarrier,
          _: Request[_],
          _: ClaimToTPI05Mapper[(Claim, List[DisplayDeclaration])],
          _: ClaimToEmailMapper[(Claim, List[DisplayDeclaration])],
          _: Format[RejectedGoodsClaimRequest[Claim]]
        )
      )
      .expects(request, *, *, *, *, *)
      .returning(EitherT.fromEither[Future](response))

  private def mockMultipleRejectedGoodsClaimSubmission(request: RejectedGoodsClaimRequest[MultipleRejectedGoodsClaim])(
    response: Either[Error, ClaimSubmitResponse]
  ) =
    (
      mockClaimService
        .submitMultipleRejectedGoodsClaim(_: RejectedGoodsClaimRequest[MultipleRejectedGoodsClaim])(
          _: HeaderCarrier,
          _: Request[_],
          _: ClaimToTPI05Mapper[(MultipleRejectedGoodsClaim, List[DisplayDeclaration])],
          _: ClaimToEmailMapper[(MultipleRejectedGoodsClaim, List[DisplayDeclaration])],
          _: Format[RejectedGoodsClaimRequest[MultipleRejectedGoodsClaim]]
        )
      )
      .expects(request, *, *, *, *, *)
      .returning(EitherT.fromEither[Future](response))

  private def mockScheduledRejectedGoodsClaimSubmission(
    request: RejectedGoodsClaimRequest[ScheduledRejectedGoodsClaim]
  )(
    response: Either[Error, ClaimSubmitResponse]
  ) =
    (
      mockClaimService
        .submitScheduledRejectedGoodsClaim(_: RejectedGoodsClaimRequest[ScheduledRejectedGoodsClaim])(
          _: HeaderCarrier,
          _: Request[_],
          _: ClaimToEmailMapper[(ScheduledRejectedGoodsClaim, DisplayDeclaration)],
          _: Format[RejectedGoodsClaimRequest[ScheduledRejectedGoodsClaim]]
        )
      )
      .expects(request, *, *, *, *)
      .returning(EitherT.fromEither[Future](response))

  private def mockSecurityClaimSubmission(
    request: SecuritiesClaimRequest
  )(
    response: Either[Error, ClaimSubmitResponse]
  ) =
    (
      mockClaimService
        .submitSecuritiesClaim(_: SecuritiesClaimRequest)(
          _: HeaderCarrier,
          _: Request[_],
          _: ClaimToEmailMapper[(SecuritiesClaim, DisplayDeclaration)],
          _: Format[SecuritiesClaimRequest]
        )
      )
      .expects(request, *, *, *, *)
      .returning(EitherT.fromEither[Future](response))

  private def mockCcsRequestEnqueue[A](
    submitClaimRequest: A,
    submitClaimResponse: ClaimSubmitResponse
  ) =
    (mockCcsSubmissionService
      .enqueue(_: A, _: ClaimSubmitResponse)(
        _: HeaderCarrier,
        _: ClaimToDec64Mapper[A]
      ))
      .expects(submitClaimRequest, submitClaimResponse, *, *)
      .returning(EitherT.pure(List(ccsSubmissionRequestWorkItem)))

  private def fakeRequestWithJsonBody(body: JsValue) =
    request.withHeaders(Headers.apply(CONTENT_TYPE -> JSON)).withBody(body)

  "The controller" should {

    "succeed returning claim reference number" when {

      "handling C285 claim request" in forAll { (request: C285ClaimRequest, response: ClaimSubmitResponse) =>
        inSequence {
          mockC285ClaimSubmission(request)(Right(response))
          mockCcsRequestEnqueue(request, response)
        }

        val result = controller.submitC285Claim()(fakeRequestWithJsonBody(Json.toJson(request)))
        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(response)
      }

      "handling Overpayments single claim request" in forAll {
        (request: SingleOverpaymentsClaimRequest, response: ClaimSubmitResponse) =>
          inSequence {
            mockSingleOverpaymentsClaimSubmission(request)(Right(response))
            mockCcsRequestEnqueue(request, response)
          }

          val result = controller.submitSingleOverpaymentsClaim()(fakeRequestWithJsonBody(Json.toJson(request)))
          status(result)        shouldBe OK
          contentAsJson(result) shouldBe Json.toJson(response)
      }

      "handling Single C&E1779 claim request" in forAll {
        (request: RejectedGoodsClaimRequest[SingleRejectedGoodsClaim], response: ClaimSubmitResponse) =>
          inSequence {
            mockRejectedGoodsClaimSubmission(request)(Right(response))
            mockCcsRequestEnqueue(request, response)
          }

          val result = controller.submitSingleRejectedGoodsClaim()(fakeRequestWithJsonBody(Json.toJson(request)))

          status(result)        shouldBe OK
          contentAsJson(result) shouldBe Json.toJson(response)
      }

      "handling Multiple C&E1779 claim request" in forAll {
        (request: RejectedGoodsClaimRequest[MultipleRejectedGoodsClaim], response: ClaimSubmitResponse) =>
          inSequence {
            mockMultipleRejectedGoodsClaimSubmission(request)(Right(response))
            mockCcsRequestEnqueue(request, response)
          }

          val result = controller.submitMultipleRejectedGoodsClaim()(fakeRequestWithJsonBody(Json.toJson(request)))

          status(result)        shouldBe OK
          contentAsJson(result) shouldBe Json.toJson(response)
      }

      "handling Scheduled C&E1779 claim request" in forAll {
        (request: RejectedGoodsClaimRequest[ScheduledRejectedGoodsClaim], response: ClaimSubmitResponse) =>
          inSequence {
            mockScheduledRejectedGoodsClaimSubmission(request)(Right(response))
            mockCcsRequestEnqueue(request, response)
          }

          val result = controller.submitScheduledRejectedGoodsClaim()(fakeRequestWithJsonBody(Json.toJson(request)))

          status(result)        shouldBe OK
          contentAsJson(result) shouldBe Json.toJson(response)
      }

      "handling securities claim request" in forAll {
        (request: SecuritiesClaimRequest, response: ClaimSubmitResponse) =>
          inSequence {
            mockSecurityClaimSubmission(request)(Right(response))
            mockCcsRequestEnqueue(request, response)
          }

          val result = controller.submitSecuritiesClaim()(fakeRequestWithJsonBody(Json.toJson(request)))

          status(result)        shouldBe OK
          contentAsJson(result) shouldBe Json.toJson(response)
      }

      "handling files request" in forAll { (request: Dec64UploadRequest) =>
        inSequence {
          mockCcsRequestEnqueue(request, ClaimSubmitResponse(request.caseNumber))
        }

        val result = controller.submitFiles()(fakeRequestWithJsonBody(Json.toJson(request)))

        status(result) shouldBe ACCEPTED
      }
    }

    "fail" when {

      "submission of the C285 claim failed" in forAll { request: C285ClaimRequest =>
        inSequence {
          mockC285ClaimSubmission(request)(Left(Error("boom!")))
        }

        val result = controller.submitC285Claim()(fakeRequestWithJsonBody(Json.toJson(request)))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "submission of the single overpayments claim failed" in forAll { request: SingleOverpaymentsClaimRequest =>
        inSequence {
          mockSingleOverpaymentsClaimSubmission(request)(Left(Error("boom!")))
        }

        val result = controller.submitSingleOverpaymentsClaim()(fakeRequestWithJsonBody(Json.toJson(request)))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "submission of the Single C&E1779 claim failed" in forAll {
        request: RejectedGoodsClaimRequest[SingleRejectedGoodsClaim] =>
          inSequence {
            mockRejectedGoodsClaimSubmission(request)(Left(Error("boom!")))
          }

          val result = controller.submitSingleRejectedGoodsClaim()(fakeRequestWithJsonBody(Json.toJson(request)))
          status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "submission of the Multiple C&E1779 claim failed" in forAll {
        request: RejectedGoodsClaimRequest[MultipleRejectedGoodsClaim] =>
          inSequence {
            mockMultipleRejectedGoodsClaimSubmission(request)(Left(Error("boom!")))
          }

          val result = controller.submitMultipleRejectedGoodsClaim()(fakeRequestWithJsonBody(Json.toJson(request)))
          status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "submission of the Scheduled C&E1779 claim failed" in forAll {
        request: RejectedGoodsClaimRequest[ScheduledRejectedGoodsClaim] =>
          inSequence {
            mockScheduledRejectedGoodsClaimSubmission(request)(Left(Error("boom!")))
          }

          val result = controller.submitScheduledRejectedGoodsClaim()(fakeRequestWithJsonBody(Json.toJson(request)))
          status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "submission of the securities claim failed" in forAll { request: SecuritiesClaimRequest => //TODO: 1718
        inSequence {
          mockSecurityClaimSubmission(request)(Left(Error("boom!")))
        }

        val result = controller.submitSecuritiesClaim()(fakeRequestWithJsonBody(Json.toJson(request)))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
