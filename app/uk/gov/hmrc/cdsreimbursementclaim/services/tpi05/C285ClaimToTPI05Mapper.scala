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

import cats.implicits.{catsSyntaxEq, catsSyntaxTuple2Semigroupal}
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{C285ClaimRequest, DeclarantTypeAnswer, Street}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.C285
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{CaseType, Claimant, DeclarationMode, YesNo}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps

class C285ClaimToTPI05Mapper extends ClaimToTPI05Mapper[C285ClaimRequest] {

  def map(request: C285ClaimRequest): Either[Error, EisSubmitClaimRequest] =
    TPI05.request
      .forClaimOfType(C285)
      .withClaimant(Claimant.basedOn(request.claim.declarantTypeAnswer))
      .withClaimantEORI(request.signedInUserDetails.eori)
      .withClaimantEmail(request.signedInUserDetails.email)
      .withClaimedAmount(request.claim.totalReimbursementAmount)
      .withReimbursementMethod(request.claim.reimbursementMethodAnswer)
      .withCaseType(CaseType.basedOn(request.claim.typeOfClaim, request.claim.reimbursementMethodAnswer))
      .withDeclarationMode(DeclarationMode.basedOn(request.claim.typeOfClaim))
      .withBasisOfClaim(request.claim.basisOfClaimAnswer.toTPI05DisplayString)
      .withGoodsDetails(
        GoodsDetails(
          descOfGoods = Some(request.claim.additionalDetailsAnswer.value),
          isPrivateImporter = Some(YesNo.basedOn(request.claim.declarantTypeAnswer))
        )
      )
      .withEORIDetails(
        EoriDetails(
          importerEORIDetails = EORIInformation.forConsignee(request.claim.consigneeDetails),
          agentEORIDetails = EORIInformation(
            EORINumber = request.claim.declarantDetails.map(_.EORI),
            CDSFullName = request.claim.declarantDetails.map(_.legalName),
            CDSEstablishmentAddress = Address(
              contactPerson = Option(request.claim.detailsRegisteredWithCdsAnswer.fullName),
              addressLine1 = Option(request.claim.detailsRegisteredWithCdsAnswer.contactAddress.line1),
              addressLine2 = request.claim.detailsRegisteredWithCdsAnswer.contactAddress.line2,
              addressLine3 = request.claim.detailsRegisteredWithCdsAnswer.contactAddress.line3,
              street = Street.fromLines(
                Option(request.claim.detailsRegisteredWithCdsAnswer.contactAddress.line1),
                request.claim.detailsRegisteredWithCdsAnswer.contactAddress.line2
              ),
              city = Some(request.claim.detailsRegisteredWithCdsAnswer.contactAddress.line4),
              postalCode = Some(request.claim.detailsRegisteredWithCdsAnswer.contactAddress.postcode.value),
              countryCode = request.claim.detailsRegisteredWithCdsAnswer.contactAddress.country.code,
              telephoneNumber = None,
              emailAddress = Option(request.claim.detailsRegisteredWithCdsAnswer.emailAddress.value)
            ),
            contactInformation = Some(
              (request.claim.mrnContactDetailsAnswer, request.claim.mrnContactAddressAnswer)
                .mapN(ContactInformation(_, _))
                .getOrElse(
                  request.claim.declarantTypeAnswer match {
                    case DeclarantTypeAnswer.Importer | DeclarantTypeAnswer.AssociatedWithImporterCompany =>
                      ContactInformation.asPerClaimant(request.claim.consigneeDetails)
                    case DeclarantTypeAnswer.AssociatedWithRepresentativeCompany                          =>
                      ContactInformation.asPerClaimant(request.claim.declarantDetails)
                  }
                )
            )
          )
        )
      )
      .withMrnDetails(
        request.claim.displayDeclaration.toList.flatMap { displayDeclaration =>
          request.claim.multipleClaims.map { case (mrn, reimbursementClaim) =>
            MrnDetail.build
              .withMrnNumber(mrn)
              .withAcceptanceDate(displayDeclaration.displayResponseDetail.acceptanceDate)
              .withDeclarantReferenceNumber(displayDeclaration.displayResponseDetail.declarantReferenceNumber)
              .withWhetherMainDeclarationReference(request.claim.movementReferenceNumber.value === mrn.value)
              .withProcedureCode(displayDeclaration.displayResponseDetail.procedureCode)
              .withDeclarantDetails(displayDeclaration.displayResponseDetail.declarantDetails)
              .withConsigneeDetails(displayDeclaration.displayResponseDetail.consigneeDetails)
              .withAccountDetails(displayDeclaration.displayResponseDetail.accountDetails)
              .withFirstNonEmptyBankDetailsWhen(request.claim.movementReferenceNumber.value === mrn.value)(
                displayDeclaration.displayResponseDetail.bankDetails,
                request.claim.bankAccountDetailsAnswer
              )
              .withNdrcDetails(
                reimbursementClaim.toList.map(reimbursement =>
                  NdrcDetails.buildChecking(
                    reimbursement.taxCode,
                    reimbursement.paymentMethod,
                    reimbursement.paymentReference,
                    reimbursement.paidAmount.roundToTwoDecimalPlaces,
                    reimbursement.claimAmount.roundToTwoDecimalPlaces
                  )
                )
              )
          }
        }
      )
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
      )
      .verify
}
