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

import cats.implicits.{catsSyntaxEq, catsSyntaxOption}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimantType, MultipleOverpaymentsClaim, TypeOfClaimAnswer}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.C285
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{CaseType, Claimant, DeclarationMode, YesNo}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.ConsigneeDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.{DisplayDeclaration, DisplayResponseDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error => CdsError}
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps

class OverpaymentsMultipleClaimToTPI05Mapper
    extends ClaimToTPI05Mapper[(MultipleOverpaymentsClaim, List[DisplayDeclaration])]
    with GetEoriDetails[MultipleOverpaymentsClaim] {

  @SuppressWarnings(Array("org.wartremover.warts.Option2Iterable"))
  override def map(
    details: (MultipleOverpaymentsClaim, List[DisplayDeclaration])
  ): Either[CdsError, EisSubmitClaimRequest] = {
    val claim        = details._1
    val declarations = details._2
      .map(declaration => MRN(declaration.displayResponseDetail.declarationId) -> declaration.displayResponseDetail)
      .toMap

    val contactInfo                                     = claim.claimantInformation.contactInformation
    val maybeConsigneeDetails: Option[ConsigneeDetails] =
      details._2.headOption.flatMap(_.displayResponseDetail.effectiveConsigneeDetails)

    (for {
      email            <- contactInfo.emailAddress.toRight(CdsError("Email address is missing"))
      claimantName     <- contactInfo.contactPerson.toRight(CdsError("Email address is missing"))
      claimantEmail     = Email(email)
      consigneeDetails <- maybeConsigneeDetails.toRight(CdsError("consignee EORINumber and CDSFullName are mandatory"))
    } yield TPI05
      .request(
        claimantEORI = claim.claimantInformation.eori,
        claimantEmailAddress = claimantEmail,
        claimantName = claimantName
      )
      .forClaimOfType(Some(C285))
      .withClaimant(Claimant.basedOn(claim.claimantType))
      .withClaimedAmount(claim.totalReimbursementAmount)
      .withReimbursementMethod(claim.reimbursementMethod)
      .withCaseType(CaseType.basedOn(TypeOfClaimAnswer.Multiple, claim.reimbursementMethod))
      .withDeclarationMode(DeclarationMode.basedOn(TypeOfClaimAnswer.Multiple))
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
      .withEORIDetails(getEoriDetails(consigneeDetails, claim))
      .withMrnDetails(getMrnDetails(claim, declarations))).flatMap(_.verify)
  }

  private def getMrnDetails(
    claim: MultipleOverpaymentsClaim,
    declarations: Map[MRN, DisplayResponseDetail]
  ): List[MrnDetail.Builder] = {
    val claimsOverMrns =
      claim.reimbursementClaims.flatMap { case (mrn, taxesClaimed) =>
        declarations.get(mrn).toList.map(declaration => mrn -> ((taxesClaimed, declaration)))
      }

    claimsOverMrns.map { case (mrn, (claimedReimbursement, declaration)) =>
      MrnDetail.build
        .withMrnNumber(mrn)
        .withAcceptanceDate(declaration.acceptanceDate)
        .withDeclarantReferenceNumber(declaration.declarantReferenceNumber)
        .withWhetherMainDeclarationReference(claim.leadMrn === mrn)
        .withProcedureCode(declaration.procedureCode)
        .withDeclarantDetails(declaration.declarantDetails)
        .withConsigneeDetails(declaration.effectiveConsigneeDetails)
        .withAccountDetails(declaration.accountDetails)
        .withFirstNonEmptyBankDetails(declaration.bankDetails, claim.bankAccountDetails)
        .withNdrcDetails {
          val ndrcDetails = declaration.ndrcDetails.toList.flatten

          claimedReimbursement.map { case (taxCode, claimedAmount) =>
            ndrcDetails
              .find(_.taxType === taxCode.value)
              .toValidNel(CdsError(s"Cannot find NDRC details for tax code: ${taxCode.value}"))
              .andThen { foundNdrcDetails =>
                NdrcDetails.buildChecking(
                  taxCode = taxCode,
                  paymentMethod = foundNdrcDetails.paymentMethod,
                  paymentReference = foundNdrcDetails.paymentReference,
                  BigDecimal(foundNdrcDetails.amount),
                  claimedAmount.roundToTwoDecimalPlaces
                )
              }
          }.toList
        }
    }.toList

  }
}
