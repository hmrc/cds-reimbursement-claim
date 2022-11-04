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

import uk.gov.hmrc.cdsreimbursementclaim.models.claim.C285ClaimRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.email.{Email, EmailRequest}
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error => CdsError}

class C285ClaimToEmailMapper extends ClaimToEmailMapper[C285ClaimRequest] {
  override def map(claim: C285ClaimRequest): Either[CdsError, EmailRequest] = {
    val x = for {
      email       <- claim.claim.contactInformation.emailAddress.toRight(
                       CdsError("no email address provided with claim")
                     )
      contactName <- claim.claim.contactInformation.contactPerson.toRight(
                       CdsError("no contact nam perovided with claim")
                     )
      claimAmount  = claim.claim.claims.map(_.claimAmount).toList.sum
    } yield EmailRequest(
      Email(email),
      contactName,
      claimAmount
    )

    x
  }
}
