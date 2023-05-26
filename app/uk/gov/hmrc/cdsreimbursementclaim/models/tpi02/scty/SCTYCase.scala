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

package uk.gov.hmrc.cdsreimbursementclaim.models.tpi02.scty

import uk.gov.hmrc.cdsreimbursementclaim.models.tpi02.Reimbursement
import play.api.libs.json.{Json, OFormat}

final case class SCTYCase(
  CDFPayCaseNumber: String,
  declarationID: Option[String],
  reasonForSecurity: String,
  procedureCode: String,
  caseStatus: String,
  goods: Option[Seq[Goods]],
  declarantEORI: String,
  importerEORI: Option[String],
  claimantEORI: Option[String],
  totalCustomsClaimAmount: Option[String],
  totalVATClaimAmount: Option[String],
  totalClaimAmount: Option[String],
  totalReimbursementAmount: Option[String],
  claimStartDate: Option[String],
  claimantName: Option[String],
  claimantEmailAddress: Option[String],
  closedDate: Option[String],
  reimbursement: Option[Seq[Reimbursement]]
)

object SCTYCase {
  implicit val format: OFormat[SCTYCase] = Json.format[SCTYCase]
}
