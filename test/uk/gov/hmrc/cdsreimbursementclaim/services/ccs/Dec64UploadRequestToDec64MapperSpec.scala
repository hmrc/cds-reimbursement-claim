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
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimSubmitResponse, Dec64UploadRequest}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Dec64UploadRequestGen.*
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TPI05RequestGen.*

@SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
class Dec64UploadRequestToDec64MapperSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  "Dec64UploadRequest to Dec64 Mapper" should {
    "map into expected DEC64 format" in forAll { (request: Dec64UploadRequest, response: ClaimSubmitResponse) =>
      val mapper       = dec64UploadRequestToDec64FilesMapper
      val dec64Request = mapper.map(request, response)

      dec64Request.map { payload =>

        payload should include("<ans2:name>CaseReference</ans2:name>")
        payload should include("<ans2:name>Eori</ans2:name>")
        payload should include("<ans2:name>DeclarationId</ans2:name>")
        payload should include("<ans2:name>DeclarationType</ans2:name>")
        payload should include("<ans2:name>ApplicationName</ans2:name>")
        payload should include("<ans2:name>DocumentType</ans2:name>")
        payload should include(s"<ans2:batchSize>${request.uploadedFiles.size}</ans2:batchSize>")
        payload should include(s"<ans2:value>${request.applicationName}</ans2:value>")

        if (request.applicationName === "Securities") {
          payload should include("<ans2:name>RFS</ans2:name>")
          payload should include(
            s"<ans2:value>${ReasonForSecurity.parseACC14Code(request.reasonForSecurity.get).get.dec64DisplayString}</ans2:value>"
          )
        }
      }
    }

  }
}
