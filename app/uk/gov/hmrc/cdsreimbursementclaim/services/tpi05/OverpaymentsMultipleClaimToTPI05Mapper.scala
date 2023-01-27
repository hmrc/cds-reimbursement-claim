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
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimantType, Country, MultipleOverpaymentsClaim, TypeOfClaimAnswer}
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
    extends ClaimToTPI05Mapper[(MultipleOverpaymentsClaim, List[DisplayDeclaration])] {

  @SuppressWarnings(Array("org.wartremover.warts.Option2Iterable"))
  override def map(
    details: (MultipleOverpaymentsClaim, List[DisplayDeclaration])
  ): Either[CdsError, EisSubmitClaimRequest] = {
    val (claim, declarations)                           = details
    val contactInfo                                     = claim.claimantInformation.contactInformation
    val maybeConsigneeDetails: Option[ConsigneeDetails] =
      declarations.headOption.flatMap(_.displayResponseDetail.consigneeDetails)
    val mrnDeclarationsMap                              = declarations
      .map(declaration => MRN(declaration.displayResponseDetail.declarationId) -> declaration.displayResponseDetail)
      .toMap

    (for {
      claimantEmail <- contactInfo.emailAddress.toRight(
                         CdsError("Email address is missing")
                       )
      contactPerson <- contactInfo.contactPerson.toRight(
                         CdsError("Email address is missing")
                       )

      consigneeDetails <- maybeConsigneeDetails.toRight(CdsError("consignee EORINumber and CDSFullName are mandatory"))
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
      .withEORIDetails(getEoriDetails(consigneeDetails, claim))
      .withMrnDetails(getMrnDetails(claim, mrnDeclarationsMap))).flatMap(_.verify)
  }

  private def getEoriDetails(consigneeDetails: ConsigneeDetails, claim: MultipleOverpaymentsClaim): EoriDetails =
    EoriDetails(
      importerEORIDetails = EORIInformation.forConsignee(consigneeDetails),
      agentEORIDetails = EORIInformation(
        EORINumber = claim.claimantInformation.eori,
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

  private def getMrnDetails(
    claim: MultipleOverpaymentsClaim,
    declarations: Map[MRN, DisplayResponseDetail]
  ): List[MrnDetail.Builder] = {
    val claimsOverMrns =
      claim.reimbursementClaims.flatMap { case (mrn, taxesClaimed) =>
        declarations.get(MRN(mrn)).toList.map(declaration => mrn -> ((taxesClaimed, declaration)))
      }

    claimsOverMrns.map { case (mrn, (claimedReimbursement, declaration)) =>
      MrnDetail.build
        .withMrnNumber(MRN(mrn))
        .withAcceptanceDate(declaration.acceptanceDate)
        .withDeclarantReferenceNumber(declaration.declarantReferenceNumber)
        .withWhetherMainDeclarationReference(claim.leadMrn.value === mrn)
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
              .toValidNel(CdsError(s"Cannot find NDRC details for tax code: ${taxCode.value}"))
              .andThen { foundNdrcDetails =>
                NdrcDetails.buildChecking(
                  taxCode = taxCode,
                  paymentMethod = foundNdrcDetails.paymentMethod,
                  paymentReference = foundNdrcDetails.paymentReference,
                  claimedAmount.paidAmount.roundToTwoDecimalPlaces,
                  claimedAmount.refundAmount.roundToTwoDecimalPlaces
                )
              }
          }.toList
        }
    }.toList

  }
}
