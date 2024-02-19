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

package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import cats.implicits.catsSyntaxEq
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ReimbursementMethodAnswer.CurrentMonthAdjustment
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.CaseType.{CMA, Individual}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.DeclarationMode.ParentDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{CaseType, DeclarationMode}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.BankAccountDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.MapFormat

import java.time.LocalDate
import scala.collection.immutable

final case class SingleRejectedGoodsClaim(
  movementReferenceNumber: MRN,
  claimantType: ClaimantType,
  payeeType: PayeeType,
  claimantInformation: ClaimantInformation,
  basisOfClaim: BasisOfRejectedGoodsClaim,
  basisOfClaimSpecialCircumstances: Option[String],
  methodOfDisposal: MethodOfDisposal,
  detailsOfRejectedGoods: String,
  inspectionDate: LocalDate,
  inspectionAddress: InspectionAddress,
  reimbursements: Seq[Reimbursement],
  reimbursementMethod: ReimbursementMethodAnswer,
  bankAccountDetails: Option[BankAccountDetails],
  supportingEvidences: immutable.Seq[EvidenceDocument]
) extends RejectedGoodsClaim {

  override def totalReimbursementAmount: BigDecimal =
    reimbursements.map(_.amount).sum

  override def leadMrn: MRN = movementReferenceNumber

  override def getClaimsOverMrns: List[(MRN, Map[TaxCode, BigDecimal])] =
    (movementReferenceNumber, reimbursements.map(r => (r.taxCode, r.amount)).toMap) :: Nil

  override def caseType: CaseType = if (reimbursementMethod === CurrentMonthAdjustment) CMA else Individual

  override def declarationMode: DeclarationMode = ParentDeclaration

  override def documents: immutable.Seq[EvidenceDocument] = supportingEvidences
}

object SingleRejectedGoodsClaim {

  implicit val reimbursementClaimsFormat: Format[Map[TaxCode, BigDecimal]] =
    MapFormat[TaxCode, BigDecimal]

  implicit val format: Format[SingleRejectedGoodsClaim] = Json.format[SingleRejectedGoodsClaim]
}
