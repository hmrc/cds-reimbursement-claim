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
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori

final case class EORIInformation(
  EORINumber: Eori,
  CDSFullName: Option[String],
  CDSEstablishmentAddress: Address,
  contactInformation: Option[ContactInformation],
  legalEntityType: Option[String] = None,
  EORIStartDate: Option[String] = None,
  VATDetails: Option[List[VATDetail]] = None
)

object EORIInformation {
  implicit val format: OFormat[EORIInformation] = Json.format[EORIInformation]
}
