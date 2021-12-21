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

import cats.data.Ior
import cats.implicits.{catsSyntaxEq, catsSyntaxTuple2Semigroupal}
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{C285ClaimRequest, Country, DeclarantTypeAnswer, Street}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.C285
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{CaseType, Claimant, DeclarationMode, YesNo}

class C285ClaimToTPI05Mapper extends ClaimToTPI05Mapper[C285ClaimRequest] {

  def mapToEisSubmitClaimRequest(request: C285ClaimRequest): Either[Error, EisSubmitClaimRequest] =
    TPI05.request
      .forClaimOfType(C285)
      .withClaimant(Claimant.of(request.claim.declarantTypeAnswer))
      .withClaimantEORI(request.signedInUserDetails.eori)
      .withClaimantEmail(request.signedInUserDetails.email)
      .withClaimedAmount(request.claim.totalReimbursementAmount)
      .withReimbursementMethod(request.claim.reimbursementMethodAnswer)
      .withCaseType(CaseType.resolveFrom(request.claim.typeOfClaim, request.claim.reimbursementMethodAnswer))
      .withDeclarationMode(DeclarationMode.of(request.claim.typeOfClaim))
      .withBasisOfClaim(request.claim.basisOfClaimAnswer.toTPI05Key)
      .withGoodsDetails(
        GoodsDetails(
          descOfGoods = Some(request.claim.commodityDetailsAnswer.value),
          isPrivateImporter = Some(YesNo.whetherFrom(request.claim.declarantTypeAnswer))
        )
      )
      .withEORIDetails(
        EoriDetails(
          agentEORIDetails = EORIInformation(
            EORINumber = request.claim.declarantDetails.map(_.EORI),
            CDSFullName = request.claim.declarantDetails.map(_.legalName),
            CDSEstablishmentAddress = Address(
              contactPerson = Option(request.claim.detailsRegisteredWithCdsAnswer.fullName),
              addressLine1 = Option(request.claim.detailsRegisteredWithCdsAnswer.contactAddress.line1),
              addressLine2 = request.claim.detailsRegisteredWithCdsAnswer.contactAddress.line2,
              AddressLine3 = request.claim.detailsRegisteredWithCdsAnswer.contactAddress.line3,
              street = Street.of(
                Option(request.claim.detailsRegisteredWithCdsAnswer.contactAddress.line1),
                request.claim.detailsRegisteredWithCdsAnswer.contactAddress.line2
              ),
              city = Some(request.claim.detailsRegisteredWithCdsAnswer.contactAddress.line4),
              postalCode = request.claim.detailsRegisteredWithCdsAnswer.contactAddress.postcode,
              countryCode = request.claim.detailsRegisteredWithCdsAnswer.contactAddress.country.code,
              telephone = None,
              emailAddress = Option(request.claim.detailsRegisteredWithCdsAnswer.emailAddress.value)
            ),
            contactInformation = Some(
              (request.claim.mrnContactDetailsAnswer, request.claim.mrnContactAddressAnswer)
                .mapN(ContactInformation.combine)
                .getOrElse(
                  request.claim.declarantTypeAnswer match {
                    case DeclarantTypeAnswer.Importer | DeclarantTypeAnswer.AssociatedWithImporterCompany =>
                      ContactInformation.from(request.claim.consigneeDetails)
                    case DeclarantTypeAnswer.AssociatedWithRepresentativeCompany                          =>
                      ContactInformation.from(request.claim.declarantDetails)
                  }
                )
            )
          ),
          importerEORIDetails = EORIInformation(
            EORINumber = request.claim.consigneeDetails.map(_.EORI),
            CDSFullName = request.claim.consigneeDetails.map(_.legalName),
            CDSEstablishmentAddress = Address(
              contactPerson = None,
              addressLine1 = request.claim.consigneeDetails.map(_.establishmentAddress.addressLine1),
              addressLine2 = request.claim.consigneeDetails.flatMap(_.establishmentAddress.addressLine2),
              AddressLine3 = request.claim.consigneeDetails.flatMap(_.establishmentAddress.addressLine3),
              street = Street.of(
                request.claim.consigneeDetails.map(_.establishmentAddress.addressLine1),
                request.claim.consigneeDetails.flatMap(_.establishmentAddress.addressLine2)
              ),
              city = request.claim.consigneeDetails.flatMap(_.establishmentAddress.addressLine3),
              countryCode = request.claim.consigneeDetails
                .map(_.establishmentAddress.countryCode)
                .getOrElse(Country.uk.code),
              postalCode = request.claim.consigneeDetails.flatMap(_.establishmentAddress.postalCode),
              telephone = request.claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.telephone)),
              emailAddress = request.claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.emailAddress))
            ),
            contactInformation = Some(
              ContactInformation(
                contactPerson = request.claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.contactName)),
                addressLine1 = request.claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine1)),
                addressLine2 = request.claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine2)),
                addressLine3 = request.claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine3)),
                street = Street.of(
                  request.claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine1)),
                  request.claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine2))
                ),
                city = request.claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine3)),
                countryCode = request.claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.countryCode)),
                postalCode = request.claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.postalCode)),
                telephoneNumber = request.claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.telephone)),
                faxNumber = None,
                emailAddress = request.claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.emailAddress))
              )
            )
          )
        )
      )
      .withMrnDetails(
        request.claim.displayDeclaration.toList.flatMap { displayDeclaration =>
          request.claim.multipleClaims.map { case (mrn, reimbursementClaim) =>
            val declarantDetails      = displayDeclaration.displayResponseDetail.declarantDetails
            val maybeConsigneeDetails = displayDeclaration.displayResponseDetail.consigneeDetails

            MrnDetail.build
              .withMrnNumber(mrn)
              .withAcceptanceDate(displayDeclaration.displayResponseDetail.acceptanceDate)
              .withDeclarantReferenceNumber(displayDeclaration.displayResponseDetail.declarantReferenceNumber)
              .withWhetherMainDeclarationReference(request.claim.movementReferenceNumber.value === mrn.value)
              .withProcedureCode(displayDeclaration.displayResponseDetail.procedureCode)
              .withDeclarantDetails(
                declarantDetails.EORI,
                declarantDetails.legalName,
                Address(
                  contactPerson = None,
                  addressLine1 = Some(declarantDetails.establishmentAddress.addressLine1),
                  addressLine2 = declarantDetails.establishmentAddress.addressLine2,
                  AddressLine3 = declarantDetails.establishmentAddress.addressLine3,
                  street = Street.of(
                    Option(declarantDetails.establishmentAddress.addressLine1),
                    declarantDetails.establishmentAddress.addressLine2
                  ),
                  city = declarantDetails.establishmentAddress.addressLine3,
                  countryCode = declarantDetails.establishmentAddress.countryCode,
                  postalCode = declarantDetails.establishmentAddress.postalCode,
                  telephone = None,
                  emailAddress = None
                ),
                declarantDetails.contactDetails.map(contactDetails =>
                  ContactInformation(
                    contactPerson = contactDetails.contactName,
                    addressLine1 = contactDetails.addressLine1,
                    addressLine2 = contactDetails.addressLine2,
                    addressLine3 = contactDetails.addressLine3,
                    street = Street.of(contactDetails.addressLine1, contactDetails.addressLine2),
                    city = contactDetails.addressLine3,
                    countryCode = contactDetails.countryCode,
                    postalCode = contactDetails.postalCode,
                    telephoneNumber = contactDetails.telephone,
                    faxNumber = None,
                    emailAddress = contactDetails.emailAddress
                  )
                )
              )
              .withConsigneeDetails(
                maybeConsigneeDetails.map(consigneeDetails =>
                  (
                    consigneeDetails.EORI,
                    consigneeDetails.legalName,
                    Address(
                      contactPerson = None,
                      addressLine1 = Some(consigneeDetails.establishmentAddress.addressLine1),
                      addressLine2 = consigneeDetails.establishmentAddress.addressLine2,
                      AddressLine3 = consigneeDetails.establishmentAddress.addressLine3,
                      street = Street.of(
                        Option(consigneeDetails.establishmentAddress.addressLine1),
                        consigneeDetails.establishmentAddress.addressLine2
                      ),
                      city = consigneeDetails.establishmentAddress.addressLine3,
                      countryCode = consigneeDetails.establishmentAddress.countryCode,
                      postalCode = consigneeDetails.establishmentAddress.postalCode,
                      telephone = None,
                      emailAddress = None
                    ),
                    consigneeDetails.contactDetails.map(contactDetails =>
                      ContactInformation(
                        contactPerson = contactDetails.contactName,
                        addressLine1 = contactDetails.addressLine1,
                        addressLine2 = contactDetails.addressLine2,
                        addressLine3 = contactDetails.addressLine3,
                        street = Street.of(contactDetails.addressLine1, contactDetails.addressLine2),
                        city = contactDetails.addressLine3,
                        countryCode = contactDetails.countryCode,
                        postalCode = contactDetails.postalCode,
                        telephoneNumber = contactDetails.telephone,
                        faxNumber = None,
                        emailAddress = contactDetails.emailAddress
                      )
                    )
                  )
                )
              )
              .withBankDetails(
                Ior.fromOptions(displayDeclaration.displayResponseDetail.bankDetails, request.claim.bankAccountDetailsAnswer)
              )
          }
        }: _*
      )
      .verify
}
