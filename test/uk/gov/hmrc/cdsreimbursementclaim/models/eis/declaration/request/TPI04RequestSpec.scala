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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.request

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ReasonForSecurityGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Acc14DeclarationGen.arbitraryRequestCommon

class TPI04RequestSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  "TPI04 Request" should {
    "serialise properly" in forAll { (mrn: MRN, reasonForSecurity: ReasonForSecurity, requestCommon: RequestCommon) =>
      val tpi04Request = TPI04Request(requestCommon, mrn, reasonForSecurity)
      Json.toJson(tpi04Request) shouldBe Json.obj(
        "getExistingClaimRequest" -> Json.obj(
          "requestCommon" -> Json.obj(
            "originatingSystem"        -> "MDTP",
            "receiptDate"              -> requestCommon.receiptDate,
            "acknowledgementReference" -> requestCommon.acknowledgementReference
          ),
          "requestDetail" -> Json.obj(
            "CDFPayService"     -> "SCTY",
            "declarationID"     -> mrn,
            "reasonForSecurity" -> reasonForSecurity.acc14Code
          )
        )
      )
    }
  }
}
