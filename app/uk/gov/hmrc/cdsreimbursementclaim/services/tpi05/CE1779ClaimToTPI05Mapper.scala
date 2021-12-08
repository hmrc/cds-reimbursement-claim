/*
 * Copyright 2021 HM Revenue & Customs
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

import uk.gov.hmrc.cdsreimbursementclaim.models
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.RejectedGoodsClaimRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.EisSubmitClaimRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.CE1179
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging

class CE1779ClaimToTPI05Mapper extends ClaimToTPI05Mapper[RejectedGoodsClaimRequest] with Logging {

  def mapToEisSubmitClaimRequest(request: RejectedGoodsClaimRequest): Either[models.Error, EisSubmitClaimRequest] =
    TPI05.request
      .forClaimOfType(CE1179)
      .verify
}
