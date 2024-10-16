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
import play.api.libs.json.{JsNull, JsString, Json}
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.cdsreimbursementclaim.Fake
import uk.gov.hmrc.cdsreimbursementclaim.connectors.SubscriptionConnector
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Sub09ReponseGen
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori
import uk.gov.hmrc.cdsreimbursementclaim.models.sub09.SubscriptionResponse
import uk.gov.hmrc.cdsreimbursementclaim.utils.ForSampledValueCheck
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext"))
class GetEoriDetailsControllerSpec extends ControllerSpec with ScalaCheckPropertyChecks with ForSampledValueCheck {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 100)

  val mockSubscriptionConnector: SubscriptionConnector =
    mock[SubscriptionConnector]

  val testEori = Eori("ABC-123-XYZ")

  lazy val controller = new GetEoriDetailsController(
    authorised = Fake.login(testEori),
    mockSubscriptionConnector,
    Helpers.stubControllerComponents()
  )

  def mockGetClaimsResponse(eori: Eori)(
    response: Option[SubscriptionResponse]
  ) =
    (
      mockSubscriptionConnector
        .getSubscription(_: Eori)(_: HeaderCarrier)
      )
      .expects(eori, *)
      .returning(Future.successful(Right(response)))

  def mockFailedResponse(eori: Eori)(errorMessage: String) =
    (
      mockSubscriptionConnector
        .getSubscription(_: Eori)(_: HeaderCarrier)
      )
      .expects(eori, *)
      .returning(Future.failed(new Exception(errorMessage)))

  "The GetEoriDetailsController" should {
    "succeed" when {
      "handling non-empty response with XI EORI" in {
        forAll(Sub09ReponseGen.genSubscriptionWithXiEori) { case (response, eoriGB, eoriXI) =>
          inSequence {
            mockGetClaimsResponse(testEori)(Some(response))
          }

          val result = controller.getEoriDetails(FakeRequest())
          status(result)        shouldBe OK
          contentAsJson(result) shouldBe Json.toJson(
            Json.obj(
              "eoriGB"      -> JsString(eoriGB.value),
              "eoriXI"      -> JsString(eoriXI.value),
              "fullName"    -> JsString("Tony Stark"),
              "eoriEndDate" -> JsString("2020-01-01")
            )
          )
        }
      }

      "handling non-empty response without XI EORI" in {
        forAll(Sub09ReponseGen.genSubscriptionWithoutXiEori) { case (response, eoriGB) =>
          inSequence {
            mockGetClaimsResponse(testEori)(Some(response))
          }

          val result = controller.getEoriDetails(FakeRequest())
          status(result)        shouldBe OK
          contentAsJson(result) shouldBe Json.toJson(
            Json.obj(
              "eoriGB"      -> JsString(eoriGB.value),
              "eoriXI"      -> JsNull,
              "fullName"    -> JsString("Tony Stark"),
              "eoriEndDate" -> JsString("2020-01-01")
            )
          )
        }
      }

      "handling empty response" in {
        forAll(Sub09ReponseGen.genSubscriptionWithoutXiEori) { case (response, eoriGB) =>
          inSequence {
            mockGetClaimsResponse(testEori)(None)
          }

          val result = controller.getEoriDetails(FakeRequest())
          status(result) shouldBe NO_CONTENT
        }
      }
    }

    "fail" when {
      "handling error response with status 500" in {
        inSequence {
          mockFailedResponse(testEori)("Sample error message")
        }

        val result = controller.getEoriDetails(FakeRequest())
        status(result) shouldBe NO_CONTENT
      }
    }
  }
}
