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
import play.api.http.{HeaderNames, Status}
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.cdsreimbursementclaim.models.Generators._
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error, GetDeclarationResponse, OverpaymentDeclarationDisplayResponse, ResponseDetail}
import uk.gov.hmrc.cdsreimbursementclaim.services.DeclarationServiceImpl
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.Future

class GetDeclarationControllerSpec extends BaseSpec with DefaultAwaitTimeout {

  implicit val ec            = scala.concurrent.ExecutionContext.Implicits.global
  val httpClient             = mock[HttpClient]
  val declarationInfoService = mock[DeclarationServiceImpl]
  private val fakeRequest    = FakeRequest("POST", "/", FakeHeaders(Seq(HeaderNames.HOST -> "localhost")), JsObject.empty)
  private val controller     = new GetDeclarationController(declarationInfoService, Helpers.stubControllerComponents())

  def mockEisResponse(response: EitherT[Future, Error, GetDeclarationResponse]) =
    (declarationInfoService
      .getDeclaration(_: String)(_: HeaderCarrier))
      .expects(*, *)
      .returning(response)

  "POST" should {
    "return 200" in {
      val responseDetail                        = sample[ResponseDetail]
      val overpaymentDeclarationDisplayResponse =
        sample[OverpaymentDeclarationDisplayResponse].copy(responseDetail = Some(responseDetail))
      val response                              = sample[GetDeclarationResponse].copy(overpaymentDeclarationDisplayResponse =
        overpaymentDeclarationDisplayResponse
      )
      val declarationId                         =
        response.overpaymentDeclarationDisplayResponse.responseDetail.map(_.declarationId).getOrElse(fail)
      mockEisResponse(EitherT.right(Future.successful(response)))
      val result                                = controller.declaration(declarationId)(fakeRequest)
      status(result)        shouldBe Status.OK
      contentAsJson(result) shouldBe Json.toJson(response)
    }

    "return 500 when on any error" in {
      val declarationId = "GB349970632046"
      mockEisResponse(EitherT.left(Future.successful(Error("Resource Unavailable"))))
      val result        = controller.declaration(declarationId)(fakeRequest)
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

  }
}
