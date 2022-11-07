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

package uk.gov.hmrc.cdsreimbursementclaim.services.tpi05

import cats.implicits.catsSyntaxEq
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimantType, SingleOverpaymentsClaim, TaxCode, TypeOfClaimAnswer}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.{NdrcDetails => DeclarationNdrcDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.C285
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{CaseType, Claimant, DeclarationMode, YesNo}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.email.{Email, EmailRequest}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error => CdsError}
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps

// todo CDSR-1795 TPI05 creation and validation - factor out common code
class OverpaymentsSingleClaimToTPI05Mapper
    extends ClaimToTPI05Mapper[(SingleOverpaymentsClaim, DisplayDeclaration, Option[DisplayDeclaration])] {

  @SuppressWarnings(Array("org.wartremover.warts.Option2Iterable"))
  override def map(
    details: (SingleOverpaymentsClaim, DisplayDeclaration, Option[DisplayDeclaration])
  ): Either[CdsError, EisSubmitClaimRequest] = {
    val (claim, declaration, duplicateDeclaration) = details
    val contactInfo                                = claim.claimantInformation.contactInformation

    (for {
      claimantEmail <- contactInfo.emailAddress.toRight(
                         CdsError("Email address is missing")
                       )
      contactPerson <- contactInfo.contactPerson.toRight(
                         CdsError("Email address is missing")
                       )
      importerEori  <- declaration.displayResponseDetail.consigneeDetails
                         .map(EORIInformation.forConsignee)
                         .toRight(CdsError("Could not deduce consignee EORI information"))
      agentEori     <- EORIInformation.forDeclarant(
                         declaration.displayResponseDetail.declarantDetails,
                         Some(claim.claimantInformation.contactInformation)
                       )
      eoriDetails    = EoriDetails(
                         importerEORIDetails = importerEori,
                         agentEORIDetails = agentEori
                       )
    } yield TPI05
      .request(
        claimantEORI = claim.claimantInformation.eori,
        claimantEmailAddress = Email(claimantEmail),
        claimantName = contactPerson
      )
      .forClaimOfType(Some(C285))
      .withClaimant(Claimant.basedOn(claim.claimantType))
      .withClaimedAmount(claim.reimbursementClaims.values.sum)
      .withReimbursementMethod(claim.reimbursementMethod)
      .withCaseType(CaseType.basedOn(TypeOfClaimAnswer.Individual, claim.reimbursementMethod))
      .withDeclarationMode(DeclarationMode.basedOn(TypeOfClaimAnswer.Individual))
      .withBasisOfClaim(claim.basisOfClaim.toTPI05DisplayString)
      .withGoodsDetails(
        GoodsDetails(
          descOfGoods = Some(claim.additionalDetails),
          isPrivateImporter = Some(claim.claimantType match {
            case ClaimantType.Consignee => YesNo.Yes
            case _                      => YesNo.No
          })
        )
      )
      .withEORIDetails(eoriDetails)
      .withMrnDetails(getMrnDetails(claim, declaration) :: Nil)
      .withMaybeDuplicateMrnDetails(
        duplicateDeclaration.map(d => getMrnDetails(claim, d, Some(MRN(d.displayResponseDetail.declarationId)), false))
      )).flatMap(_.verify)
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
      .withConsigneeDetails(displayDeclaration.displayResponseDetail.consigneeDetails)
      .withAccountDetails(if (includeAccountDetails) displayDeclaration.displayResponseDetail.accountDetails else None)
      .withFirstNonEmptyBankDetails(
        displayDeclaration.displayResponseDetail.bankDetails,
        claim.bankAccountDetails
      )
      .withNdrcDetails(
        for {
          (taxCode, reclaimAmount) <- claim.reimbursementClaims.toList
          ndrcDetails              <- nrdcDetails.filter(_.taxType === taxCode.value)
        } yield NdrcDetails.buildChecking(
          taxCode,
          ndrcDetails.paymentMethod,
          ndrcDetails.paymentReference,
          BigDecimal(ndrcDetails.amount).roundToTwoDecimalPlaces,
          reclaimAmount.roundToTwoDecimalPlaces
        )
      )

  }
}
