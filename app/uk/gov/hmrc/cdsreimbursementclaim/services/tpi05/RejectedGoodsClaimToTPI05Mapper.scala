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
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.RejectedGoodsClaim
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.TemporalAccessorOps
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.CE1179
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.{DisplayDeclaration, DisplayResponseDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error => CdsError}
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps

// todo CDSR-1795 TPI05 creation and validation - factor out common code
class RejectedGoodsClaimToTPI05Mapper[Claim <: RejectedGoodsClaim](putReimbursementMethodInNDRCDetails: Boolean)
    extends ClaimToTPI05Mapper[(Claim, List[DisplayDeclaration])]
    with GetEoriDetails[Claim] {

  @SuppressWarnings(Array("org.wartremover.warts.Option2Iterable", "org.wartremover.warts.Throw"))
  override def map(
    details: (Claim, List[DisplayDeclaration])
  ): Either[CdsError, EisSubmitClaimRequest] = {
    val claim        = details._1
    val declarations = details._2
      .map(declaration => MRN(declaration.displayResponseDetail.declarationId) -> declaration.displayResponseDetail)
      .toMap

    val headDeclaration =
      details._2.headOption.getOrElse(throw new Exception("Missing head declaration"))

    (for {
      email        <- claim.claimantInformation.contactInformation.emailAddress
                        .toRight(CdsError("Email address is missing"))
      claimantName <- claim.claimantInformation.contactInformation.contactPerson
                        .toRight(CdsError("Claimant name is missing"))
      claimantEmail = Email(email)
    } yield TPI05
      .request(
        claimantEORI = claim.claimantInformation.eori,
        claimantEmailAddress = claimantEmail,
        claimantName = claimantName
      )
      .forClaimOfType(Some(CE1179))
      .withClaimant(Claimant.basedOn(claim.claimantType), Claimant.basedOn(claim.payeeType))
      .withClaimedAmount(claim.totalReimbursementAmount)
      .withReimbursementMethod(claim.reimbursementMethod, !putReimbursementMethodInNDRCDetails)
      .withDisposalMethod(claim.methodOfDisposal)
      .withBasisOfClaim(claim.basisOfClaim.toTPI05DisplayString)
      .withGoodsDetails(getGoodsDetails(claim))
      .withEORIDetails(getEoriDetails(claim, headDeclaration))
      .withMrnDetails(getMrnDetails(claim, declarations))
      .withDeclarationMode(claim.declarationMode)
      .withCaseType(claim.caseType)).flatMap(_.verify)
  }

  private def getGoodsDetails(claim: Claim) =
    GoodsDetails(
      descOfGoods = Some(claim.detailsOfRejectedGoods),
      anySpecialCircumstances = claim.basisOfClaimSpecialCircumstances,
      dateOfInspection = Some(claim.inspectionDate.toIsoLocalDate),
      atTheImporterOrDeclarantAddress = Some(claim.inspectionAddress.addressType.toTPI05DisplayString),
      inspectionAddress = Some(
        InspectionAddress(
          addressLine1 = claim.inspectionAddress.addressLine1,
          addressLine2 = claim.inspectionAddress.addressLine2,
          addressLine3 = claim.inspectionAddress.addressLine3,
          city = claim.inspectionAddress.city,
          countryCode = claim.inspectionAddress.countryCode,
          postalCode = claim.inspectionAddress.postalCode
        )
      )
    )

  private def getMrnDetails(claim: Claim, declarations: Map[MRN, DisplayResponseDetail]) = {
    val claimsOverMrns =
      claim.getClaimsOverMrns.flatMap { case (mrn, taxesClaimed) =>
        declarations.get(mrn).toList.map(declaration => mrn -> ((taxesClaimed, declaration)))
      }.toMap

    claimsOverMrns.map { case (mrn, (claimedReimbursement, declaration)) =>
      MrnDetail.build
        .withMrnNumber(mrn)
        .withAcceptanceDate(declaration.acceptanceDate)
        .withDeclarantReferenceNumber(declaration.declarantReferenceNumber)
        .withWhetherMainDeclarationReference(mrn === claim.leadMrn)
        .withProcedureCode(declaration.procedureCode)
        .withDeclarantDetails(declaration.declarantDetails)
        .withConsigneeDetails(Some(declaration.effectiveConsigneeDetails))
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
                  taxCode,
                  foundNdrcDetails.paymentMethod,
                  foundNdrcDetails.paymentReference,
                  BigDecimal(foundNdrcDetails.amount),
                  claimedAmount.roundToTwoDecimalPlaces,
                  if (putReimbursementMethodInNDRCDetails) Some(claim.reimbursementMethod) else None
                )
              }
          }.toList
        }
    }.toList

  }
}
