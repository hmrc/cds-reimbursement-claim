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

package uk.gov.hmrc.cdsreimbursementclaim.models.tpi01

import play.api.libs.json.{Json, OFormat}

final case class SCTYCaseDetails(
  CDFPayCaseNumber: String,
  declarationID: Option[String],
  claimStartDate: String,
  closedDate: Option[String],
  reasonForSecurity: String,
  caseStatus: String,
  declarantEORI: String,
  importerEORI: String,
  claimantEORI: Option[String],
  totalCustomsClaimAmount: Option[String],
  totalVATClaimAmount: Option[String],
  declarantReferenceNumber: Option[String]
) extends CaseDetails {

  def total: BigDecimal =
    totalCustomsClaimAmount.map(BigDecimal.apply).getOrElse(ZERO) +
      totalVATClaimAmount.map(BigDecimal.apply).getOrElse(ZERO)
}

object SCTYCaseDetails {
  implicit val format: OFormat[SCTYCaseDetails] = Json.format[SCTYCaseDetails]
}
