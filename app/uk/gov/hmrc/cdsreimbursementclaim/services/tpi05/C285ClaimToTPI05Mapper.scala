/*
 * Copyright 2021 HM Revenue & Customs
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

import cats.implicits.catsSyntaxTuple2Semigroupal
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{C285Claim, C285ClaimRequest, Country, DeclarantTypeAnswer, Street}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.C285
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{CaseType, Claimant, DeclarationMode, YesNo}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.{Address, ContactInformation, EORIInformation, EisSubmitClaimRequest, EoriDetails, GoodsDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging

import javax.inject.Singleton

@Singleton
class C285ClaimToTPI05Mapper extends ClaimToTPI05Mapper[C285ClaimRequest] with Logging {

  def mapToEisSubmitClaimRequest(request: C285ClaimRequest): Either[Error, EisSubmitClaimRequest] = {

    def createEORIDetails(c285Claim: C285Claim): EoriDetails = {

      val agentContactInfo = (c285Claim.mrnContactDetailsAnswer, c285Claim.mrnContactAddressAnswer)
        .mapN(ContactInformation(_, _))
        .getOrElse(
          c285Claim.declarantTypeAnswer match {
            case DeclarantTypeAnswer.Importer | DeclarantTypeAnswer.AssociatedWithImporterCompany =>
              ContactInformation(c285Claim.consigneeDetails)
            case DeclarantTypeAnswer.AssociatedWithRepresentativeCompany                          =>
              ContactInformation(c285Claim.declarantDetails)
          }
        )

      EoriDetails(
        agentEORIDetails = EORIInformation(
          EORINumber = c285Claim.declarantDetails.map(_.EORI).getOrElse(Eori("")),
          CDSFullName = c285Claim.declarantDetails.map(_.legalName),
          CDSEstablishmentAddress = Address(
            contactPerson = Option(c285Claim.detailsRegisteredWithCdsAnswer.fullName),
            addressLine1 = Option(c285Claim.detailsRegisteredWithCdsAnswer.contactAddress.line1),
            addressLine2 = c285Claim.detailsRegisteredWithCdsAnswer.contactAddress.line2,
            AddressLine3 = c285Claim.detailsRegisteredWithCdsAnswer.contactAddress.line3,
            street = Street(
              Option(c285Claim.detailsRegisteredWithCdsAnswer.contactAddress.line1),
              c285Claim.detailsRegisteredWithCdsAnswer.contactAddress.line2
            ),
            city = Some(c285Claim.detailsRegisteredWithCdsAnswer.contactAddress.line4),
            postalCode = c285Claim.detailsRegisteredWithCdsAnswer.contactAddress.postcode,
            countryCode = c285Claim.detailsRegisteredWithCdsAnswer.contactAddress.country.code,
            telephone = None,
            emailAddress = Option(c285Claim.detailsRegisteredWithCdsAnswer.emailAddress.value)
          ),
          contactInformation = Some(agentContactInfo)
        ),
        importerEORIDetails = EORIInformation(
          EORINumber = c285Claim.consigneeDetails.map(_.EORI).getOrElse(Eori("")),
          CDSFullName = c285Claim.consigneeDetails.map(_.legalName),
          CDSEstablishmentAddress = Address(
            contactPerson = None,
            addressLine1 = c285Claim.consigneeDetails.map(_.establishmentAddress.addressLine1),
            addressLine2 = c285Claim.consigneeDetails.flatMap(_.establishmentAddress.addressLine2),
            AddressLine3 = c285Claim.consigneeDetails.flatMap(_.establishmentAddress.addressLine3),
            street = Street(
              c285Claim.consigneeDetails.map(_.establishmentAddress.addressLine1),
              c285Claim.consigneeDetails.flatMap(_.establishmentAddress.addressLine2)
            ),
            city = c285Claim.consigneeDetails.flatMap(_.establishmentAddress.addressLine3),
            countryCode = c285Claim.consigneeDetails.map(_.establishmentAddress.countryCode).getOrElse(Country.uk.code),
            postalCode = c285Claim.consigneeDetails.flatMap(_.establishmentAddress.postalCode),
            telephone = c285Claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.telephone)),
            emailAddress = c285Claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.emailAddress))
          ),
          contactInformation = Some(
            ContactInformation(
              contactPerson = c285Claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.contactName)),
              addressLine1 = c285Claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine1)),
              addressLine2 = c285Claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine2)),
              addressLine3 = c285Claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine3)),
              street = Street(
                c285Claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine1)),
                c285Claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine2))
              ),
              city = c285Claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine3)),
              countryCode = c285Claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.countryCode)),
              postalCode = c285Claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.postalCode)),
              telephoneNumber = c285Claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.telephone)),
              faxNumber = None,
              emailAddress = c285Claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.emailAddress))
            )
          )
        )
      )
    }

    TPI05.request
      .forClaimOfType(C285)
      .withClaimant(Claimant(request.claim.declarantTypeAnswer))
      .withClaimantEORI(request.signedInUserDetails.eori)
      .withClaimantEmail(request.signedInUserDetails.email)
      .withClaimedAmount(request.claim.totalReimbursementAmount)
      .withReimbursementMethod(request.claim.reimbursementMethodAnswer)
      .withCaseType(CaseType(request.claim.typeOfClaim, request.claim.reimbursementMethodAnswer))
      .withDeclarationMode(DeclarationMode(request.claim.typeOfClaim))
      .withBasisOfClaim(request.claim.basisOfClaimAnswer.toTPI05Key)
      .withEORIDetails(createEORIDetails(request.claim))
      .withGoodsDetails(
        GoodsDetails(
          descOfGoods = Some(request.claim.commodityDetailsAnswer.value),
          isPrivateImporter = Some(YesNo(request.claim.declarantTypeAnswer))
        )
      )
      .verify
  }
}
