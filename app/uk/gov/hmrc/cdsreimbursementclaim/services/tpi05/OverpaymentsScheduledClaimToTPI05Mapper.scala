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

import uk.gov.hmrc.cdsreimbursementclaim.models.Error as CdsError
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ScheduledOverpaymentsClaim, TypeOfClaimAnswer}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.*
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.C285
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{CaseType, Claimant, DeclarationMode}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.{DisplayDeclaration, DisplayResponseDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps

class OverpaymentsScheduledClaimToTPI05Mapper(putReimbursementMethodInNDRCDetails: Boolean)
    extends ClaimToTPI05Mapper[(ScheduledOverpaymentsClaim, DisplayDeclaration)]
    with GetEoriDetails[ScheduledOverpaymentsClaim] {

  @SuppressWarnings(Array("org.wartremover.warts.Option2Iterable", "org.wartremover.warts.Throw"))
  override def map(
    details: (ScheduledOverpaymentsClaim, DisplayDeclaration)
  ): Either[CdsError, EisSubmitClaimRequest] = {
    val (claim, declaration) = details
    val contactInfo          = claim.claimantInformation.contactInformation

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
      .withClaimedAmount(claim.totalReimbursementAmount)
      .withReimbursementMethod(claim.reimbursementMethod, !putReimbursementMethodInNDRCDetails)
      .withCaseType(CaseType.basedOn(TypeOfClaimAnswer.Scheduled, claim.reimbursementMethod))
      .withDeclarationMode(DeclarationMode.basedOn(TypeOfClaimAnswer.Scheduled))
      .withBasisOfClaim(claim.basisOfClaim.toTPI05DisplayString)
      .withGoodsDetails(
        GoodsDetails.from(claim.additionalDetails, claim.newEoriAndDan, claim.claimantType)
      )
      .withEORIDetails(getEoriDetails(claim, declaration))
      .withMrnDetails(getMrnDetails(claim, declaration.displayResponseDetail))
      .withMaybeNewEORIAndDAN(claim.newEoriAndDan)).flatMap(_.verify)
  }

  private def getMrnDetails(
    claim: ScheduledOverpaymentsClaim,
    displayResponseDetail: DisplayResponseDetail
  ): List[MrnDetail.Builder] = {
    val claimedReimbursement = claim.getClaimedReimbursements

    MrnDetail.build
      .withMrnNumber(claim.movementReferenceNumber)
      .withAcceptanceDate(displayResponseDetail.acceptanceDate)
      .withDeclarantReferenceNumber(displayResponseDetail.declarantReferenceNumber)
      .withWhetherMainDeclarationReference(true)
      .withProcedureCode(displayResponseDetail.procedureCode)
      .withDeclarantDetails(displayResponseDetail.declarantDetails)
      .withConsigneeDetails(Some(displayResponseDetail.effectiveConsigneeDetails))
      .withAccountDetails(displayResponseDetail.accountDetails)
      .withFirstNonEmptyBankDetails(displayResponseDetail.bankDetails, claim.bankAccountDetails)
      .withNdrcDetails(
        claimedReimbursement.map(reimbursement =>
          NdrcDetails.buildChecking(
            reimbursement.taxCode,
            reimbursement.paymentMethod,
            reimbursement.paymentReference,
            reimbursement.paidAmount.roundToTwoDecimalPlaces,
            reimbursement.claimAmount.roundToTwoDecimalPlaces,
            if (putReimbursementMethodInNDRCDetails) Some(claim.reimbursementMethod) else None
          )
        )
      )
  } :: Nil
}
