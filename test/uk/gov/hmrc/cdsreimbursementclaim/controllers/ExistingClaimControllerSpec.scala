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

import java.time.LocalDateTime
import cats.data.EitherT
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ReasonForSecurityGen._
import play.api.test.Helpers._
import uk.gov.hmrc.cdsreimbursementclaim.Fake
import uk.gov.hmrc.cdsreimbursementclaim.connectors.ExistingDeclarationConnector
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ExistingClaim
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class DuplicateClaimControllerSpec extends ControllerSpec with ScalaCheckPropertyChecks {

  val mockExistingDeclarationConnector: ExistingDeclarationConnector = mock[ExistingDeclarationConnector]
  implicit val headerCarrier: HeaderCarrier                          = HeaderCarrier()

  val controller = new ExistingClaimController(
    authenticate = Fake.login(Fake.user, LocalDateTime.of(2020, 1, 1, 15, 47, 20)),
    mockExistingDeclarationConnector,
    Helpers.stubControllerComponents()
  )

  "Existing Claim Controller" when {
    "handling a request to get existing claims" must {
      "return 200 OK with a declaration JSON payload for a successful TPI-04 call" in forAll {
        (mrn: MRN, reason: ReasonForSecurity, existingClaim: Boolean) =>
          val response = ExistingClaim(existingClaim)

          (mockExistingDeclarationConnector
            .checkExistingDeclaration(_: MRN, _: ReasonForSecurity)(_: HeaderCarrier))
            .expects(mrn, reason, *)
            .returning(EitherT.fromEither[Future](Right(response)))

          val result = controller.claimExists(mrn, reason)(FakeRequest())

          status(result)        shouldBe OK
          contentAsJson(result) shouldBe Json.toJson(response)
      }

      "return 500 when the TPI-04 call fails or is unsuccessful" in forAll { (mrn: MRN, reason: ReasonForSecurity) =>
        (mockExistingDeclarationConnector
          .checkExistingDeclaration(_: MRN, _: ReasonForSecurity)(_: HeaderCarrier))
          .expects(mrn, reason, *)
          .returning(EitherT.fromEither[Future](Left(Error("error while getting declaration"))))

        val result = controller.claimExists(mrn, reason)(FakeRequest())

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
