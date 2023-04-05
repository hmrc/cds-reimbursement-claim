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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{Country, Street}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.{ConsigneeDetails, ContactDetails, DeclarantDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error => CdsError}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori

final case class EORIInformation(
  EORINumber: Eori,
  CDSFullName: String,
  CDSEstablishmentAddress: Address,
  contactInformation: Option[ContactInformation],
  legalEntityType: Option[String] = None,
  EORIStartDate: Option[String] = None,
  VATDetails: Option[List[VATDetail]] = None
)

object EORIInformation {

  def forConsigneeOld(maybeConsigneeDetails: Option[ConsigneeDetails]): Either[CdsError, EORIInformation] = {
    val maybeContactDetails: Option[ContactDetails] = maybeConsigneeDetails.flatMap(_.contactDetails)

    val maybeTelephone    = maybeContactDetails.flatMap(_.telephone)
    val maybeEmailAddress = maybeContactDetails.flatMap(_.emailAddress)

    val maybeEstablishmentAddressLine1 = maybeConsigneeDetails.map(_.establishmentAddress.addressLine1)
    val maybeEstablishmentAddressLine2 = maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine2)
    val maybeEstablishmentAddressLine3 = maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine3)

    for {
      eoriNumber  <- maybeConsigneeDetails.map(_.EORI)
      cdsFullName <- maybeConsigneeDetails.map(_.legalName)
    } yield EORIInformation(
      EORINumber = eoriNumber,
      CDSFullName = cdsFullName,
      CDSEstablishmentAddress = Address(
        contactPerson = None,
        addressLine1 = maybeEstablishmentAddressLine1,
        addressLine2 = maybeEstablishmentAddressLine2,
        addressLine3 = maybeEstablishmentAddressLine3,
        street = Street.fromLines(maybeEstablishmentAddressLine1, maybeEstablishmentAddressLine2),
        city = maybeEstablishmentAddressLine3,
        countryCode = maybeConsigneeDetails
          .map(_.establishmentAddress.countryCode)
          .getOrElse(Country.uk.code),
        postalCode = maybeConsigneeDetails.flatMap(_.establishmentAddress.postalCode),
        telephoneNumber = maybeTelephone,
        emailAddress = maybeEmailAddress
      ),
      contactInformation = maybeConsigneeDetails.flatMap(_.contactDetails).map { contactDetails =>
        val maybeAddress1 = contactDetails.addressLine1
        val maybeAddress2 = contactDetails.addressLine2
        val maybeAddress3 = contactDetails.addressLine3

        ContactInformation(
          contactPerson = contactDetails.contactName,
          addressLine1 = maybeAddress1,
          addressLine2 = maybeAddress2,
          addressLine3 = maybeAddress3,
          street = Street.fromLines(maybeAddress1, maybeAddress2),
          city = maybeAddress3,
          countryCode = contactDetails.countryCode,
          postalCode = contactDetails.postalCode,
          telephoneNumber = maybeTelephone,
          faxNumber = None,
          emailAddress = maybeEmailAddress
        )
      }
    )
  }.toRight(CdsError("EORINumber and CDSFullName are mandatory"))

  def forConsignee(consigneeDetails: ConsigneeDetails): EORIInformation = {
    val maybeContactDetails: Option[ContactDetails] = consigneeDetails.contactDetails

    val maybeTelephone    = maybeContactDetails.flatMap(_.telephone)
    val maybeEmailAddress = maybeContactDetails.flatMap(_.emailAddress)

    val establishmentAddressLine1      = consigneeDetails.establishmentAddress.addressLine1
    val maybeEstablishmentAddressLine2 = consigneeDetails.establishmentAddress.addressLine2
    val maybeEstablishmentAddressLine3 = consigneeDetails.establishmentAddress.addressLine3

    EORIInformation(
      EORINumber = consigneeDetails.EORI,
      CDSFullName = consigneeDetails.legalName,
      CDSEstablishmentAddress = Address(
        contactPerson = None,
        addressLine1 = Some(establishmentAddressLine1),
        addressLine2 = maybeEstablishmentAddressLine2,
        addressLine3 = maybeEstablishmentAddressLine3,
        street = Street.fromLines(Some(establishmentAddressLine1), maybeEstablishmentAddressLine2),
        city = maybeEstablishmentAddressLine3,
        countryCode = consigneeDetails.establishmentAddress.countryCode,
        postalCode = consigneeDetails.establishmentAddress.postalCode,
        telephoneNumber = maybeTelephone,
        emailAddress = maybeEmailAddress
      ),
      contactInformation = consigneeDetails.contactDetails.map { contactDetails =>
        val maybeAddress1 = contactDetails.addressLine1
        val maybeAddress2 = contactDetails.addressLine2
        val maybeAddress3 = contactDetails.addressLine3

        ContactInformation(
          contactPerson = contactDetails.contactName,
          addressLine1 = maybeAddress1,
          addressLine2 = maybeAddress2,
          addressLine3 = maybeAddress3,
          street = Street.fromLines(maybeAddress1, maybeAddress2),
          city = maybeAddress3,
          countryCode = contactDetails.countryCode,
          postalCode = contactDetails.postalCode,
          telephoneNumber = maybeTelephone,
          faxNumber = None,
          emailAddress = maybeEmailAddress
        )
      }
    )
  }

  def forDeclarant(
    declarantDetails: DeclarantDetails,
    contactInformation: Option[ContactInformation]
  ): Either[CdsError, EORIInformation] =
    for {
      countryCode <- declarantDetails.contactDetails
                       .flatMap(_.countryCode)
                       .orElse(Some(Country.uk.code))
                       .toRight(CdsError("Country code not present in ACC14 response"))
    } yield EORIInformation(
      EORINumber = declarantDetails.EORI,
      CDSFullName = declarantDetails.legalName,
      CDSEstablishmentAddress = Address(
        contactPerson = declarantDetails.contactDetails.flatMap(_.contactName),
        addressLine1 = declarantDetails.contactDetails.flatMap(_.addressLine1),
        addressLine2 = declarantDetails.contactDetails.flatMap(_.addressLine2),
        addressLine3 = declarantDetails.contactDetails.flatMap(_.addressLine3),
        street = Street.fromLines(
          declarantDetails.contactDetails.flatMap(_.addressLine1),
          declarantDetails.contactDetails.flatMap(_.addressLine2)
        ),
        city = declarantDetails.contactDetails.flatMap(_.addressLine4),
        postalCode = declarantDetails.contactDetails.flatMap(_.postalCode),
        countryCode = countryCode,
        telephoneNumber = declarantDetails.contactDetails.flatMap(_.telephone),
        emailAddress = declarantDetails.contactDetails.flatMap(_.emailAddress)
      ),
      contactInformation = contactInformation
    )

  implicit val format: OFormat[EORIInformation] = Json.format[EORIInformation]
}
