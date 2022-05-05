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
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimSubmitResponse, MultipleRejectedGoodsClaim, RejectedGoodsClaimRequest, ScheduledRejectedGoodsClaim, SingleRejectedGoodsClaim}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.RejectedGoodsClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TPI05RequestGen._

class RejectedGoodsClaimToDec64MapperSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  "Rejected Goods Claim to Dec64 Mapper" should {
    "obtain the correct batch size for a Single Journey" in forAll {
      (journey: SingleRejectedGoodsClaim, response: ClaimSubmitResponse) =>
        val mapper       = singleCE1779ClaimToDec64FilesMapper
        val dec64Request = mapper.map(RejectedGoodsClaimRequest(journey), response)

        dec64Request.map { envelope =>
          envelope.Body.BatchFileInterfaceMetadata.batchSize shouldBe journey.supportingEvidences.size
        }
    }

    "obtain the correct batch size for a Multiple Journey" in forAll {
      (journey: MultipleRejectedGoodsClaim, response: ClaimSubmitResponse) =>
        val mapper       = multipleCE1779ClaimToDec64FilesMapper
        val dec64Request = mapper.map(RejectedGoodsClaimRequest(journey), response)

        dec64Request.map { envelope =>
          envelope.Body.BatchFileInterfaceMetadata.batchSize shouldBe journey.supportingEvidences.size
        }
    }

    "obtain the correct batch size for a Scheduled Journey" in forAll {
      (journey: ScheduledRejectedGoodsClaim, response: ClaimSubmitResponse) =>
        val mapper       = scheduledCE1779ClaimToDec64FilesMapper
        val dec64Request = mapper.map(RejectedGoodsClaimRequest(journey), response)

        dec64Request.map { envelope =>
          envelope.Body.BatchFileInterfaceMetadata.batchSize shouldBe journey.supportingEvidences.size + 1
        }
    }
  }
}
