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

package uk.gov.hmrc.cdsreimbursementclaim.services.email

import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ScheduledRejectedGoodsClaim
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.email.{Email, EmailRequest}
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error => CdsError}

class ScheduledRejectedGoodsClaimToEmailMapper
    extends ClaimToEmailMapper[(ScheduledRejectedGoodsClaim, DisplayDeclaration)] {

  override def map(claim: (ScheduledRejectedGoodsClaim, DisplayDeclaration)): Either[CdsError, EmailRequest] = {
    val (scheduledRejectedGoodsClaim, _) = claim
    for {
      email       <- scheduledRejectedGoodsClaim.claimantInformation.contactInformation.emailAddress.toRight(
                       CdsError("no email address provided with claim")
                     )
      contactName <- scheduledRejectedGoodsClaim.claimantInformation.contactInformation.contactPerson.toRight(
                       CdsError("no contact nam perovided with claim")
                     )
      claimAmount  = scheduledRejectedGoodsClaim.totalReimbursementAmount
    } yield EmailRequest(
      Email(email),
      contactName,
      claimAmount
    )
  }

}
