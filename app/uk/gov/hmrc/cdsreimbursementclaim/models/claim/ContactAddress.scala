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

package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import cats.Eq
import julienrf.json.derived
import play.api.libs.json.OFormat

final case class ContactAddress(
  line1: String,
  line2: Option[String],
  line3: Option[String],
  line4: String,
  postcode: Postcode,
  country: Country
)

object ContactAddress {

  implicit val format: OFormat[ContactAddress] = derived.oformat[ContactAddress]()

  implicit val eq: Eq[ContactAddress] = Eq.fromUniversalEquals
}
