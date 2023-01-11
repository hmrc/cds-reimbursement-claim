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

import cats.data.EitherT
import org.scalamock.handlers.CallHandler3
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.cdsreimbursementclaim.Fake
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.AuthenticatedUserRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.GetDeclarationError
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ReasonForSecurityGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Acc14DeclarationGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.services.DeclarationService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext"))
class DeclarationControllerSpec extends ControllerSpec {

  val mockDeclarationService: DeclarationService = mock[DeclarationService]
  implicit val headerCarrier: HeaderCarrier      = HeaderCarrier()

  val request = new AuthenticatedUserRequest(
    Fake.user,
    LocalDateTime.now(),
    headerCarrier,
    FakeRequest()
  )

  val controller = new DeclarationController(
    authenticate = Fake.login(Fake.user, LocalDateTime.of(2020, 1, 1, 15, 47, 20)),
    mockDeclarationService,
    Helpers.stubControllerComponents()
  )

  def mockDeclarationService(mrn: MRN, reasonForSecurity: Option[String] = None)(
    response: Either[Error, Option[DisplayDeclaration]]
  ): CallHandler3[MRN, Option[String], HeaderCarrier, EitherT[Future, Error, Option[DisplayDeclaration]]] =
    (mockDeclarationService
      .getDeclaration(_: MRN, _: Option[String])(_: HeaderCarrier))
      .expects(mrn, reasonForSecurity, *)
      .returning(EitherT.fromEither[Future](response))

  def mockDeclarationServiceWithErrorCodes(mrn: MRN, reasonForSecurity: Option[String] = None)(
    response: Either[GetDeclarationError, DisplayDeclaration]
  ): CallHandler3[MRN, Option[String], HeaderCarrier, EitherT[Future, GetDeclarationError, DisplayDeclaration]] =
    (mockDeclarationService
      .getDeclarationWithErrorCodes(_: MRN, _: Option[String])(_: HeaderCarrier))
      .expects(mrn, reasonForSecurity, *)
      .returning(EitherT.fromEither[Future](response))

  "Declaration Controller" when {

    "handling a request to get a declaration" must {

      "return 200 OK with a declaration JSON payload for a successful ACC-14 call" in {
        val mrn                  = sample[MRN]
        val expectedResponseBody = sample[DisplayDeclaration]

        mockDeclarationService(mrn)(Right(Some(expectedResponseBody)))

        val result = controller.declaration(mrn)(request)
        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(expectedResponseBody)
      }

      "return 204 NO CONTENT if no declaration information comes back from a successful call to ACC-14 call" in {
        val mrn = sample[MRN]

        mockDeclarationService(mrn)(Right(None))

        val result = controller.declaration(mrn)(request)
        status(result) shouldBe NO_CONTENT
      }

      "return 500 when the ACC-14 call fails or is unsuccessful" in {
        val mrn    = sample[MRN]
        mockDeclarationService(mrn)(Left(Error("error while getting declaration")))
        val result = controller.declaration(mrn)(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "return 200 OK with a declaration JSON payload for a successful ACC-14 call for reasonForSecurity" in {
        val mrn                  = sample[MRN]
        val reasonForSecurity    = sample[ReasonForSecurity]
        val expectedResponseBody =
          genDisplayDeclarationWithSecurityReason(Some(reasonForSecurity.acc14Code), Some(mrn)).sample

        mockDeclarationServiceWithErrorCodes(mrn, Some(reasonForSecurity.acc14Code))(Right(expectedResponseBody.orNull))

        val result = controller.declarationWithReasonForSecurity(mrn, reasonForSecurity)(request)
        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(expectedResponseBody)
        contentAsJson(result)
          .validate[DisplayDeclaration]
          .get
          .displayResponseDetail
          .securityReason     shouldBe Some(reasonForSecurity.acc14Code)
      }

      "return 400 BAD REQUEST with mismatchMrn a declaration JSON payload for a successful ACC-14 call for reasonForSecurity" in {
        val mrn                  = sample[MRN]
        val reasonForSecurity    = sample[ReasonForSecurity]
        val expectedResponseBody = genDisplayDeclarationWithSecurityReason(Some(reasonForSecurity.acc14Code)).sample

        mockDeclarationServiceWithErrorCodes(mrn, Some(reasonForSecurity.acc14Code))(Right(expectedResponseBody.orNull))

        val result = controller.declarationWithReasonForSecurity(mrn, reasonForSecurity)(request)
        status(result)        shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(GetDeclarationError.mismatchMrn)
      }

      "return 500 INTERNAL_SERVER_ERROR if no declaration information or error codes 072 or 086 comes back to ACC-14 with reasonForSecurity" in {
        val mrn               = sample[MRN]
        val reasonForSecurity = sample[ReasonForSecurity]

        mockDeclarationServiceWithErrorCodes(mrn, Some(reasonForSecurity.acc14Code))(
          Left(GetDeclarationError.unexpectedError)
        )

        val result = controller.declarationWithReasonForSecurity(mrn, reasonForSecurity)(request)
        status(result)        shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe Json.toJson(GetDeclarationError.unexpectedError)
      }

      "return 500 when the ACC-14 call fails or is unsuccessful for a call with reasonForSecurity" in {
        val mrn               = sample[MRN]
        val reasonForSecurity = sample[ReasonForSecurity]

        mockDeclarationServiceWithErrorCodes(mrn, Some(reasonForSecurity.acc14Code))(
          Left(GetDeclarationError.unexpectedError)
        )
        val result = controller.declarationWithReasonForSecurity(mrn, reasonForSecurity)(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }
  }
}
