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

package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import cats.Eq
import play.api.libs.json.{Format, Json}

final case class InspectionAddress(
  addressLine1: String,
  addressLine2: Option[String] = None,
  addressLine3: Option[String] = None,
  city: Option[String] = None,
  postalCode: Postcode,
  addressType: InspectionAddressType
)

object InspectionAddress {

  implicit val equality: Eq[InspectionAddress] =
    Eq.fromUniversalEquals[InspectionAddress]

  implicit val format: Format[InspectionAddress] =
    Json.format[InspectionAddress]
}
