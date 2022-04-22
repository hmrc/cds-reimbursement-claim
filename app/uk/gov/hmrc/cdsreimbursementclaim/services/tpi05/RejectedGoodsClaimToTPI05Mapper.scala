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
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{Country, RejectedGoodsClaim}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.TemporalAccessorOps
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.CE1179
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.{DisplayDeclaration}
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps

class RejectedGoodsClaimToTPI05Mapper[Claim <: RejectedGoodsClaim]
    extends ClaimToTPI05Mapper[(Claim, List[DisplayDeclaration])] {

  @SuppressWarnings(Array("org.wartremover.warts.Option2Iterable"))
  override def map(
    details: (Claim, List[DisplayDeclaration])
  ): Either[Error, EisSubmitClaimRequest] = {
    val claim                 = details._1
    val declarations          = details._2
      .map(declaration => MRN(declaration.displayResponseDetail.declarationId) -> declaration.displayResponseDetail)
      .toMap
    val maybeConsigneeDetails = details._2.headOption.flatMap(_.displayResponseDetail.consigneeDetails)

    val claimsOverMrns =
      claim.getClaimsOverMrns.flatMap { case (mrn, taxesClaimed) =>
        declarations.get(mrn).map(declaration => mrn -> ((taxesClaimed, declaration)))
      }.toMap

    TPI05.request
      .forClaimOfType(CE1179)
      .withClaimant(Claimant.basedOn(claim.claimantType))
      .withClaimantEmail(claim.claimantInformation.contactInformation.emailAddress.map(Email(_)))
      .withClaimantEORI(claim.claimantInformation.eori)
      .withClaimedAmount(claim.totalReimbursementAmount)
      .withReimbursementMethod(claim.reimbursementMethod)
      .withDisposalMethod(claim.methodOfDisposal)
      .withBasisOfClaim(claim.basisOfClaim.toTPI05DisplayString)
      .withGoodsDetails(
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
      )
      .withEORIDetails(
        EoriDetails(
          importerEORIDetails = EORIInformation.forConsignee(maybeConsigneeDetails),
          agentEORIDetails = EORIInformation(
            EORINumber = Some(claim.claimantInformation.eori),
            CDSFullName = claim.claimantInformation.fullName,
            CDSEstablishmentAddress = Address(
              contactPerson = claim.claimantInformation.establishmentAddress.contactPerson,
              addressLine1 = claim.claimantInformation.establishmentAddress.addressLine1,
              addressLine2 = claim.claimantInformation.establishmentAddress.addressLine2,
              addressLine3 = claim.claimantInformation.establishmentAddress.addressLine3,
              street = claim.claimantInformation.establishmentAddress.street,
              city = claim.claimantInformation.establishmentAddress.city,
              countryCode = claim.claimantInformation.establishmentAddress.countryCode.getOrElse(Country.uk.code),
              postalCode = claim.claimantInformation.establishmentAddress.postalCode,
              telephoneNumber = claim.claimantInformation.establishmentAddress.telephoneNumber,
              emailAddress = claim.claimantInformation.establishmentAddress.emailAddress
            ),
            contactInformation = Some(claim.claimantInformation.contactInformation)
          )
        )
      )
      .withMrnDetails(
        claimsOverMrns.map { case (mrn, (claimedReimbursement, declaration)) =>
          MrnDetail.build
            .withMrnNumber(mrn)
            .withAcceptanceDate(declaration.acceptanceDate)
            .withDeclarantReferenceNumber(declaration.declarantReferenceNumber)
            .withWhetherMainDeclarationReference(mrn === claim.leadMrn)
            .withProcedureCode(declaration.procedureCode)
            .withDeclarantDetails(declaration.declarantDetails)
            .withConsigneeDetails(declaration.consigneeDetails)
            .withAccountDetails(declaration.accountDetails)
            .withFirstNonEmptyBankDetails(declaration.bankDetails, claim.bankAccountDetails)
            .withNdrcDetails {
              val ndrcDetails = declaration.ndrcDetails.toList.flatten

              claimedReimbursement.map { case (taxCode, claimedAmount) =>
                ndrcDetails
                  .find(_.taxType === taxCode.value)
                  .toValidNel(Error(s"Cannot find NDRC details for tax code: ${taxCode.value}"))
                  .andThen { foundNdrcDetails =>
                    NdrcDetails.buildChecking(
                      taxCode,
                      foundNdrcDetails.paymentMethod,
                      foundNdrcDetails.paymentReference,
                      BigDecimal(foundNdrcDetails.amount),
                      claimedAmount.roundToTwoDecimalPlaces
                    )
                  }
              }.toList
            }
        }.toList
      )
      .withDeclarationMode(claim.declarationMode)
      .withCaseType(claim.caseType)
      .verify
  }
}
