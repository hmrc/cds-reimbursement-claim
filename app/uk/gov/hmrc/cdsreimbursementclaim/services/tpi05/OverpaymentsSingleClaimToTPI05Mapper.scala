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

import cats.implicits.catsSyntaxEq

import uk.gov.hmrc.cdsreimbursementclaim.models.claim.SingleOverpaymentsClaim
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.TaxCode
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.TypeOfClaimAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.CaseType
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.C285
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.DeclarationMode
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.{NdrcDetails => DeclarationNdrcDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error => CdsError}
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps

// todo CDSR-1795 TPI05 creation and validation - factor out common code
class OverpaymentsSingleClaimToTPI05Mapper(putReimbursementMethodInNDRCDetails: Boolean)
    extends ClaimToTPI05Mapper[(SingleOverpaymentsClaim, DisplayDeclaration, Option[DisplayDeclaration])]
    with GetEoriDetails[SingleOverpaymentsClaim] {

  @SuppressWarnings(Array("org.wartremover.warts.Option2Iterable"))
  override def map(
    details: (SingleOverpaymentsClaim, DisplayDeclaration, Option[DisplayDeclaration])
  ): Either[CdsError, EisSubmitClaimRequest] = {
    val (claim, declaration, duplicateDeclaration) = details

    val contactInfo =
      claim.claimantInformation.contactInformation

    (for {
      claimantEmail <- contactInfo.emailAddress.toRight(CdsError("Email address is missing"))
      contactPerson <- contactInfo.contactPerson.toRight(CdsError("Claimant name is missing"))
    } yield TPI05
      .request(
        claimantEORI = claim.claimantInformation.eori,
        claimantEmailAddress = Email(claimantEmail),
        claimantName = contactPerson
      )
      .forClaimOfType(Some(C285))
      .withClaimant(Claimant.basedOn(claim.claimantType), Claimant.basedOn(claim.payeeType))
      .withClaimedAmount(claim.reimbursements.map(_.amount).sum)
      .withReimbursementMethod(claim.reimbursementMethod, !putReimbursementMethodInNDRCDetails)
      .withCaseType(CaseType.basedOn(TypeOfClaimAnswer.Individual, claim.reimbursementMethod))
      .withDeclarationMode(DeclarationMode.basedOn(TypeOfClaimAnswer.Individual))
      .withBasisOfClaim(claim.basisOfClaim.toTPI05DisplayString)
      .withGoodsDetails(
        GoodsDetails.from(claim.additionalDetails, claim.newEoriAndDan, claim.claimantType)
      )
      .withEORIDetails(getEoriDetails(claim, declaration))
      .withMrnDetails(getMrnDetails(claim, declaration) :: Nil)
      .withMaybeDuplicateMrnDetails(
        duplicateDeclaration.map(d =>
          getMrnDetails(claim, d, Some(MRN(d.displayResponseDetail.declarationId)), includeAccountDetails = false)
        )
      )
      .withMaybeNewEORIAndDAN(claim.newEoriAndDan)).flatMap(_.verify)
  }

  private def getMrnDetails(
    claim: SingleOverpaymentsClaim,
    displayDeclaration: DisplayDeclaration,
    mrn: Option[MRN] = None,
    includeAccountDetails: Boolean = true
  ) = {
    val nrdcDetails: List[DeclarationNdrcDetails] =
      displayDeclaration.displayResponseDetail.ndrcDetails.toList.flatten.sortBy(x => TaxCode.getOrFail(x.taxType))
    MrnDetail.build
      .withMrnNumber(mrn.getOrElse(claim.movementReferenceNumber))
      .withAcceptanceDate(displayDeclaration.displayResponseDetail.acceptanceDate)
      .withDeclarantReferenceNumber(displayDeclaration.displayResponseDetail.declarantReferenceNumber)
      .withWhetherMainDeclarationReference(true)
      .withProcedureCode(displayDeclaration.displayResponseDetail.procedureCode)
      .withDeclarantDetails(displayDeclaration.displayResponseDetail.declarantDetails)
      .withConsigneeDetails(Some(displayDeclaration.displayResponseDetail.effectiveConsigneeDetails))
      .withAccountDetails(if (includeAccountDetails) displayDeclaration.displayResponseDetail.accountDetails else None)
      .withFirstNonEmptyBankDetails(
        displayDeclaration.displayResponseDetail.bankDetails,
        claim.bankAccountDetails
      )
      .withNdrcDetails(
        for {
          reimbursement <- claim.reimbursements.toList
          ndrcDetails   <- nrdcDetails.filter(_.taxType === reimbursement.taxCode.value)
        } yield NdrcDetails.buildChecking(
          reimbursement.taxCode,
          ndrcDetails.paymentMethod,
          ndrcDetails.paymentReference,
          BigDecimal(ndrcDetails.amount).roundToTwoDecimalPlaces,
          reimbursement.amount.roundToTwoDecimalPlaces,
          if (putReimbursementMethodInNDRCDetails) Some(reimbursement.reimbursementMethod) else None
        )
      )

  }
}
