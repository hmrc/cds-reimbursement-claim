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

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.cdsreimbursementclaim.Fake
import uk.gov.hmrc.cdsreimbursementclaim.models.CDFPayService
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi02.{ErrorResponse, GetSpecificCaseResponse}
import uk.gov.hmrc.cdsreimbursementclaim.services.GetSpecificClaimService
import uk.gov.hmrc.cdsreimbursementclaim.utils.ForSampledValueCheck
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Tpi02ReponseGen
import play.api.libs.json.Json
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi02.SpecificClaimResponse

@SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext"))
class GetSpecificClaimControllerSpec extends ControllerSpec with ScalaCheckPropertyChecks with ForSampledValueCheck {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 100)

  val mockGetSpecificClaimService: GetSpecificClaimService =
    mock[GetSpecificClaimService]

  val testEori = Eori("ABC-123-XYZ")

  lazy val controller = new GetSpecificClaimController(
    authorised = Fake.login(testEori),
    mockGetSpecificClaimService,
    Helpers.stubControllerComponents()
  )

  def mockGetClaimsResponse(cdfPayService: CDFPayService, cdfPayCaseNumber: String)(
    response: Either[ErrorResponse, GetSpecificCaseResponse]
  ) =
    (
      mockGetSpecificClaimService
        .getSpecificClaim(_: CDFPayService, _: String)(_: HeaderCarrier)
      )
      .expects(cdfPayService, cdfPayCaseNumber, *)
      .returning(Future.successful(response))

  def mockGetClaimsResponseThrowsException(cdfPayService: CDFPayService, cdfPayCaseNumber: String) =
    (
      mockGetSpecificClaimService
        .getSpecificClaim(_: CDFPayService, _: String)(_: HeaderCarrier)
      )
      .expects(cdfPayService, cdfPayCaseNumber, *)
      .throws(new RuntimeException(""))

  "The GetSpecificClaimController" should {
    "succeed" when {
      "handling non-empty responseDetails with NDRC cases" in {
        forAll(Tpi02ReponseGen.getGetSpecificCaseResponseNdrc) { response =>
          inSequence {
            mockGetClaimsResponse(CDFPayService.NDRC, "XYZ-999")(Right(response))
          }

          val result = controller.getSpecificClaim(CDFPayService.NDRC, "XYZ-999")(FakeRequest())
          status(result)        shouldBe OK
          contentAsJson(result) shouldBe Json.toJson(
            SpecificClaimResponse.fromTpi02Response(
              response.responseDetail.getOrElse(fail("misising responseDetail field"))
            )
          )
        }
      }
      "handling non-empty responseDetails with SCTY cases" in {
        forAll(Tpi02ReponseGen.getGetSpecificCaseResponseScty) { response =>
          inSequence {
            mockGetClaimsResponse(CDFPayService.SCTY, "XYZ-999")(Right(response))
          }

          val result = controller.getSpecificClaim(CDFPayService.SCTY, "XYZ-999")(FakeRequest())
          status(result)        shouldBe OK
          contentAsJson(result) shouldBe Json.toJson(
            SpecificClaimResponse.fromTpi02Response(
              response.responseDetail.getOrElse(fail("misising responseDetail field"))
            )
          )
        }
      }
    }
    "fail" when {
      "handling empty responseDetails" in {
        forSampledValue(Tpi02ReponseGen.getGetSpecificCaseResponseEmpty) { response =>
          inSequence {
            mockGetClaimsResponse(CDFPayService.NDRC, "XYZ-999")(Right(response))
          }

          val result = controller.getSpecificClaim(CDFPayService.NDRC, "XYZ-999")(FakeRequest())
          status(result)        shouldBe BAD_REQUEST
          contentAsJson(result) shouldBe Json.toJson(response.responseCommon)
        }
      }
      "handling error response with status 403" in {
        forSampledValue(Tpi02ReponseGen.genErrorResponse(403)) { response =>
          inSequence {
            mockGetClaimsResponse(CDFPayService.NDRC, "XYZ-999")(Left(response))
          }

          val result = controller.getSpecificClaim(CDFPayService.NDRC, "XYZ-999")(FakeRequest())
          status(result)        shouldBe BAD_REQUEST
          contentAsJson(result) shouldBe Json.toJson(response.errorDetail)
        }
      }
      "handling error response with status 500" in {
        forSampledValue(Tpi02ReponseGen.genErrorResponse(500)) { response =>
          inSequence {
            mockGetClaimsResponse(CDFPayService.NDRC, "XYZ-999")(Left(response))
          }

          val result = controller.getSpecificClaim(CDFPayService.NDRC, "XYZ-999")(FakeRequest())
          status(result)        shouldBe SERVICE_UNAVAILABLE
          contentAsJson(result) shouldBe Json.toJson(response.errorDetail)
        }
      }
    }
  }
}
