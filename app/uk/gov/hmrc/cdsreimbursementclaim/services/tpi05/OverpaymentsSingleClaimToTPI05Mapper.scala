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

import cats.implicits.{catsSyntaxEq, catsSyntaxOption}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{Country, RejectedGoodsClaim, SingleOverpaymentsClaim, TypeOfClaimAnswer}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.TemporalAccessorOps
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.{C285, CE1179}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{CaseType, Claimant, DeclarationMode}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.ConsigneeDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.{DisplayDeclaration, DisplayResponseDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
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
    val contactInfo = claim.claimantInformation.contactInformation

    (for {
      claimantEmail <- contactInfo.emailAddress.toRight(
        CdsError("Email address is missing")
      )
      contactPerson <- contactInfo.contactPerson.toRight(
        CdsError("Email address is missing")
      )
//      eoriDetails <- claim.claimantInformation.eori
    } yield TPI05
      .request(
        claimantEORI = claim.claimantInformation.eori,
        claimantEmailAddress = claimantEmail,
        claimantName = contactPerson
      )
      .forClaimOfType(Some(C285))
      .withClaimant(Claimant.basedOn(claim.claimantType))
      .withClaimedAmount(claim.reimbursementClaims.map(_._2).sum)
      .withReimbursementMethod(claim.reimbursementMethod)
      .withCaseType(CaseType.basedOn(TypeOfClaimAnswer.Individual, claim.reimbursementMethod))
      .withDeclarationMode(DeclarationMode.basedOn(TypeOfClaimAnswer.Individual))
      .withBasisOfClaim(claim.basisOfClaim.toTPI05DisplayString)
      .withGoodsDetails(
        GoodsDetails(
          descOfGoods = Some(request.claim.additionalDetailsAnswer.value),
          isPrivateImporter = Some(YesNo.basedOn(request.claim.declarantTypeAnswer))
        )
      )
      .withEORIDetails(eoriDetails)
      .withMrnDetails(getMrnDetails(request))
      .withMaybeDuplicateMrnDetails(
        request.claim.duplicateDisplayDeclaration.map(_.displayResponseDetail).map { displayDeclaration =>
          MrnDetail.build
            .withMrnNumber(MRN(displayDeclaration.declarationId))
            .withAcceptanceDate(displayDeclaration.acceptanceDate)
            .withDeclarantReferenceNumber(displayDeclaration.declarantReferenceNumber)
            .withWhetherMainDeclarationReference(true)
            .withProcedureCode(displayDeclaration.procedureCode)
            .withDeclarantDetails(displayDeclaration.declarantDetails)
            .withConsigneeDetails(displayDeclaration.consigneeDetails)
            .withFirstNonEmptyBankDetails(displayDeclaration.bankDetails, request.claim.bankAccountDetailsAnswer)
            .withNdrcDetails(
              request.claim.claims.toList.map { reimbursement =>
                NdrcDetails.buildChecking(
                  reimbursement.taxCode,
                  reimbursement.paymentMethod,
                  reimbursement.paymentReference,
                  reimbursement.paidAmount.roundToTwoDecimalPlaces,
                  reimbursement.claimAmount.roundToTwoDecimalPlaces
                )
              }
            )
        }
      )).flatMap(_.verify)
  }

}
