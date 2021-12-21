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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.Country

final case class Address(
  contactPerson: Option[String],
  addressLine1: Option[String],
  addressLine2: Option[String],
  AddressLine3: Option[String],
  street: Option[String],
  city: Option[String],
  countryCode: String,
  postalCode: Option[String],
  telephone: Option[String],
  emailAddress: Option[String]
)

object Address {

  def from(contactInformation: ContactInformation): Address =
    Address(
      contactPerson = contactInformation.contactPerson,
      addressLine1 = contactInformation.addressLine1,
      addressLine2 = contactInformation.addressLine2,
      AddressLine3 = contactInformation.addressLine3,
      street = contactInformation.street,
      city = contactInformation.city,
      countryCode = contactInformation.countryCode.getOrElse(Country.uk.code),
      postalCode = contactInformation.postalCode,
      telephone = contactInformation.telephoneNumber,
      emailAddress = contactInformation.emailAddress
    )

  implicit val format: OFormat[Address] = Json.format[Address]
}
