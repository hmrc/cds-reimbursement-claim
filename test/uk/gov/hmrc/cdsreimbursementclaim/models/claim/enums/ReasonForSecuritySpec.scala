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

package uk.gov.hmrc.cdsreimbursementclaim.models.claim.enums

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ReasonForSecurityGen._

class ReasonForSecuritySpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  "Reason for Security validation" should {
    "has Query String Bindable" in forAll { reason: ReasonForSecurity =>
      val rfs = reason.toString
      val key = "reasonForSecurity"
      implicitly[QueryStringBindable[ReasonForSecurity]].bind(key, Map(key -> List(rfs))) shouldBe Some(Right(reason))
    }
  }
}