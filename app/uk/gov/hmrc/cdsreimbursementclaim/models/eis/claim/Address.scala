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
)

object Address {
  def fromContactInformation(contactInformation: ContactInformation): Either[CdsError, Address] =
    contactInformation.countryCode
      .toRight(CdsError("country code is mandatory"))
      .map { countryCode =>
        Address(
          contactPerson = contactInformation.contactPerson,
          addressLine1 = contactInformation.addressLine1,
          addressLine2 = contactInformation.addressLine2,
          addressLine3 = contactInformation.addressLine3,
          street = contactInformation.street,
          city = contactInformation.city,
          countryCode = countryCode,
          postalCode = contactInformation.postalCode,
          telephoneNumber = contactInformation.telephoneNumber,
          emailAddress = contactInformation.emailAddress
        )
      }

  def fromEstablishmentAddress(establishmentAddress: EstablishmentAddress): Address =
    Address(
      contactPerson = None,
      addressLine1 = Some(establishmentAddress.addressLine1),
      addressLine2 = establishmentAddress.addressLine2,
      addressLine3 = establishmentAddress.addressLine3,
      street = None,
      city = None,
      countryCode = establishmentAddress.countryCode,
      postalCode = establishmentAddress.postalCode,
      telephoneNumber = None,
      emailAddress = None
    )

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit val format: OFormat[Address] = Json.format[Address]
}
