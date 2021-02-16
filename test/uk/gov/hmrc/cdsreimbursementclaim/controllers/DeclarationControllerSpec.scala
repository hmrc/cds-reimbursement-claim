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

import akka.stream.Materializer
import cats.data.EitherT
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.cdsreimbursementclaim.Fake
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.AuthenticatedRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.GenerateDeclaration._
import uk.gov.hmrc.cdsreimbursementclaim.models.declaration.Declaration
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error, MRN}
import uk.gov.hmrc.cdsreimbursementclaim.services.DeclarationService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeclarationControllerSpec extends ControllerSpec {

  val declarationService              = mock[DeclarationService]
  implicit val headerCarrier          = HeaderCarrier()
  implicit lazy val mat: Materializer = fakeApplication.materializer

  val request = new AuthenticatedRequest(
    Fake.user,
    LocalDateTime.now(),
    headerCarrier,
    FakeRequest()
  )

  val controller = new DeclarationController(
    authenticate = Fake.login(Fake.user, LocalDateTime.of(2020, 1, 1, 15, 47, 20)),
    declarationService,
    Helpers.stubControllerComponents()
  )

  def mockDeclarationService(mrn: MRN)(response: Either[Error, Declaration]) =
    (declarationService
      .getDeclaration(_: MRN)(_: HeaderCarrier))
      .expects(mrn, *)
      .returning(EitherT.fromEither[Future](response))

  "Declaration Controller" when {

    "handling request to get a declaration" must {

      "return 200 OK for a successful call" in {

        val mrn                  = sample[MRN]
        val expectedResponseBody = sample[Declaration]

        inSequence {
          mockDeclarationService(mrn)(Right(expectedResponseBody))
        }

        val result = controller.declaration(mrn)(request)
        status(result)        shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(expectedResponseBody)
      }

      "return 500 when eis call fails" in {
        val mrn    = sample[MRN]
        mockDeclarationService(mrn)(Left(Error.apply("error while getting declaration")))
        val result = controller.declaration(mrn)(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
