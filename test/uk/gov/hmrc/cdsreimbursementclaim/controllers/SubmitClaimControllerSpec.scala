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
import org.scalamock.scalatest.MockFactory
import org.scalatest.Ignore
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.JsObject
import play.api.mvc.Request
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{SubmitClaimRequest, SubmitClaimResponse}
import uk.gov.hmrc.cdsreimbursementclaim.services.SubmitClaimService
import uk.gov.hmrc.cdsreimbursementclaim.services.ccs.CcsSubmissionService
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.Future

@Ignore
class SubmitClaimControllerSpec extends AnyWordSpec with Matchers with MockFactory with DefaultAwaitTimeout {

  implicit val ec         = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc         = HeaderCarrier()
  val httpClient          = mock[HttpClient]
  val eisService          = mock[SubmitClaimService]
  val ccs                 = mock[CcsSubmissionService]
  private val fakeRequest = FakeRequest("POST", "/", FakeHeaders(Seq(HeaderNames.HOST -> "localhost")), JsObject.empty)
  private val controller  = new SubmitClaimController(eisService, ccs, Helpers.stubControllerComponents())

  def mockEisResponse(response: EitherT[Future, Error, SubmitClaimResponse]) =
    (eisService
      .submitClaim(_: SubmitClaimRequest)(_: HeaderCarrier, _: Request[_]))
      .expects(*, *, *)
      .returning(response)

  "POST" should {
    "return 200" in {
      val response = SubmitClaimResponse("")
      mockEisResponse(EitherT.right(Future.successful(response)))
      val result   = controller.submitClaim()(fakeRequest)
      status(result)        shouldBe Status.OK
      contentAsJson(result) shouldBe response
    }

    "return 500 when on any error" in {
      mockEisResponse(EitherT.left(Future.successful(Error("Resource Unavailable"))))
      val result = controller.submitClaim()(fakeRequest)
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

  }
}
