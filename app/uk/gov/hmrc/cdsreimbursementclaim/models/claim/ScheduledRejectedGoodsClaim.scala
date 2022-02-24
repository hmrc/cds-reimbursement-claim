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

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.CaseType.Bulk
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.DeclarationMode.ParentDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{CaseType, DeclarationMode}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.BankAccountDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.MapFormat

import java.time.LocalDate

// TODO: Reflect a Frontend model once ready
final case class ScheduledRejectedGoodsClaim(
  movementReferenceNumber: MRN,
  claimantType: ClaimantType,
  claimantInformation: ClaimantInformation,
  basisOfClaim: BasisOfRejectedGoodsClaim,
  basisOfClaimSpecialCircumstances: Option[String],
  methodOfDisposal: MethodOfDisposal,
  detailsOfRejectedGoods: String,
  inspectionDate: LocalDate,
  inspectionAddress: InspectionAddress,
  reimbursementClaims: Map[TaxCode, BigDecimal],
  reimbursementMethod: ReimbursementMethodAnswer,
  bankAccountDetails: Option[BankAccountDetails],
  supportingEvidences: Seq[EvidenceDocument],
  scheduledDocument: EvidenceDocument
) extends RejectedGoodsClaim {

  override def totalReimbursementAmount: BigDecimal =
    reimbursementClaims.values.sum

  override def leadMrn: MRN = movementReferenceNumber

  override def getClaimsOverMrns: List[(MRN, Map[TaxCode, BigDecimal])] =
    (movementReferenceNumber, reimbursementClaims) :: Nil

  override def caseType: CaseType = Bulk

  override def declarationMode: DeclarationMode = ParentDeclaration

  override def documents: Seq[EvidenceDocument] =
    scheduledDocument +: supportingEvidences
}

object ScheduledRejectedGoodsClaim {

  implicit val reimbursementClaimsFormat: Format[Map[TaxCode, BigDecimal]] =
    MapFormat[TaxCode, BigDecimal]

  implicit val format: Format[ScheduledRejectedGoodsClaim] =
    Json.format[ScheduledRejectedGoodsClaim]
}
