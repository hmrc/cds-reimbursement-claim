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

package uk.gov.hmrc.cdsreimbursementclaim.services

import cats.data.EitherT
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.cdsreimbursementclaim.connectors.DeclarationInfoConnector
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeclarationInfoServiceSpec extends AnyWordSpec with Matchers with MockFactory {

  val declarationInfoConnector = mock[DeclarationInfoConnector]

  val declarationInfoService = new DeclarationInfoService(declarationInfoConnector)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val request: Request[_] = FakeRequest()
  val okResponse: JsValue          = Json.parse(s"""{
                                                       |  "overpaymentDeclarationDisplayResponse": {
                                                       |    "responseCommon": {
                                                       |      "status": "OK",
                                                       |      "processingDate": "9447-42-84T35:41:48Z"
                                                       |    }
                                                       |  }
                                                       |}""".stripMargin)

  def errorResponse(errorMessage: String): JsValue = Json.parse(s"""{
                                                                   |   "ErrorDetails":{
                                                                   |      "ProcessingDateTime":"2016-10-10T13:52:16Z",
                                                                   |      "CorrelationId":"d60de98c-f499-47f5-b2d6-e80966e8d19e",
                                                                   |      "ErrorMessage":"$errorMessage"
                                                                   |    }
                                                                   |}""".stripMargin)

  def mockDeclarationConnector(declarationInfo: JsValue)(response: Either[Error, HttpResponse]) =
    (declarationInfoConnector
      .getDeclarationInfo(_: JsValue)(_: HeaderCarrier))
      .expects(declarationInfo, *)
      .returning(EitherT.fromEither[Future](response))
      .atLeastOnce()

  "Declaration Information Request Service" when {
    "handling a request returns" should {
      "handle successful submits" when {
        "there is a valid payload" in {
          mockDeclarationConnector(JsString("Hello"))(
            Right(HttpResponse(200, okResponse, Map.empty[String, Seq[String]]))
          )
          await(declarationInfoService.getDeclarationInfo(JsString("Hello")).value) shouldBe Right(okResponse)
        }
      }

      "handle unsuccesful submits" when {
        "400 response" in {
          val decInfoResponse = errorResponse("Invalid Request")
          mockDeclarationConnector(JsString("Hello"))(
            Right(HttpResponse(400, decInfoResponse, Map.empty[String, Seq[String]]))
          )
          val response        = await(declarationInfoService.getDeclarationInfo(JsString("Hello")).value)
          response.fold(_.message should include("Invalid Request"), _ => fail())
        }

        "404 response" in {
          val decInfoResponse = errorResponse("Resource is unavailable")
          mockDeclarationConnector(JsString("Hello"))(
            Right(HttpResponse(404, decInfoResponse, Map.empty[String, Seq[String]]))
          )
          val response        = await(declarationInfoService.getDeclarationInfo(JsString("Hello")).value)
          response.fold(_.message should include("Resource is unavailable"), _ => fail())
        }

        "405 response" in {
          val decInfoResponse = errorResponse("Method not allowed")
          mockDeclarationConnector(JsString("Hello"))(
            Right(HttpResponse(405, decInfoResponse, Map.empty[String, Seq[String]]))
          )
          val response        = await(declarationInfoService.getDeclarationInfo(JsString("Hello")).value)
          response.fold(_.message should include("Method not allowed"), _ => fail())
        }

        "500 response" in {
          val decInfoResponse = errorResponse("invalid JSON format")
          mockDeclarationConnector(JsString("Hello"))(
            Right(HttpResponse(500, decInfoResponse, Map.empty[String, Seq[String]]))
          )
          val response        = await(declarationInfoService.getDeclarationInfo(JsString("Hello")).value)
          response.fold(_.message should include("invalid JSON format"), _ => fail())
        }

        "Invalid Json response" in {
          mockDeclarationConnector(JsString("Hello"))(
            Right(HttpResponse(200, """{"a"-"b"}""", Map.empty[String, Seq[String]]))
          )
          val response = await(declarationInfoService.getDeclarationInfo(JsString("Hello")).value)
          response.fold(_.message should include("Unexpected character"), _ => fail())
        }

      }
    }
  }

}
