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

package uk.gov.hmrc.cdsreimbursementclaim.services

import uk.gov.hmrc.cdsreimbursementclaim.models.claim._

package object ccs {

  implicit val c285ClaimToDec64FilesMapper: C285ClaimToDec64FilesMapper =
    new C285ClaimToDec64FilesMapper

  implicit val singleCE1779ClaimToDec64FilesMapper: RejectedGoodsClaimToDec64Mapper[SingleRejectedGoodsClaim] =
    new RejectedGoodsClaimToDec64Mapper[SingleRejectedGoodsClaim]

  implicit val multipleCE1779ClaimToDec64FilesMapper: RejectedGoodsClaimToDec64Mapper[MultipleRejectedGoodsClaim] =
    new RejectedGoodsClaimToDec64Mapper[MultipleRejectedGoodsClaim]

  implicit val scheduledCE1779ClaimToDec64FilesMapper: RejectedGoodsClaimToDec64Mapper[ScheduledRejectedGoodsClaim] =
    new RejectedGoodsClaimToDec64Mapper[ScheduledRejectedGoodsClaim]

  implicit val securitiesClaimToDec64FilesMapper: SecuritiesClaimToDec64Mapper =
    new SecuritiesClaimToDec64Mapper
}
