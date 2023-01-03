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

import uk.gov.hmrc.cdsreimbursementclaim.models.claim._

package object tpi05 {

  implicit val c285ClaimToTPI05Mapper: C285ClaimToTPI05Mapper =
    new C285ClaimToTPI05Mapper

  implicit val overpaymentsSingleClaimToTPI05Mapper: OverpaymentsSingleClaimToTPI05Mapper =
    new OverpaymentsSingleClaimToTPI05Mapper

  implicit val singleRejectedGoodsClaimToTPI05Mapper: RejectedGoodsClaimToTPI05Mapper[SingleRejectedGoodsClaim] =
    new RejectedGoodsClaimToTPI05Mapper[SingleRejectedGoodsClaim]

  implicit val multipleRejectedGoodsClaimToTPI05Mapper: RejectedGoodsClaimToTPI05Mapper[MultipleRejectedGoodsClaim] =
    new RejectedGoodsClaimToTPI05Mapper[MultipleRejectedGoodsClaim]

  implicit val scheduledRejectedGoodsClaimToTPI05Mapper: ScheduledRejectedGoodsClaimToTPI05Mapper =
    new ScheduledRejectedGoodsClaimToTPI05Mapper

  implicit val securitiesClaimToTPI05Mapper: SecuritiesClaimToTPI05Mapper =
    new SecuritiesClaimToTPI05Mapper
}
