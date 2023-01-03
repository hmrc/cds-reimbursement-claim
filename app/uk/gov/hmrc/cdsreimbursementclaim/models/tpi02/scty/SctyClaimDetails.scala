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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.cdsreimbursementclaim.models.CaseStatus
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi02.Reimbursement

final case class SctyClaimDetails(
  CDFPayCaseNumber: String,
  declarationID: Option[String],
  reasonForSecurity: String,
  procedureCode: String,
  caseStatus: String,
  caseSubStatus: Option[String],
  goods: Option[Seq[Goods]],
  declarantEORI: String,
  importerEORI: String,
  claimantEORI: Option[String],
  totalCustomsClaimAmount: Option[String],
  totalVATClaimAmount: Option[String],
  totalClaimAmount: Option[String],
  totalReimbursementAmount: Option[String],
  claimStartDate: String,
  claimantName: Option[String],
  claimantEmailAddress: Option[String],
  closedDate: Option[String],
  reimbursements: Option[Seq[Reimbursement]]
)

object SctyClaimDetails {
  implicit val format: OFormat[SctyClaimDetails] = Json.format[SctyClaimDetails]

  def fromTpi02Response(caseDetails: SCTYCase): SctyClaimDetails =
    SctyClaimDetails(
      CDFPayCaseNumber = caseDetails.CDFPayCaseNumber,
      declarationID = caseDetails.declarationID,
      reasonForSecurity = caseDetails.reasonForSecurity,
      procedureCode = caseDetails.procedureCode,
      caseStatus = CaseStatus.transformedCaseStatusScty(caseDetails.caseStatus),
      caseSubStatus = CaseStatus.caseSubStatusScty(caseDetails.caseStatus),
      goods = caseDetails.goods.map(_.map(g => Goods(g.itemNumber, g.goodsDescription))),
      declarantEORI = caseDetails.declarantEORI,
      importerEORI = caseDetails.importerEORI,
      claimantEORI = caseDetails.claimantEORI,
      totalCustomsClaimAmount = caseDetails.totalCustomsClaimAmount,
      totalVATClaimAmount = caseDetails.totalVATClaimAmount,
      totalClaimAmount = caseDetails.totalClaimAmount,
      totalReimbursementAmount = caseDetails.totalReimbursementAmount,
      claimStartDate = caseDetails.claimStartDate,
      claimantName = caseDetails.claimantName,
      claimantEmailAddress = caseDetails.claimantEmailAddress,
      closedDate = caseDetails.closedDate,
      reimbursements = caseDetails.reimbursement.map(
        _.map(r =>
          Reimbursement(
            r.reimbursementDate,
            r.reimbursementAmount,
            r.taxType,
            r.reimbursementMethod
          )
        )
      )
    )

}
