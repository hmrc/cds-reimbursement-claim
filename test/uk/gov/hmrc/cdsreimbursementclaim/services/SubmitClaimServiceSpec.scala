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
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.await
import uk.gov.hmrc.cdsreimbursementclaim.connectors.SubmitClaimConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import play.api.test.Helpers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class SubmitClaimServiceSpec extends AnyWordSpec with Matchers with MockFactory {

  val submitClaimConnector = mock[SubmitClaimConnector]

  val submitClaimService =
    new SubmitClaimServiceImpl(
      submitClaimConnector
    )

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val request: Request[_] = FakeRequest()

  //TODO: take this comment out - we setup a mock for the http call
  def mockSubmitClaim(submitClaimData: JsValue)(
    response: Either[Error, HttpResponse]
  ) =
    (submitClaimConnector
      .submitClaim(_: JsValue)(_: HeaderCarrier))
      .expects(submitClaimData, hc)
      .returning(EitherT.fromEither[Future](response))

  "Submit Claim Service" when {

    "handling submit claim returns" should {

      "handle successful submits" when {

        "there is a valid payload" in {

          val responseJsonBody =
            Json.parse(s"""
                          |{
                          | //TODO: add some valid payload structure here
                          |}
                          |""".stripMargin)

          mockSubmitClaim(
            Json.parse("{}")
          )(Right(HttpResponse(200, responseJsonBody, Map.empty[String, Seq[String]])))

          await(submitClaimService.submitClaim(Json.parse("{}")).value) shouldBe Right(
            "{}"
          ) //TODO: change this to what you need ie the actual response
        }
      }
    }
  }
}
