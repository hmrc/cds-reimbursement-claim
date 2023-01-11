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
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.cdsreimbursementclaim.Fake
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi01.{ClaimsSelector, ErrorResponse, GetReimbursementClaimsResponse}
import uk.gov.hmrc.cdsreimbursementclaim.services.GetClaimsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Tpi01ReponseGen
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi01.ClaimsResponse
import uk.gov.hmrc.cdsreimbursementclaim.utils.ForSampledValueCheck

@SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext"))
class GetClaimsControllerSpec extends ControllerSpec with ScalaCheckPropertyChecks with ForSampledValueCheck {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 100)

  val mockGetClaimsService: GetClaimsService =
    mock[GetClaimsService]

  val testEori = Eori("ABC-123-XYZ")

  lazy val controller = new GetClaimsController(
    authorised = Fake.login(testEori),
    mockGetClaimsService,
    Helpers.stubControllerComponents()
  )

  def mockGetClaimsResponse(eori: Eori, claimsSelector: ClaimsSelector)(
    response: Either[ErrorResponse, GetReimbursementClaimsResponse]
  ) =
    (
      mockGetClaimsService
        .getClaims(_: Eori, _: ClaimsSelector)(_: HeaderCarrier)
      )
      .expects(eori, claimsSelector, *)
      .returning(Future.successful(response))

  "The GetClaimsController" should {
    "succeed" when {
      "handling non-empty responseDetails" in {
        forAll(Tpi01ReponseGen.genGetReimbursementClaimsResponseVariant) { response =>
          inSequence {
            mockGetClaimsResponse(testEori, ClaimsSelector.All)(Right(response))
          }

          val result = controller.getAllClaims(FakeRequest())
          status(result)        shouldBe OK
          contentAsJson(result) shouldBe Json.toJson(
            Json.obj(
              "claims" -> Json.toJson(
                ClaimsResponse.fromTpi01Response(
                  response.responseDetail.getOrElse(fail("misising responseDetail field"))
                )
              )
            )
          )
        }
      }
    }
    "fail" when {
      "handling empty responseDetails" in {
        forSampledValue(Tpi01ReponseGen.genGetReimbursementClaimsResponseEmpty) { response =>
          inSequence {
            mockGetClaimsResponse(testEori, ClaimsSelector.All)(Right(response))
          }

          val result = controller.getAllClaims(FakeRequest())
          status(result)        shouldBe BAD_REQUEST
          contentAsJson(result) shouldBe Json.toJson(response.responseCommon)
        }
      }
      "handling error response with status 403" in {
        forSampledValue(Tpi01ReponseGen.genErrorResponse(403)) { response =>
          inSequence {
            mockGetClaimsResponse(testEori, ClaimsSelector.All)(Left(response))
          }

          val result = controller.getAllClaims(FakeRequest())
          status(result)        shouldBe BAD_REQUEST
          contentAsJson(result) shouldBe Json.toJson(response.errorDetail)
        }
      }
      "handling error response with status 500" in {
        forSampledValue(Tpi01ReponseGen.genErrorResponse(500)) { response =>
          inSequence {
            mockGetClaimsResponse(testEori, ClaimsSelector.All)(Left(response))
          }

          val result = controller.getAllClaims(FakeRequest())
          status(result)        shouldBe SERVICE_UNAVAILABLE
          contentAsJson(result) shouldBe Json.toJson(response.errorDetail)
        }
      }
    }
  }
}
