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
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.EstablishmentAddress
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error => CdsError}
import play.api.Logger

final case class Address(
  contactPerson: Option[String],
  addressLine1: Option[String],
  addressLine2: Option[String],
  addressLine3: Option[String],
  street: Option[String],
  city: Option[String],
  countryCode: String,
  postalCode: Option[String],
  telephoneNumber: Option[String],
  emailAddress: Option[String]
) {

  def validateLength(name: String, valueOpt: Option[String], maxLengthInc: Int): Unit =
    valueOpt.foreach(value =>
      if (value.length() > maxLengthInc)
        Logger(this.getClass).warn(
          s"Address' property $name value is ${value.length()} long but only $maxLengthInc allowed: $value"
        )
    )

  validateLength("addressLine1", addressLine1, 35)
  validateLength("addressLine2", addressLine2, 35)
  validateLength("addressLine3", addressLine3, 35)
  validateLength("street", street, 70)
  validateLength("city", city, 35)
  validateLength("emailAddress", emailAddress, 241)
  validateLength("postalCode", postalCode, 9)
}

object Address {
  def fromContactInformation(contactInformation: ContactInformation): Either[CdsError, Address] =
    contactInformation.countryCode
      .toRight(CdsError("country code is mandatory"))
      .map { countryCode =>
        Address(
          contactPerson = contactInformation.contactPerson.noneIfEmpty,
          addressLine1 = contactInformation.addressLine1.noneIfEmpty,
          addressLine2 = contactInformation.addressLine2.noneIfEmpty,
          addressLine3 = contactInformation.addressLine3.noneIfEmpty,
          street = contactInformation.street.noneIfEmpty,
          city = contactInformation.city.noneIfEmpty,
          countryCode = countryCode,
          postalCode = contactInformation.postalCode.noneIfEmpty,
          telephoneNumber = contactInformation.telephoneNumber.noneIfEmpty,
          emailAddress = contactInformation.emailAddress.noneIfEmpty
        )
      }

  def fromEstablishmentAddress(establishmentAddress: EstablishmentAddress): Address =
    Address(
      contactPerson = None,
      addressLine1 = Some(establishmentAddress.addressLine1),
      addressLine2 = establishmentAddress.addressLine2.noneIfEmpty,
      addressLine3 = establishmentAddress.addressLine3.noneIfEmpty,
      street = None,
      city = None,
      countryCode = establishmentAddress.countryCode,
      postalCode = establishmentAddress.postalCode.noneIfEmpty,
      telephoneNumber = None,
      emailAddress = None
    )

  implicit val format: OFormat[Address] = Json.format[Address]

  extension (opt: Option[String]) {
    def noneIfEmpty: Option[String] = opt.match {
      case Some(value) if value.isBlank() => None
      case _                              => opt
    }
  }
}
