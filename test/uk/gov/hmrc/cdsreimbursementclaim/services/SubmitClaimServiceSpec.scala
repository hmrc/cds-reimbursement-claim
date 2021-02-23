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
import uk.gov.hmrc.cdsreimbursementclaim.connectors.SubmitClaimConnector
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.cdsreimbursementclaim.models.GenerateSubmitClaim.{sample, _}
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi05.response.SubmitClaimResponse
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi05.request.SubmitClaimRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmitClaimServiceSpec extends AnyWordSpec with Matchers with MockFactory {

  val submitClaimConnector = mock[SubmitClaimConnector]

  val submitClaimService = new SubmitClaimServiceImpl(submitClaimConnector)

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val submitClaimRequest         = sample[SubmitClaimRequest]
  val submitClaimRequestJson     = Json.toJson(submitClaimRequest)

  implicit val request: Request[_] = FakeRequest()

  def errorResponse(errorMessage: String): JsValue = Json.parse(s"""{
      |   "ErrorDetails":{
      |      "ProcessingDateTime":"2016-10-10T13:52:16Z",
      |      "CorrelationId":"d60de98c-f499-47f5-b2d6-e80966e8d19e",
      |      "ErrorMessage":"$errorMessage"
      |    }
      |}""".stripMargin)

  def mockSubmitClaim(submitClaimData: JsValue)(response: Either[Error, HttpResponse]) =
    (submitClaimConnector
      .submitClaim(_: JsValue)(_: HeaderCarrier))
      .expects(submitClaimData, *)
      .returning(EitherT.fromEither[Future](response))
      .atLeastOnce()

  "Submit Claim Service" when {
    "handling submit claim returns" should {
      "handle successful submits" when {
        "there is a valid payload" in {
          val okResponse = sample[SubmitClaimResponse]
          mockSubmitClaim(submitClaimRequestJson)(
            Right(HttpResponse(200, Json.toJson(okResponse), Map.empty[String, Seq[String]]))
          )
          await(submitClaimService.submitClaim(submitClaimRequest).value) shouldBe Right(okResponse)
        }
      }

      "handle unsuccesful submits" when {
        "400 response" in {
          val eisResponse = errorResponse("Invalid ClaimType")
          mockSubmitClaim(submitClaimRequestJson)(Right(HttpResponse(400, eisResponse, Map.empty[String, Seq[String]])))
          val response    = await(submitClaimService.submitClaim(submitClaimRequest).value)
          response.fold(_.message should include("Invalid ClaimType"), _ => fail())
        }

        "404 response" in {
          val eisResponse = errorResponse("Resource is unavailable")
          mockSubmitClaim(submitClaimRequestJson)(Right(HttpResponse(404, eisResponse, Map.empty[String, Seq[String]])))
          val response    = await(submitClaimService.submitClaim(submitClaimRequest).value)
          response.fold(_.message should include("Resource is unavailable"), _ => fail())
        }

        "405 response" in {
          val eisResponse = errorResponse("Method not allowed")
          mockSubmitClaim(submitClaimRequestJson)(Right(HttpResponse(405, eisResponse, Map.empty[String, Seq[String]])))
          val response    = await(submitClaimService.submitClaim(submitClaimRequest).value)
          response.fold(_.message should include("Method not allowed"), _ => fail())
        }

        "500 response" in {
          val eisResponse = errorResponse("invalid JSON format")
          mockSubmitClaim(submitClaimRequestJson)(Right(HttpResponse(500, eisResponse, Map.empty[String, Seq[String]])))
          val response    = await(submitClaimService.submitClaim(submitClaimRequest).value)
          response.fold(_.message should include("invalid JSON format"), _ => fail())
        }

        "Invalid Json response" in {
          mockSubmitClaim(submitClaimRequestJson)(
            Right(HttpResponse(200, """{"a"-"b"}""", Map.empty[String, Seq[String]]))
          )
          val response = await(submitClaimService.submitClaim(submitClaimRequest).value)
          response.fold(_.message should include("Unexpected character"), _ => fail())
        }

      }
    }
  }

}
