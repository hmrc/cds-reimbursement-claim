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
  val empty: Address                    = Address(
    Some("No contact person"),
    Some("No line 1"),
    Some("No line 2"),
    Some("No line 3"),
    Some("No street"),
    Some("No city"),
    "GB",
    Some("None"),
    Some("No telephone"),
    Some("No email")
  )
  implicit val format: OFormat[Address] = Json.format[Address]
}
