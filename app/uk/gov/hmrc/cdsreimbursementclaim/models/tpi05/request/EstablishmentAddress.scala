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

package uk.gov.hmrc.cdsreimbursementclaim.models.tpi05.request

import play.api.libs.json.{Json, OFormat}

final case class EstablishmentAddress(
  contactPerson: Option[String],
  addressline1: Option[String],
  addressline2: Option[String],
  addressline3: Option[String],
  street: Option[String],
  city: Option[String],
  countryCode: Option[String],
  postalCode: Option[String],
  telephone: Option[String],
  emailAddress: Option[String]
)

object EstablishmentAddress {
  implicit val establishmentAddressFormat: OFormat[EstablishmentAddress] = Json.format

}
