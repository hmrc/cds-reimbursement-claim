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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ContactAddress, ContactDetails, Street}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.{ClaimantDetails, ConsigneeDetails, DeclarantDetails}

final case class ContactInformation(
  contactPerson: Option[String],
  addressLine1: Option[String],
  addressLine2: Option[String],
  addressLine3: Option[String],
  street: Option[String],
  city: Option[String],
  countryCode: Option[String],
  postalCode: Option[String],
  telephoneNumber: Option[String],
  faxNumber: Option[String],
  emailAddress: Option[String]
)

object ContactInformation {

  def apply(contactDetails: ContactDetails, contactAddress: ContactAddress): ContactInformation =
    new ContactInformation(
      contactPerson = Option(contactDetails.fullName),
      addressLine1 = Option(contactAddress.line1),
      addressLine2 = contactAddress.line2,
      addressLine3 = contactAddress.line3,
      street = Street.fromLines(Option(contactAddress.line1), contactAddress.line2),
      city = Option(contactAddress.line4),
      countryCode = Option(contactAddress.country.code),
      postalCode = Option(contactAddress.postcode.value),
      telephoneNumber = contactDetails.phoneNumber.map(_.value),
      faxNumber = None,
      emailAddress = Option(contactDetails.emailAddress.value)
    )

  def asPerClaimant(claimantDetails: Option[ClaimantDetails]): ContactInformation = {

    val maybeContactDetails = claimantDetails.flatMap(_.contactDetails)

    new ContactInformation(
      contactPerson = claimantDetails.map(_.legalName),
      addressLine1 = maybeContactDetails.flatMap(_.addressLine1),
      addressLine2 = maybeContactDetails.flatMap(_.addressLine2),
      addressLine3 = maybeContactDetails.flatMap(_.addressLine3),
      street = Street.fromLines(
        maybeContactDetails.flatMap(_.addressLine1),
        maybeContactDetails.flatMap(_.addressLine2)
      ),
      city = maybeContactDetails.flatMap(_.addressLine3),
      postalCode = maybeContactDetails.flatMap(_.postalCode),
      countryCode = maybeContactDetails.flatMap(_.countryCode),
      telephoneNumber = maybeContactDetails.flatMap(_.telephone),
      faxNumber = None,
      emailAddress = maybeContactDetails.flatMap(_.emailAddress)
    )
  }

  def fromDeclarantDetails(declarantDetails: DeclarantDetails): ContactInformation =
    new ContactInformation(
      contactPerson = declarantDetails.contactDetails.flatMap(_.contactName),
      addressLine1 = declarantDetails.contactDetails.flatMap(_.addressLine1),
      addressLine2 = declarantDetails.contactDetails.flatMap(_.addressLine2),
      addressLine3 = declarantDetails.contactDetails.flatMap(_.addressLine3),
      street = Street.fromLines(
        declarantDetails.contactDetails.flatMap(_.addressLine1),
        declarantDetails.contactDetails.flatMap(_.addressLine2)
      ),
      city = declarantDetails.contactDetails.flatMap(_.addressLine3),
      countryCode = declarantDetails.contactDetails.flatMap(_.countryCode),
      postalCode = declarantDetails.contactDetails.flatMap(_.postalCode),
      telephoneNumber = declarantDetails.contactDetails.flatMap(_.telephone),
      faxNumber = None,
      emailAddress = declarantDetails.contactDetails.flatMap(_.emailAddress)
    )

  def fromConsigneeDetails(consigneeDetails: ConsigneeDetails): ContactInformation =
    new ContactInformation(
      contactPerson = consigneeDetails.contactDetails.flatMap(_.contactName),
      addressLine1 = consigneeDetails.contactDetails.flatMap(_.addressLine1),
      addressLine2 = consigneeDetails.contactDetails.flatMap(_.addressLine2),
      addressLine3 = consigneeDetails.contactDetails.flatMap(_.addressLine3),
      street = Street.fromLines(
        consigneeDetails.contactDetails.flatMap(_.addressLine1),
        consigneeDetails.contactDetails.flatMap(_.addressLine2)
      ),
      city = consigneeDetails.contactDetails.flatMap(_.addressLine3),
      countryCode = consigneeDetails.contactDetails.flatMap(_.countryCode),
      postalCode = consigneeDetails.contactDetails.flatMap(_.postalCode),
      telephoneNumber = consigneeDetails.contactDetails.flatMap(_.telephone),
      faxNumber = None,
      emailAddress = consigneeDetails.contactDetails.flatMap(_.emailAddress)
    )

  implicit val format: OFormat[ContactInformation] = Json.format[ContactInformation]
}
