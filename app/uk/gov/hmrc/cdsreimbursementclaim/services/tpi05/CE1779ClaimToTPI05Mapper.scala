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

import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{Country, RejectedGoodsClaim, Street}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.ISOLocalDate
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.CE1179
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{Claimant, InspectionAddressType}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email

class CE1779ClaimToTPI05Mapper extends ClaimToTPI05Mapper[(RejectedGoodsClaim, DisplayDeclaration)] {

  def mapToEisSubmitClaimRequest(
    data: (RejectedGoodsClaim, DisplayDeclaration)
  ): Either[Error, EisSubmitClaimRequest] = {
    val claim                 = data._1
    val declaration           = data._2.displayResponseDetail
    val maybeConsigneeDetails = declaration.consigneeDetails

    TPI05.request
      .forClaimOfType(CE1179)
      .withClaimant(Claimant.of(claim.claimantType))
      .withClaimantEmail(claim.claimantInformation.contactInformation.emailAddress.map(Email(_)))
      .withClaimantEORI(claim.claimantInformation.eori)
      .withClaimedAmount(claim.totalReimbursementAmount)
      .withReimbursementMethod(claim.reimbursementMethod)
      .withDisposalMethod(claim.methodOfDisposal)
      .withBasisOfClaim(claim.basisOfClaim.toTPI05Key)
      .withGoodsDetails(
        GoodsDetails(
          descOfGoods = Some(claim.detailsOfRejectedGoods),
          anySpecialCircumstances = claim.basisOfClaimSpecialCircumstances,
          dateOfInspection = Some(ISOLocalDate.of(claim.inspectionDate)),
          atTheImporterOrDeclarantAddress = Some(InspectionAddressType(claim.claimantType)),
          inspectionAddress = Some(claim.inspectionAddress)
        )
      )
      .withEORIDetails(
        EoriDetails(
          agentEORIDetails = EORIInformation(
            EORINumber = Some(claim.claimantInformation.eori),
            CDSFullName = claim.claimantInformation.fullName,
            CDSEstablishmentAddress = Address.from(claim.claimantInformation.establishmentAddress),
            contactInformation = Some(claim.claimantInformation.contactInformation)
          ),
          importerEORIDetails = EORIInformation(
            EORINumber = maybeConsigneeDetails.map(_.consigneeEORI),
            CDSFullName = maybeConsigneeDetails.map(_.legalName),
            CDSEstablishmentAddress = Address(
              contactPerson = None,
              addressLine1 = maybeConsigneeDetails.map(_.establishmentAddress.addressLine1),
              addressLine2 = maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine2),
              AddressLine3 = maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine3),
              street = Street.of(
                maybeConsigneeDetails.map(_.establishmentAddress.addressLine1),
                maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine2)
              ),
              city = maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine3),
              countryCode = maybeConsigneeDetails
                .map(_.establishmentAddress.countryCode)
                .getOrElse(Country.uk.code),
              postalCode = maybeConsigneeDetails.flatMap(_.establishmentAddress.postalCode),
              telephone = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.telephone)),
              emailAddress = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.emailAddress))
            ),
            contactInformation = Some(
              ContactInformation(
                contactPerson = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.contactName)),
                addressLine1 = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine1)),
                addressLine2 = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine2)),
                addressLine3 = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine3)),
                street = Street.of(
                  maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine1)),
                  maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine2))
                ),
                city = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine3)),
                countryCode = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.countryCode)),
                postalCode = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.postalCode)),
                telephoneNumber = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.telephone)),
                faxNumber = None,
                emailAddress = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.emailAddress))
              )
            )
          )
        )
      )
      .withMrnDetails(
        MrnDetail.build
          .withMrnNumber(claim.movementReferenceNumber)
          .withAcceptanceDate(declaration.acceptanceDate)
          .withDeclarantReferenceNumber(declaration.declarantReferenceNumber)
          .withWhetherMainDeclarationReference(true)
          .withProcedureCode(declaration.procedureCode)
          .withDeclarantDetails(
            declaration.declarantDetails.declarantEORI,
            declaration.declarantDetails.legalName,
            Address(
              contactPerson = None,
              addressLine1 = Some(declaration.declarantDetails.establishmentAddress.addressLine1),
              addressLine2 = declaration.declarantDetails.establishmentAddress.addressLine2,
              AddressLine3 = declaration.declarantDetails.establishmentAddress.addressLine3,
              street = Street.of(
                Option(declaration.declarantDetails.establishmentAddress.addressLine1),
                declaration.declarantDetails.establishmentAddress.addressLine2
              ),
              city = declaration.declarantDetails.establishmentAddress.addressLine3,
              countryCode = declaration.declarantDetails.establishmentAddress.countryCode,
              postalCode = declaration.declarantDetails.establishmentAddress.postalCode,
              telephone = None,
              emailAddress = None
            ),
            declaration.declarantDetails.contactDetails.map(contactDetails =>
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
                consigneeDetails.consigneeEORI,
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
      )
      .verify
  }
}
