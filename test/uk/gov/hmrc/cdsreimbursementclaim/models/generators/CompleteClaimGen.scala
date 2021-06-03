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

import org.scalacheck.Gen
import org.scalacheck.ScalacheckShapeless._
import uk.gov.hmrc.cdsreimbursementclaim.models.ClaimsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.BankAccountDetailsAnswer.CompleteBankAccountDetailAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.BasisOfClaimAnswer.CompleteBasisOfClaimAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.CompleteClaim.CompleteC285Claim
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DeclarantTypeAnswer.CompleteDeclarantTypeAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DeclarationDetailsAnswer.CompleteDeclarationDetailsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.MovementReferenceNumberAnswer.CompleteMovementReferenceNumberAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{Claim, CompleteClaim, ContactDetailsFormData, DetailsRegisteredWithCdsFormData, EntryDeclarationDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.MRNInformation

object CompleteClaimGen extends GenUtils {

  implicit val claimantDetailsAsIndividualGen: Gen[DetailsRegisteredWithCdsFormData]                =
    gen[DetailsRegisteredWithCdsFormData]
  implicit val claimantDetailsAsImporterCompanyGen: Gen[ContactDetailsFormData]                     =
    gen[ContactDetailsFormData]
  implicit val claimGen: Gen[Claim]                                                                 = gen[Claim]
  implicit val completeClaimsAnswerGen: Gen[ClaimsAnswer]                                           = gen[ClaimsAnswer]
  implicit val completeBankAccountDetailAnswerGen: Gen[CompleteBankAccountDetailAnswer]             =
    gen[CompleteBankAccountDetailAnswer]
  implicit val mrnInformationGen: Gen[MRNInformation]                                               = gen[MRNInformation]
  implicit val entryDeclarationDetailsGen: Gen[EntryDeclarationDetails]                             = gen[EntryDeclarationDetails]
  implicit val completeDeclarationDetailsAnswerGen: Gen[CompleteDeclarationDetailsAnswer]           =
    gen[CompleteDeclarationDetailsAnswer]
  implicit val completeBasisOfClaimAnswerGen: Gen[CompleteBasisOfClaimAnswer]                       = gen[CompleteBasisOfClaimAnswer]
  implicit val completeDeclarantTypeAnswerGen: Gen[CompleteDeclarantTypeAnswer]                     = gen[CompleteDeclarantTypeAnswer]
  implicit val completeMovementReferenceNumberAnswerGen: Gen[CompleteMovementReferenceNumberAnswer] =
    gen[CompleteMovementReferenceNumberAnswer]
  implicit val completeClaimGen: Gen[CompleteClaim]                                                 = gen[CompleteClaim]
  implicit val completeC285ClaimGen: Gen[CompleteC285Claim]                                         = gen[CompleteC285Claim]

}
