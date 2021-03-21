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
import org.scalamock.handlers.CallHandler3
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Headers, Request, WrappedRequest}
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.cdsreimbursementclaim.Fake
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.AuthenticatedRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{SubmitClaimRequest, SubmitClaimResponse}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.CcsSubmissionGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
import uk.gov.hmrc.cdsreimbursementclaim.services.ClaimService
import uk.gov.hmrc.cdsreimbursementclaim.services.ccs.{CcsSubmissionRequest, CcsSubmissionService}
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

  private val controller = new SubmitClaimController(
    authenticate = Fake.login(Fake.user, LocalDateTime.of(2020, 1, 1, 15, 47, 20)),
    mockClaimService,
    mockCcsSubmissionService,
    Helpers.stubControllerComponents()
  )

  def mockSubmitClaimService(request: SubmitClaimRequest)(
    response: Either[Error, SubmitClaimResponse]
  ): CallHandler3[SubmitClaimRequest, HeaderCarrier, Request[_], EitherT[Future, Error, SubmitClaimResponse]] =
    (mockClaimService
      .submitClaim(_: SubmitClaimRequest)(_: HeaderCarrier, _: Request[_]))
      .expects(request, *, *)
      .returning(EitherT.fromEither[Future](response))

  def mockCcsRequestEnqueue(
    submitClaimRequest: SubmitClaimRequest,
    submitClaimResponse: SubmitClaimResponse
  ): CallHandler3[SubmitClaimRequest, SubmitClaimResponse, HeaderCarrier, EitherT[Future, Error, List[
    WorkItem[CcsSubmissionRequest]
  ]]] =
    (mockCcsSubmissionService
      .enqueue(_: SubmitClaimRequest, _: SubmitClaimResponse)(_: HeaderCarrier))
      .expects(submitClaimRequest, submitClaimResponse, *)
      .returning(EitherT.pure(List(ccsSubmissionRequestWorkItem)))

  def fakeRequestWithJsonBody(body: JsValue): WrappedRequest[JsValue] =
    request.withHeaders(Headers.apply(CONTENT_TYPE -> JSON)).withBody(body)

  "Submit Controller" when {

    "handling submit claim requests" should {

      "return the claim reference number" in {

        val submitClaimRequest  = sample[SubmitClaimRequest]
        val submitClaimResponse = sample[SubmitClaimResponse]

        inSequence {
          mockSubmitClaimService(submitClaimRequest)(Right(submitClaimResponse))
          mockCcsRequestEnqueue(submitClaimRequest, submitClaimResponse)
        }

        val result = controller.submitClaim()(fakeRequestWithJsonBody(Json.toJson(submitClaimRequest)))
        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(submitClaimResponse)
      }

      "return an error if the submission of the claim failed" in {

        val submitClaimRequest = sample[SubmitClaimRequest]

        inSequence {
          mockSubmitClaimService(submitClaimRequest)(Left(Error("boom!")))
        }

        val result = controller.submitClaim()(fakeRequestWithJsonBody(Json.toJson(submitClaimRequest)))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

  }
}
