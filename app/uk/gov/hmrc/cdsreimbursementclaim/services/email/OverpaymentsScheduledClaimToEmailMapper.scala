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

package uk.gov.hmrc.cdsreimbursementclaim.services.email

import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ScheduledOverpaymentsClaim
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.email.{Email, EmailRequest}
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error => CdsError}

class OverpaymentsScheduledClaimToEmailMapper
    extends ClaimToEmailMapper[(ScheduledOverpaymentsClaim, DisplayDeclaration, Option[DisplayDeclaration])] {
  override def map(
    claim: (ScheduledOverpaymentsClaim, DisplayDeclaration, Option[DisplayDeclaration])
  ): Either[CdsError, EmailRequest] = {
    val (overpaymentsClaim, _, _) = claim
    for {
      email       <- overpaymentsClaim.claimantInformation.contactInformation.emailAddress.toRight(
                       CdsError("no email address provided with claim")
                     )
      contactName <- overpaymentsClaim.claimantInformation.contactInformation.contactPerson.toRight(
                       CdsError("no contact name provided with claim")
                     )
      claimAmount  = overpaymentsClaim.reimbursementClaims.values.sum
    } yield EmailRequest(
      Email(email),
      contactName,
      claimAmount
    )
  }
}
