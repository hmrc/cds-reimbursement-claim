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

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{Assertion, Assertions, Ignore, OptionValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{C285ClaimRequest, ClaimantInformation, ClaimantType, ClaimedReimbursement, DeclarantTypeAnswer, SingleOverpaymentsClaim, Street, TypeOfClaimAnswer}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.{ContactInformation, EisSubmitClaimRequest}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.C285ClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.NdrcDetails

@Ignore
class C285MappingCrossValidationSpec
    extends AnyWordSpec
    with C285ClaimSupport
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with OptionValues
    with TypeCheckedTripleEquals {

  def toClaimantType(declarantType: DeclarantTypeAnswer): ClaimantType =
    declarantType match {
      case DeclarantTypeAnswer.Importer                            => ClaimantType.Consignee
      case DeclarantTypeAnswer.AssociatedWithImporterCompany       => ClaimantType.Declarant
      case DeclarantTypeAnswer.AssociatedWithRepresentativeCompany => ClaimantType.User
    }

  "The C285 claim old and new mappers " should {

    "produce the same TPI05 request for a single claim" in forAll { generatedRequest: C285ClaimRequest =>
      whenever(generatedRequest.claim.typeOfClaim === TypeOfClaimAnswer.Individual) {
        val c285ClaimRequest = generatedRequest.copy(claim = generatedRequest.claim.copy(associatedMRNsAnswer = None))
        val tpi05RequestOld  = c285ClaimToTPI05Mapper.map(c285ClaimRequest)
        val declaration      = c285ClaimRequest.claim.displayDeclaration.value

        val overpaymentsSingleRequest: (SingleOverpaymentsClaim, DisplayDeclaration, Option[DisplayDeclaration]) = (
          SingleOverpaymentsClaim(
            movementReferenceNumber = c285ClaimRequest.claim.movementReferenceNumber,
            duplicateMovementReferenceNumber = c285ClaimRequest.claim.duplicateMovementReferenceNumberAnswer,
            claimantType = toClaimantType(c285ClaimRequest.claim.declarantTypeAnswer),
            claimantInformation = ClaimantInformation(
              c285ClaimRequest.signedInUserDetails.eori,
              c285ClaimRequest.signedInUserDetails.contactName.value,
              establishmentAddress = ContactInformation(
                contactPerson = Some(c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.fullName),
                addressLine1 = Some(c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.contactAddress.line1),
                addressLine2 = c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.contactAddress.line2,
                addressLine3 = c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.contactAddress.line3,
                street = Street.fromLines(
                  Some(c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.contactAddress.line1),
                  c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.contactAddress.line2
                ),
                city = Some(c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.contactAddress.line4),
                countryCode = Some(c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.contactAddress.country.code),
                postalCode = Some(c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.contactAddress.postcode.value),
                telephoneNumber = None,
                faxNumber = None,
                emailAddress = Some(c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.emailAddress.value)
              ),
              contactInformation = c285ClaimRequest.claim.contactInformation
            ),
            basisOfClaim = c285ClaimRequest.claim.basisOfClaimAnswer,
            whetherNorthernIreland = true,
            additionalDetails = c285ClaimRequest.claim.additionalDetailsAnswer.value,
            reimbursementClaims =
              c285ClaimRequest.claim.claimedReimbursementsAnswer.map(c => (c.taxCode, c.claimAmount)).toList.toMap,
            reimbursementMethod = c285ClaimRequest.claim.reimbursementMethodAnswer,
            bankAccountDetails = c285ClaimRequest.claim.bankAccountDetailsAnswer,
            supportingEvidences = c285ClaimRequest.claim.documents.toList.flatten
          ),
          declaration.copy(
            declaration.displayResponseDetail.copy(ndrcDetails =
              Some(
                c285ClaimRequest.claim.claims
                  .map(x =>
                    NdrcDetails(x.taxCode.value, x.paidAmount.toString(), x.paymentMethod, x.paymentReference, None)
                  )
                  .toList
              )
            )
          ),
          c285ClaimRequest.claim.duplicateDisplayDeclaration.map(dup =>
            dup.copy(
              dup.displayResponseDetail.copy(ndrcDetails =
                Some(
                  c285ClaimRequest.claim.claims
                    .map(x =>
                      NdrcDetails(x.taxCode.value, x.paidAmount.toString(), x.paymentMethod, x.paymentReference, None)
                    )
                    .toList
                )
              )
            )
          )
        )
        val tpi05RequestNew                                                                                      = overpaymentsSingleClaimToTPI05Mapper.map(overpaymentsSingleRequest)

        tpi05RequestOld.isRight                                                        shouldBe tpi05RequestNew.isRight
        Json.toJson(tpi05RequestOld.toOption.value.postNewClaimsRequest.requestDetail) shouldBe Json.toJson(
          tpi05RequestNew.toOption.value.postNewClaimsRequest.requestDetail
        )
      }
    }

  }
}
