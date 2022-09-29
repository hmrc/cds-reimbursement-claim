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

package uk.gov.hmrc.cdsreimbursementclaim.services.ccs

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ClaimSubmitResponse
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.SecuritiesClaimRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.SecuritiesClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TPI05RequestGen._

class SecuritiesClaimToDec64MapperSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  "Securities Claim to Dec64 Mapper" should {
    "obtain the correct batch size" in forAll { (request: SecuritiesClaimRequest, response: ClaimSubmitResponse) =>
      val mapper       = securitiesClaimToDec64FilesMapper
      val dec64Request = mapper.map(request, response)

      dec64Request.map { envelope =>
        envelope.Body.BatchFileInterfaceMetadata.batchSize shouldBe request.claim.supportingEvidences.size
        envelope.Body.BatchFileInterfaceMetadata.properties.property
          .map(p => p.name)                                  should contain.allOf(
          "CaseReference",
          "Eori",
          "DeclarationId",
          "DeclarationType",
          "RFS",
          "ApplicationName",
          "DocumentType"
        )
        envelope.Body.BatchFileInterfaceMetadata.properties.property
          .map(p => (p.name, p.value))                       should contain(
          "ApplicationName" -> "Securities"
        )
      }
    }

  }
}
