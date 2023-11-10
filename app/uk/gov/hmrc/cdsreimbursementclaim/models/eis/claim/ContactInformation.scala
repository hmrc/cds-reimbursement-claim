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

import play.api.Logger
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ContactAddress, ContactDetails, Street}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.ClaimantDetails

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
) {

  def validateLength(name: String, valueOpt: Option[String], maxLengthInc: Int): Unit =
    valueOpt.foreach(value =>
      if (value.length() > maxLengthInc)
        Logger(this.getClass).warn(
          s"ContactInformation's property $name value is ${value.length()} long but only $maxLengthInc allowed: $value"
        )
    )

  validateLength("addressLine1", addressLine1, 35)
  validateLength("addressLine2", addressLine2, 35)
  validateLength("addressLine3", addressLine3, 35)
  validateLength("street", street, 70)
  validateLength("city", city, 35)
  validateLength("emailAddress", emailAddress, 241)
  validateLength("postalCode", postalCode, 9)

  def withoutDuplicateAddressLines: ContactInformation =
    this.copy(
      addressLine1 = Street.line1(addressLine1, addressLine2),
      addressLine2 = Street.line2(addressLine1, addressLine2)
    )
}

object ContactInformation {

  def apply(contactDetails: ContactDetails, contactAddress: ContactAddress): ContactInformation =
    new ContactInformation(
      contactPerson = Option(contactDetails.fullName),
      addressLine1 = Street.line1(Option(contactAddress.line1), contactAddress.line2),
      addressLine2 = Street.line2(Option(contactAddress.line1), contactAddress.line2),
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
      addressLine1 = maybeContactDetails.flatMap(x => Street.line1(x.addressLine1, x.addressLine2)),
      addressLine2 = maybeContactDetails.flatMap(x => Street.line2(x.addressLine1, x.addressLine2)),
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

  implicit val format: OFormat[ContactInformation] = Json.format[ContactInformation]
}
