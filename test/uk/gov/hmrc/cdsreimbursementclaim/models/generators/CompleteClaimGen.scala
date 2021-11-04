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

package uk.gov.hmrc.cdsreimbursementclaim.models.generators

import org.scalacheck.magnolia._
import org.scalacheck.magnolia.Typeclass
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.CompleteClaim
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.answers.ClaimedReimbursementsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.MRNInformation
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.BasisOfClaim
object CompleteClaimGen {

  implicit val detailsRegisteredWithCdsAnswerGen: Typeclass[DetailsRegisteredWithCdsAnswer] =
    gen[DetailsRegisteredWithCdsAnswer]
  implicit val contactAddressGen: Typeclass[ContactAddress]                                 = gen[ContactAddress]
  implicit val mrnContactDetailsGen: Typeclass[MrnContactDetails]                           = gen[MrnContactDetails]
  implicit val claimGen: Typeclass[ClaimedReimbursement]                                    = gen[ClaimedReimbursement]
  implicit val claimedReimbursementAnswerGen: Typeclass[ClaimedReimbursementsAnswer]        = gen[ClaimedReimbursementsAnswer]
  implicit val bankAccountDetailsAnswerGen: Typeclass[BankAccountDetails]                   =
    gen[BankAccountDetails]
  implicit val mrnInformationGen: Typeclass[MRNInformation]                                 = gen[MRNInformation]
  implicit val basisOfClaimAnswerGen: Typeclass[BasisOfClaim]                               = gen[BasisOfClaim]
  implicit val declarantTypeAnswerGen: Typeclass[DeclarantTypeAnswer]                       = gen[DeclarantTypeAnswer]
  implicit val completeClaimGen: Typeclass[CompleteClaim]                                   = gen[CompleteClaim]
}
