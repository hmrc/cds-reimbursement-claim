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

package uk.gov.hmrc.cdsreimbursementclaim.models.tpi02.ndrc

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.cdsreimbursementclaim.models.CaseStatus
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi02.Reimbursement

final case class NdrcClaimDetails(
  CDFPayCaseNumber: String,
  declarationID: Option[String],
  claimType: String, //C285 or C&E1179
  caseType: String, //if (C285) Individual, Bulk, CMA, C18 || if (C&E1179) Individual, Bulk, CMA
  caseStatus: String,
  caseSubStatus: Option[String],
  descOfGoods: Option[String],
  descOfRejectedGoods: Option[String],
  totalClaimAmount: Option[String],
  declarantEORI: String,
  importerEORI: String,
  claimantEORI: Option[String],
  basisOfClaim: Option[String],
  claimStartDate: String,
  claimantName: Option[String],
  claimantEmailAddress: Option[String],
  closedDate: Option[String],
  reimbursements: Option[Seq[Reimbursement]],
  MRNDetails:Option[Seq[ProcedureDetail]]
)

object NdrcClaimDetails {
  implicit val format: OFormat[NdrcClaimDetails] = Json.format[NdrcClaimDetails]

  def fromTpi02Response(ndrcCase: NDRCCase): NdrcClaimDetails =
    NdrcClaimDetails(
      CDFPayCaseNumber = ndrcCase.NDRCDetail.CDFPayCaseNumber,
      declarationID = ndrcCase.NDRCDetail.declarationID,
      claimType = ndrcCase.NDRCDetail.claimType,
      caseType = ndrcCase.NDRCDetail.caseType,
      caseStatus = CaseStatus.transformedCaseStatusNdrc(ndrcCase.NDRCDetail.caseStatus),
      caseSubStatus = CaseStatus.caseSubStatusNdrc(ndrcCase.NDRCDetail.caseStatus),
      descOfGoods = ndrcCase.NDRCDetail.descOfGoods,
      descOfRejectedGoods = ndrcCase.NDRCDetail.descOfRejectedGoods,
      totalClaimAmount = ndrcCase.NDRCAmounts.totalClaimAmount,
      declarantEORI = ndrcCase.NDRCDetail.declarantEORI,
      importerEORI = ndrcCase.NDRCDetail.importerEORI,
      claimantEORI = ndrcCase.NDRCDetail.claimantEORI,
      basisOfClaim = ndrcCase.NDRCDetail.basisOfClaim,
      claimStartDate = ndrcCase.NDRCDetail.claimStartDate,
      claimantName = ndrcCase.NDRCDetail.claimantName,
      claimantEmailAddress = ndrcCase.NDRCDetail.claimantEmailAddress,
      closedDate = ndrcCase.NDRCDetail.closedDate,
      reimbursements = ndrcCase.NDRCDetail.reimbursement.map(
        _.map(r =>
          Reimbursement(
            r.reimbursementDate,
            r.reimbursementAmount,
            r.taxType,
            r.reimbursementMethod
          )
        )
      ),
      MRNDetails = ndrcCase.NDRCDetail.MRNDetails
    )
}
