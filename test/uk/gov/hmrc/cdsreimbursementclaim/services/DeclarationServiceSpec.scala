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

import java.time.{Duration, LocalDateTime, ZoneId}

import cats.data.EitherT
import org.scalamock.matchers.ArgCapture.CaptureOne
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.cdsreimbursementclaim.connectors.DefaultDeclarationConnector
import uk.gov.hmrc.cdsreimbursementclaim.models.Generators._
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error, GetDeclarationResponse, MRN, OverpaymentDeclarationDisplayResponse, ResponseDetail}
import uk.gov.hmrc.cdsreimbursementclaim.utils.TimeUtils
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeclarationServiceSpec extends AnyWordSpec with Matchers with MockFactory {

  val declarationInfoConnector = mock[DefaultDeclarationConnector]

  val declarationInfoService = new DeclarationServiceImpl(declarationInfoConnector)

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

  def mockDeclarationConnector(response: Either[Error, HttpResponse]) =
    (declarationInfoConnector
      .getDeclarationInfo(_: JsValue)(_: HeaderCarrier))
      .expects(*, *)
      .returning(EitherT.fromEither[Future](response))
      .atLeastOnce()

  val emptyHeaders = Map.empty[String, Seq[String]]

  "Declaration Information Request Service" when {
    "handling a request returns" should {
      "handle successful submits" when {
        "there is a valid payload" in {
          val responseDetail                        = sample[ResponseDetail]
          val overpaymentDeclarationDisplayResponse =
            sample[OverpaymentDeclarationDisplayResponse].copy(responseDetail = Some(responseDetail))
          val decResponse: GetDeclarationResponse   = sample[GetDeclarationResponse]
            .copy(overpaymentDeclarationDisplayResponse = overpaymentDeclarationDisplayResponse)
          val declarationId                         =
            decResponse.overpaymentDeclarationDisplayResponse.responseDetail.map(_.declarationId).getOrElse(fail)
          val httpResponse                          = Right(HttpResponse(200, Json.toJson(decResponse), emptyHeaders))

          val requestBody = CaptureOne[JsValue]()
          (declarationInfoConnector
            .getDeclarationInfo(_: JsValue)(_: HeaderCarrier))
            .expects(capture(requestBody), *)
            .returning(EitherT.fromEither[Future](httpResponse))
            .atLeastOnce()

          val declaration = await(declarationInfoService.getDeclaration(declarationId).value)
          declaration
            .getOrElse(fail)
            .overpaymentDeclarationDisplayResponse
            .responseDetail
            .getOrElse(fail)
            .declarationId shouldBe declarationId
          val responseInternal = requestBody.value \ "overpaymentDeclarationDisplayRequest"
          (responseInternal \ "requestCommon" \ "originatingSystem").get.as[String] shouldBe "MDTP"
          val requestDate = LocalDateTime.parse(
            (responseInternal \ "requestCommon" \ "receiptDate").get.as[String],
            TimeUtils.eisDateFormat
          )
          Duration.between(LocalDateTime.now(ZoneId.systemDefault()), requestDate).getSeconds.toInt should be < 5
          (responseInternal \ "requestCommon" \ "acknowledgementReference").isDefined             shouldBe true
          (responseInternal \ "requestDetail" \ "declarationId").get.as[String]                   shouldBe declarationId.value
        }
      }

      "handle unsuccesful submits" when {
        "400 response" in {
          val mrn             = MRN.parse("21GBIDMSXBLNR06016").getOrElse(fail)
          val decInfoResponse = errorResponse("Invalid Request")
          mockDeclarationConnector(Right(HttpResponse(400, decInfoResponse, Map.empty[String, Seq[String]])))
          val response        = await(declarationInfoService.getDeclaration(mrn).value)
          response.fold(_.message should include("Invalid Request"), _ => fail())
        }

        "401 response" in {
          val mrn             = MRN.parse("21GBIDMSXBLNR06016").getOrElse(fail)
          val decInfoResponse = errorResponse("Unauthorized")
          mockDeclarationConnector(
            Right(HttpResponse(404, decInfoResponse, Map.empty[String, Seq[String]]))
          )
          val response        = await(declarationInfoService.getDeclaration(mrn).value)
          response.fold(_.message should include("Unauthorized"), _ => fail())
        }

        "403 response" in {
          val mrn             = MRN.parse("21GBIDMSXBLNR06016").getOrElse(fail)
          val decInfoResponse = errorResponse("WAF Forbidden")
          mockDeclarationConnector(
            Right(HttpResponse(404, decInfoResponse, Map.empty[String, Seq[String]]))
          )
          val response        = await(declarationInfoService.getDeclaration(mrn).value)
          response.fold(_.message should include("WAF Forbidden"), _ => fail())
        }

        "405 response" in {
          val mrn             = MRN.parse("21GBIDMSXBLNR06016").getOrElse(fail)
          val decInfoResponse = errorResponse("Method not allowed")
          mockDeclarationConnector(
            Right(HttpResponse(405, decInfoResponse, Map.empty[String, Seq[String]]))
          )
          val response        = await(declarationInfoService.getDeclaration(mrn).value)
          response.fold(_.message should include("Method not allowed"), _ => fail())
        }

        "500 response" in {
          val mrn             = MRN.parse("21GBIDMSXBLNR06016").getOrElse(fail)
          val decInfoResponse = errorResponse("invalid JSON format")
          mockDeclarationConnector(
            Right(HttpResponse(500, decInfoResponse, Map.empty[String, Seq[String]]))
          )
          val response        = await(declarationInfoService.getDeclaration(mrn).value)
          response.fold(_.message should include("invalid JSON format"), _ => fail())
        }

        "Invalid Json response" in {
          val mrn      = MRN.parse("21GBIDMSXBLNR06016").getOrElse(fail)
          mockDeclarationConnector(
            Right(HttpResponse(200, """{"a"-"b"}""", Map.empty[String, Seq[String]]))
          )
          val response = await(declarationInfoService.getDeclaration(mrn).value)
          response.fold(_.message should include("Unexpected character"), _ => fail())
        }

      }
    }
  }

}