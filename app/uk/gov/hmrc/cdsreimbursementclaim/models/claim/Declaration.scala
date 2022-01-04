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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.{BankDetails, ConsigneeDetails, DeclarantDetails, NdrcDetails}

final case class Declaration(
  declarantId: String,
  acceptanceDate: String,
  declarantDetails: DeclarantDetails,
  consigneeDetails: Option[ConsigneeDetails],
  maskedBankDetails: Option[BankDetails],
  securityDetails: Option[List[SecurityDetails]],
  ndrcDetails: Option[List[NdrcDetails]]
)

object Declaration {

  implicit val format: OFormat[Declaration] = Json.format[Declaration]
}
