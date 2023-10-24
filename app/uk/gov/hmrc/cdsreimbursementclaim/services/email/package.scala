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

package uk.gov.hmrc.cdsreimbursementclaim.services

import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{MultipleRejectedGoodsClaim, SingleRejectedGoodsClaim}

package object email {

  implicit val overpaymentsSingleClaimToEmailMapper: OverpaymentsSingleClaimToEmailMapper =
    new OverpaymentsSingleClaimToEmailMapper

  implicit val overpaymentsMultipleClaimToEmailMapper: OverpaymentsMultipleClaimToEmailMapper =
    new OverpaymentsMultipleClaimToEmailMapper

  implicit val overpaymentsScheduledClaimToEmailMapper: OverpaymentsScheduledClaimToEmailMapper =
    new OverpaymentsScheduledClaimToEmailMapper

  implicit val singleRejectedGoodsClaimToEmailMapper: RejectedGoodsClaimToEmailMapper[SingleRejectedGoodsClaim] =
    new RejectedGoodsClaimToEmailMapper[SingleRejectedGoodsClaim]

  implicit val multipleRejectedGoodsClaimToEmailMapper: RejectedGoodsClaimToEmailMapper[MultipleRejectedGoodsClaim] =
    new RejectedGoodsClaimToEmailMapper[MultipleRejectedGoodsClaim]

  implicit val scheduledRejectedGoodsClaimToEmailMapper: ScheduledRejectedGoodsClaimToEmailMapper =
    new ScheduledRejectedGoodsClaimToEmailMapper

  implicit val securitiesClaimToEmailMapper: SecuritiesClaimToEmailMapper =
    new SecuritiesClaimToEmailMapper
}
