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
import play.api.libs.json.{JsObject, JsString, JsValue}
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.cdsreimbursementclaim.config.AppConfig
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.services.DeclarationInfoService
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.Future

class DeclarationInfoControllerSpec extends ControllerSpec with DefaultAwaitTimeout {

  implicit val ec            = scala.concurrent.ExecutionContext.Implicits.global
  implicit val materialiser  = NoMaterializer
  val httpClient             = mock[HttpClient]
  implicit val hc            = HeaderCarrier()
  val appConfig              = instanceOf[AppConfig]
  val declarationInfoService = mock[DeclarationInfoService]
  private val fakeRequest    = FakeRequest("POST", "/", FakeHeaders(Seq(HeaderNames.HOST -> "localhost")), JsObject.empty)
  private val controller     = new DeclarationInfoController(declarationInfoService, Helpers.stubControllerComponents())

  def mockEisResponse(response: EitherT[Future, Error, JsValue]) =
    (declarationInfoService
      .getDeclarationInfo(_: JsValue)(_: HeaderCarrier))
      .expects(*, *)
      .returning(response)

  "POST" should {
    "return 200" in {
      val response = JsObject(Seq("hello" -> JsString("word")))
      mockEisResponse(EitherT.right(Future.successful(response)))
      val result   = controller.getDeclarationInfo()(fakeRequest)
      status(result)        shouldBe Status.OK
      contentAsJson(result) shouldBe response
    }

    "return 500 when on any error" in {
      mockEisResponse(EitherT.left(Future.successful(Error("Resource Unavailable"))))
      val result = controller.getDeclarationInfo()(fakeRequest)
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

  }
}
