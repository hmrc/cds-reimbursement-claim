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

package uk.gov.hmrc.cdsreimbursementclaim.models.tpi02.scty

import play.api.libs.json.{Json, OFormat}
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
      caseStatus = transformedCaseStatus(caseDetails.caseStatus),
      caseSubStatus = caseSubStatus(caseDetails.caseStatus),
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

  def caseSubStatus(caseStatus: String): Option[String] = caseStatus match {
    case "Resolved-Refund"            => Some("Resolved-Refund")
    case "Resolved-Manual BTA"        => Some("Resolved-Manual BTA")
    case "Closed-C18 Raised"          => Some("Closed-C18 Raised")
    case "Resolved-Auto BTA"          => Some("Resolved-Auto BTA")
    case "Resolved-Manual BTA/Refund" => Some("Resolved-Manual BTA/Refund")
    case "Resolved-Withdrawn"         => Some("Resolved-Withdrawn")
    case _                            => None
  }

  def transformedCaseStatus(caseStatus: String): String =
    caseStatus match {
      case "Open"                              => "In Progress"
      case "Pending-Approval"                  => "Pending"
      case "Pending-Payment"                   => "Pending"
      case "Partial Refund"                    => "Pending"
      case "Resolved-Refund"                   => "Closed"
      case "Pending-Query"                     => "Pending"
      case "Resolved-Manual BTA"               => "Closed"
      case "Pending-C18"                       => "Pending"
      case "Closed-C18 Raised"                 => "Closed"
      case "RTBH Letter Initiated"             => "Pending"
      case "Awaiting RTBH Letter Response"     => "Pending"
      case "Reminder Letter Initiated"         => "Pending"
      case "Awaiting Reminder Letter Response" => "Pending"
      case "Decision Letter Initiated"         => "Pending"
      case "Partial BTA"                       => "Pending"
      case "Partial BTA/Refund"                => "Pending"
      case "Resolved-Auto BTA"                 => "Closed"
      case "Resolved-Manual BTA/Refund"        => "Closed"
      case "Open-Extension Granted"            => "In Progress"
      case "Resolved-Withdrawn"                => "Closed"
    }
}
