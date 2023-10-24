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

package uk.gov.hmrc.cdsreimbursementclaim.services.tpi05

import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.EisSubmitClaimRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.claim._

trait ClaimToTPI05Mapper[Claim] {
  def map(claim: Claim): Either[Error, EisSubmitClaimRequest]
}

object ClaimToTPI05Mappers {

  def apply(putReimbursementMethodInNDRCDetails: Boolean): Bundle =
    new Bundle(putReimbursementMethodInNDRCDetails)

  class Bundle(putReimbursementMethodInNDRCDetails: Boolean) {

    implicit val overpaymentsSingleClaimToTPI05Mapper: OverpaymentsSingleClaimToTPI05Mapper =
      new OverpaymentsSingleClaimToTPI05Mapper(putReimbursementMethodInNDRCDetails)

    implicit val overpaymentsMultipleClaimToTPI05Mapper: OverpaymentsMultipleClaimToTPI05Mapper =
      new OverpaymentsMultipleClaimToTPI05Mapper(putReimbursementMethodInNDRCDetails)

    implicit val overpaymentsScheduledClaimToTPI05Mapper: OverpaymentsScheduledClaimToTPI05Mapper =
      new OverpaymentsScheduledClaimToTPI05Mapper(putReimbursementMethodInNDRCDetails)

    implicit val singleRejectedGoodsClaimToTPI05Mapper: RejectedGoodsClaimToTPI05Mapper[SingleRejectedGoodsClaim] =
      new RejectedGoodsClaimToTPI05Mapper[SingleRejectedGoodsClaim](putReimbursementMethodInNDRCDetails)

    implicit val multipleRejectedGoodsClaimToTPI05Mapper: RejectedGoodsClaimToTPI05Mapper[MultipleRejectedGoodsClaim] =
      new RejectedGoodsClaimToTPI05Mapper[MultipleRejectedGoodsClaim](putReimbursementMethodInNDRCDetails)

    implicit val scheduledRejectedGoodsClaimToTPI05Mapper: ScheduledRejectedGoodsClaimToTPI05Mapper =
      new ScheduledRejectedGoodsClaimToTPI05Mapper(putReimbursementMethodInNDRCDetails)

    implicit val securitiesClaimToTPI05Mapper: SecuritiesClaimToTPI05Mapper =
      new SecuritiesClaimToTPI05Mapper
  }
}
