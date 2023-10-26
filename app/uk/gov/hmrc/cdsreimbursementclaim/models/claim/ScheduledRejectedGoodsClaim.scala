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

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.CaseType.Bulk
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.DeclarationMode.ParentDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{CaseType, DeclarationMode}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.BankAccountDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.MapFormat
import cats.implicits.catsSyntaxSemigroup

import java.time.LocalDate
import cats.kernel.Semigroup

import scala.collection.immutable

final case class ScheduledRejectedGoodsClaim(
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
  reimbursementClaims: Map[String, Map[TaxCode, AmountPaidWithRefund]],
  reimbursementMethod: ReimbursementMethodAnswer,
  bankAccountDetails: Option[BankAccountDetails],
  supportingEvidences: immutable.Seq[EvidenceDocument],
  scheduledDocument: EvidenceDocument
) extends RejectedGoodsClaim {

  override def totalReimbursementAmount: BigDecimal =
    reimbursementClaims.values.map(_.values.map(_.refundAmount).sum).sum

  override def leadMrn: MRN = movementReferenceNumber

  lazy val combinedReimbursementClaims: Map[TaxCode, AmountPaidWithRefund] =
    reimbursementClaims.values.reduceOption((x, y) => x |+| y).getOrElse(Map.empty)

  override def getClaimsOverMrns: List[(MRN, Map[TaxCode, BigDecimal])] =
    (movementReferenceNumber, combinedReimbursementClaims.view.mapValues(_.refundAmount).toMap) :: Nil

  def getClaimedReimbursements: List[ClaimedReimbursement] =
    combinedReimbursementClaims.toList
      .map { case (taxCode, reimbursement) =>
        ClaimedReimbursement(
          taxCode = taxCode,
          paidAmount = reimbursement.paidAmount,
          claimAmount = reimbursement.refundAmount
        )
      }

  override def caseType: CaseType = Bulk

  override def declarationMode: DeclarationMode = ParentDeclaration

  override def documents: immutable.Seq[EvidenceDocument] =
    scheduledDocument +: supportingEvidences
}

object ScheduledRejectedGoodsClaim {

  implicit val semigroup: Semigroup[Map[TaxCode, AmountPaidWithRefund]] =
    (x: Map[TaxCode, AmountPaidWithRefund], y: Map[TaxCode, AmountPaidWithRefund]) =>
      (x.toSeq ++ y.toSeq)
        .groupBy(_._1)
        .view
        .mapValues(_.map(_._2).reduceOption(_ |+| _).getOrElse(AmountPaidWithRefund.empty))
        .toMap

  implicit val reimbursementClaimsFormat: Format[Map[TaxCode, AmountPaidWithRefund]] =
    MapFormat[TaxCode, AmountPaidWithRefund]

  implicit val format: Format[ScheduledRejectedGoodsClaim] =
    Json.format[ScheduledRejectedGoodsClaim]
}
