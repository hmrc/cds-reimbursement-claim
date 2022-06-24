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
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error => CdsError}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{C285ClaimRequest, Street}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.C285
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{CaseType, Claimant, DeclarationMode, YesNo}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps

class C285ClaimToTPI05Mapper extends ClaimToTPI05Mapper[C285ClaimRequest] {

  // todo CDSR-1795 TPI05 creation and validation - factor out common code
  def map(request: C285ClaimRequest): Either[CdsError, EisSubmitClaimRequest] =
    (for {
      claimantEmail <- request.signedInUserDetails.email.toRight(
                         CdsError("Email address is missing")
                       ) // todo CDSR-1795 can we use verified email instead?
      eoriDetails   <- getEoriDetails(request)
    } yield TPI05
      .request(
        claimantEORI = request.signedInUserDetails.eori,
        claimantEmailAddress = claimantEmail //,
//        claimantName = request.signedInUserDetails.contactName.value
      )
      .forClaimOfType(C285)
      .withClaimant(Claimant.basedOn(request.claim.declarantTypeAnswer))
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

  private def getEoriDetails(request: C285ClaimRequest): Either[CdsError, EoriDetails] =
    for {
      agentEoriNumber  <- request.claim.declarantDetails.map(_.EORI).toRight(CdsError("agent EORI is required"))
      agentCdsFullName <-
        request.claim.declarantDetails.map(_.legalName).toRight(CdsError("agent CDS Full Name is required"))
      consigneeDetails <-
        request.claim.consigneeDetails.toRight(CdsError("consignee EORINumber and CDSFullName are mandatory"))
    } yield EoriDetails(
      importerEORIDetails = EORIInformation.forConsignee(consigneeDetails),
      agentEORIDetails = EORIInformation(
        EORINumber = agentEoriNumber,
        CDSFullName = agentCdsFullName,
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
        contactInformation = Some(request.claim.contactInformation)
      )
    )

  private def getMrnDetails(request: C285ClaimRequest) =
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

}
