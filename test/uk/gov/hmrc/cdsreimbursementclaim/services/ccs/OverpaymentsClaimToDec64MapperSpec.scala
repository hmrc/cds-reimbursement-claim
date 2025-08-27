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
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimSubmitResponse, MultipleOverpaymentsClaim, MultipleOverpaymentsClaimRequest, ScheduledOverpaymentsClaim, ScheduledOverpaymentsClaimRequest, SingleOverpaymentsClaim, SingleOverpaymentsClaimRequest}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.OverpaymentsClaimGen.*
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TPI05RequestGen.*

class OverpaymentsClaimToDec64MapperSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  "Overpayments Claim to Dec64 Mapper" should {
    "obtain the correct batch size for a Single Journey" in forAll {
      (journey: SingleOverpaymentsClaim, response: ClaimSubmitResponse) =>
        val mapper       = singleOverpaymentsClaimToDec64FilesMapper
        val dec64Request = mapper.map(SingleOverpaymentsClaimRequest(journey), response)

        dec64Request.map { payload =>
          payload should include("<ans2:name>CaseReference</ans2:name>")
          payload should include("<ans2:name>Eori</ans2:name>")
          payload should include("<ans2:name>DeclarationId</ans2:name>")
          payload should include("<ans2:name>DeclarationType</ans2:name>")
          payload should include("<ans2:name>ApplicationName</ans2:name>")
          payload should include("<ans2:name>DocumentType</ans2:name>")
          payload should include(s"<ans2:batchSize>${journey.supportingEvidences.size}</ans2:batchSize>")
          payload should include("<ans2:value>NDRC</ans2:value>")
        }
    }

    "obtain the correct batch size for a Multiple Journey" in forAll {
      (journey: MultipleOverpaymentsClaim, response: ClaimSubmitResponse) =>
        val mapper       = multipleOverpaymentsClaimToDec64FilesMapper
        val dec64Request = mapper.map(MultipleOverpaymentsClaimRequest(journey), response)

        dec64Request.map { payload =>
          payload should include("<ans2:name>CaseReference</ans2:name>")
          payload should include("<ans2:name>Eori</ans2:name>")
          payload should include("<ans2:name>DeclarationId</ans2:name>")
          payload should include("<ans2:name>DeclarationType</ans2:name>")
          payload should include("<ans2:name>ApplicationName</ans2:name>")
          payload should include("<ans2:name>DocumentType</ans2:name>")
          payload should include(s"<ans2:batchSize>${journey.supportingEvidences.size}</ans2:batchSize>")
          payload should include("<ans2:value>NDRC</ans2:value>")
        }
    }

    "obtain the correct batch size for a Scheduled Journey" in forAll {
      (journey: ScheduledOverpaymentsClaim, response: ClaimSubmitResponse) =>
        val mapper       = scheduledOverpaymentsClaimToDec64FilesMapper
        val dec64Request = mapper.map(ScheduledOverpaymentsClaimRequest(journey), response)

        dec64Request.map { payload =>
          payload should include("<ans2:name>CaseReference</ans2:name>")
          payload should include("<ans2:name>Eori</ans2:name>")
          payload should include("<ans2:name>DeclarationId</ans2:name>")
          payload should include("<ans2:name>DeclarationType</ans2:name>")
          payload should include("<ans2:name>ApplicationName</ans2:name>")
          payload should include("<ans2:name>DocumentType</ans2:name>")
          payload should include(s"<ans2:batchSize>${journey.documents.size}</ans2:batchSize>")
          payload should include("<ans2:value>NDRC</ans2:value>")
        }
    }
  }
}
