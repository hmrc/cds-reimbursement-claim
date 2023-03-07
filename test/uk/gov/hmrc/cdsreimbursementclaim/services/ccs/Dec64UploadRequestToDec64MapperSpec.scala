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

package uk.gov.hmrc.cdsreimbursementclaim.services.ccs

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ClaimSubmitResponse
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.Dec64UploadRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Dec64UploadRequestGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TPI05RequestGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity

@SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
class Dec64UploadRequestToDec64MapperSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  "Dec64UploadRequest to Dec64 Mapper" should {
    "map into expected DEC64 format" in forAll { (request: Dec64UploadRequest, response: ClaimSubmitResponse) =>
      val mapper       = dec64UploadRequestToDec64FilesMapper
      val dec64Request = mapper.map(request, response)

      dec64Request.map { envelope =>
        envelope.Body.BatchFileInterfaceMetadata.batchSize shouldBe request.uploadedFiles.size
        envelope.Body.BatchFileInterfaceMetadata.properties.property
          .map(p => p.name)                                  should contain.allOf(
          "CaseReference",
          "Eori",
          "DeclarationId",
          "DeclarationType",
          "ApplicationName",
          "DocumentType"
        )
        if (request.applicationName === "Securities") {
          envelope.Body.BatchFileInterfaceMetadata.properties.property
            .find(_.name === "RFS")
            .getOrElse(fail("missing reason for security"))
            .value shouldBe ReasonForSecurity.parseACC14Code(request.reasonForSecurity.get).get.dec64DisplayString
        }
        envelope.Body.BatchFileInterfaceMetadata.properties.property
          .map(p => (p.name, p.value))                       should contain(
          "ApplicationName" -> request.applicationName
        )
      }
    }

  }
}
