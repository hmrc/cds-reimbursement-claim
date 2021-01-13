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

import play.api.http.{HeaderNames, Status}
import play.api.libs.json.{JsObject, JsString, JsValue, Json, Writes}
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.cdsreimbursementclaim.config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class PostNewClaimControllerSpec extends ControllerSpec with DefaultAwaitTimeout {

  val appConfig = instanceOf[AppConfig]
  val httpClient = mock[HttpClient]

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val materialiser = NoMaterializer
  private val fakeRequest = FakeRequest("POST", "/", FakeHeaders(Seq(HeaderNames.HOST -> "localhost")) , JsObject.empty)
  private val controller  = new PostNewClaimController(appConfig,httpClient, Helpers.stubControllerComponents())

  "POST" should {
    "return 200" in {
      val response = JsObject(Seq("hello" -> JsString("word")))
      (httpClient
        .POST[JsValue,HttpResponse](_:String,_:JsValue,_:Seq[(String, String)])(_: Writes[JsValue],_: HttpReads[HttpResponse],_: HeaderCarrier, _ : ExecutionContext))
        .expects(*,*,*,*,*,*,*)
        .returning(Future.successful(HttpResponse(OK,Json.stringify(response))))

      val result = controller.claim()(fakeRequest)
      status(result) shouldBe Status.OK
      contentAsJson(result) shouldBe response
    }

    "return 404 when resource is unavailable" in {
      (httpClient
        .POST[JsValue,HttpResponse](_:String,_:JsValue,_:Seq[(String, String)])(_: Writes[JsValue],_: HttpReads[HttpResponse],_: HeaderCarrier, _ : ExecutionContext))
        .expects(*,*,*,*,*,*,*)
        .returning(Future.successful(HttpResponse(NOT_FOUND,"Resource Unavailable")))

      val result = controller.claim()(fakeRequest)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe "PostNewClaimController response status: 404, body: Resource Unavailable"
    }

  }
}
