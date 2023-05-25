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

package uk.gov.hmrc.cdsreimbursementclaim.models.tpi01.ndrc

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.cdsreimbursementclaim.models.CaseStatus
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi01.{ClaimItem, ClaimTransformer, NDRCCaseDetails, ResponseDetail}
import collection.immutable.Seq

/** Model of an NDRC type of claim data returned from /claims endpoint */
final case class NdrcClaimItem(
  CDFPayCaseNumber: String,
  declarationID: Option[String],
  claimStartDate: String,
  closedDate: Option[String],
  caseStatus: String,
  caseSubStatus: Option[String],
  declarantEORI: String,
  importerEORI: String,
  claimantEORI: Option[String],
  totalCustomsClaimAmount: Option[String],
  totalVATClaimAmount: Option[String],
  totalExciseClaimAmount: Option[String],
  declarantReferenceNumber: Option[String],
  basisOfClaim: Option[String]
) extends ClaimItem

@SuppressWarnings(Array("org.wartremover.warts.Any"))
object NdrcClaimItem extends ClaimTransformer[NDRCCaseDetails, NdrcClaimItem] {
  implicit val format: OFormat[NdrcClaimItem]                     = Json.format[NdrcClaimItem]
  def convert(responseDetail: ResponseDetail): Seq[NdrcClaimItem] =
    super.convert(responseDetail, _.NDRCCases)

  override def fromTpi01Response(caseDetails: NDRCCaseDetails): NdrcClaimItem =
    NdrcClaimItem(
      CDFPayCaseNumber = caseDetails.CDFPayCaseNumber,
      declarationID = caseDetails.declarationID,
      claimStartDate = caseDetails.claimStartDate,
      closedDate = caseDetails.closedDate,
      caseStatus = CaseStatus.transformedCaseStatusNdrc(caseDetails.caseStatus),
      caseSubStatus = CaseStatus.caseSubStatusNdrc(caseDetails.caseStatus),
      declarantEORI = caseDetails.declarantEORI,
      importerEORI = caseDetails.importerEORI,
      claimantEORI = caseDetails.claimantEORI,
      totalCustomsClaimAmount = caseDetails.totalCustomsClaimAmount,
      totalVATClaimAmount = caseDetails.totalVATClaimAmount,
      totalExciseClaimAmount = caseDetails.totalExciseClaimAmount,
      declarantReferenceNumber = caseDetails.declarantReferenceNumber,
      basisOfClaim = caseDetails.basisOfClaim
    )
}
