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

import cats.implicits.catsSyntaxSemigroup
import cats.kernel.Semigroup
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.BankAccountDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.MapFormat

final case class ScheduledOverpaymentsClaim(
  movementReferenceNumber: MRN,
  claimantType: ClaimantType,
  claimantInformation: ClaimantInformation,
  basisOfClaim: BasisOfClaim,
  whetherNorthernIreland: Boolean,
  additionalDetails: String,
  reimbursementClaims: Map[String, Map[TaxCode, AmountPaidWithRefund]],
  reimbursementMethod: ReimbursementMethodAnswer,
  bankAccountDetails: Option[BankAccountDetails],
  scheduledDocument: EvidenceDocument,
  supportingEvidences: Seq[EvidenceDocument]
) {
  lazy val combinedReimbursementClaims: Map[TaxCode, AmountPaidWithRefund] =
    reimbursementClaims.values.reduceOption((x, y) => x |+| y).getOrElse(Map.empty)

  def documents: Seq[EvidenceDocument] = scheduledDocument +: supportingEvidences

  def totalReimbursementAmount: BigDecimal =
    reimbursementClaims.values.map(_.values.map(_.refundAmount).sum).sum

  def getClaimedReimbursements: List[ClaimedReimbursement] =
    combinedReimbursementClaims.toList
      .map { case (taxCode, reimbursement) =>
        ClaimedReimbursement(
          taxCode = taxCode,
          paidAmount = reimbursement.paidAmount,
          claimAmount = reimbursement.refundAmount
        )
      }

}

object ScheduledOverpaymentsClaim {

  implicit val semigroup: Semigroup[Map[TaxCode, AmountPaidWithRefund]] =
    (x: Map[TaxCode, AmountPaidWithRefund], y: Map[TaxCode, AmountPaidWithRefund]) =>
      (x.toSeq ++ y.toSeq)
        .groupBy(_._1)
        .mapValues(_.map(_._2).reduceOption(_ |+| _).getOrElse(AmountPaidWithRefund.empty))

  implicit val reimbursementClaimsFormat: Format[Map[TaxCode, AmountPaidWithRefund]] =
    MapFormat[TaxCode, AmountPaidWithRefund]

  implicit val format: Format[ScheduledOverpaymentsClaim] = Json.format[ScheduledOverpaymentsClaim]
}

final case class ScheduledOverpaymentsClaimRequest(claim: ScheduledOverpaymentsClaim)

object ScheduledOverpaymentsClaimRequest {
  implicit val format: Format[ScheduledOverpaymentsClaimRequest] = Json.format[ScheduledOverpaymentsClaimRequest]
}
