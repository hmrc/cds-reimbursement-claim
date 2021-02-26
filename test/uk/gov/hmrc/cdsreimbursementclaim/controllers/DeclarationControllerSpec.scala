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
import org.scalamock.handlers.CallHandler2
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.cdsreimbursementclaim.Fake
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.AuthenticatedRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.DeclarationGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.services.DeclarationService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeclarationControllerSpec extends ControllerSpec {

  val mockDeclarationService: DeclarationService = mock[DeclarationService]
  implicit val headerCarrier: HeaderCarrier      = HeaderCarrier()

  val request = new AuthenticatedRequest(
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

  def mockDeclarationService(mrn: MRN)(
    response: Either[Error, Option[DisplayDeclaration]]
  ): CallHandler2[MRN, HeaderCarrier, EitherT[Future, Error, Option[DisplayDeclaration]]] =
    (mockDeclarationService
      .getDeclaration(_: MRN)(_: HeaderCarrier))
      .expects(mrn, *)
      .returning(EitherT.fromEither[Future](response))

  "Declaration Controller" when {

    "handling request to get a declaration" must {

      "return 200 OK with declaration JSON payload for a successful ACC-14 call" in {

        val mrn                  = sample[MRN]
        val expectedResponseBody = sample[DisplayDeclaration]

        mockDeclarationService(mrn)(Right(Some(expectedResponseBody)))

        val result = controller.declaration(mrn)(request)
        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(expectedResponseBody)
      }

      "return 500 when the ACC-14 call fails or is unsuccessful" in {
        val mrn    = sample[MRN]
        mockDeclarationService(mrn)(Left(Error("error while getting declaration")))
        val result = controller.declaration(mrn)(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
