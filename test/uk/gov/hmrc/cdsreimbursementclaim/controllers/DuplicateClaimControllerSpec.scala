/*
 * Copyright 2022 HM Revenue & Customs
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

import cats.implicits.catsSyntaxEq
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, status}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ReasonForSecurityGen._
import play.api.test.Helpers._
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ExistingClaim

class DuplicateClaimControllerSpec extends ControllerSpec with ScalaCheckPropertyChecks {

  val controller: DuplicateClaimController = instanceOf[DuplicateClaimController]

  "Duplicate Claim Controller" when {
    "handling a request to get duplicate claims" must {
      "return 200 OK with a declaration JSON payload for a successful TPI-04 call" in forAll {
        (mrn: MRN, reason: ReasonForSecurity) =>
          whenever(reason =!= ReasonForSecurity.AccountSales) { // Temporary restriction, this will be replaced in CDSR-1783
            val response = ExistingClaim(false, None, None)
            val result   = controller.isDuplicate(mrn, reason)(FakeRequest())

            status(result)        shouldBe OK
            contentAsJson(result) shouldBe Json.toJson(response)
          }
      }
    }
  }
}
