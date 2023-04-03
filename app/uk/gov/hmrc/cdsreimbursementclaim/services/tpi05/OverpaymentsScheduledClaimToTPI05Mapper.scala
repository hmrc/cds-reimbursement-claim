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

import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimantType, ScheduledOverpaymentsClaim, TypeOfClaimAnswer}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.C285
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{CaseType, Claimant, DeclarationMode, YesNo}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.{DisplayDeclaration, DisplayResponseDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error => CdsError}
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps

class OverpaymentsScheduledClaimToTPI05Mapper
    extends ClaimToTPI05Mapper[(ScheduledOverpaymentsClaim, DisplayDeclaration)] {

  @SuppressWarnings(Array("org.wartremover.warts.Option2Iterable"))
  override def map(
    details: (ScheduledOverpaymentsClaim, DisplayDeclaration)
  ): Either[CdsError, EisSubmitClaimRequest] = {
    val (claim, declaration) = details
    val contactInfo          = claim.claimantInformation.contactInformation

    (for {
      claimantEmail <- contactInfo.emailAddress.toRight(
                         CdsError("Email address is missing")
                       )
      contactPerson <- contactInfo.contactPerson.toRight(
                         CdsError("Email address is missing")
                       )
      importerEori  <- declaration.displayResponseDetail.effectiveConsigneeDetails
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
      .withClaimedAmount(claim.totalReimbursementAmount)
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
      .withMrnDetails(getMrnDetails(claim, declaration.displayResponseDetail))).flatMap(_.verify)
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
      .withConsigneeDetails(displayResponseDetail.effectiveConsigneeDetails)
      .withAccountDetails(displayResponseDetail.accountDetails)
      .withFirstNonEmptyBankDetails(displayResponseDetail.bankDetails, claim.bankAccountDetails)
      .withNdrcDetails(
        claimedReimbursement.map(reimbursement =>
          NdrcDetails.buildChecking(
            reimbursement.taxCode,
            reimbursement.paymentMethod,
            reimbursement.paymentReference,
            reimbursement.paidAmount.roundToTwoDecimalPlaces,
            reimbursement.claimAmount.roundToTwoDecimalPlaces
          )
        )
      )
  } :: Nil
}
