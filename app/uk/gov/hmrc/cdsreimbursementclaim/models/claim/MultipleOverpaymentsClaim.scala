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

@SuppressWarnings(Array("org.wartremover.warts.TraversableOps", "org.wartremover.warts.IterableOps"))
final case class MultipleOverpaymentsClaim(
  movementReferenceNumbers: List[MRN],
  claimantType: ClaimantType,
  claimantInformation: ClaimantInformation,
  basisOfClaim: BasisOfClaim,
  whetherNorthernIreland: Boolean,
  additionalDetails: String,
  reimbursementClaims: Map[String, Map[TaxCode, AmountPaidWithCorrect]],
  reimbursementMethod: ReimbursementMethodAnswer,
  bankAccountDetails: Option[BankAccountDetails],
  supportingEvidences: Seq[EvidenceDocument]
) {

  def leadMrn: MRN = movementReferenceNumbers.head

  lazy val combinedReimbursementClaims: Map[TaxCode, AmountPaidWithCorrect] =
    reimbursementClaims.values.reduceOption((x, y) => x |+| y).getOrElse(Map.empty)

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

object MultipleOverpaymentsClaim {

  implicit val semigroup: Semigroup[Map[TaxCode, AmountPaidWithCorrect]] =
    (x: Map[TaxCode, AmountPaidWithCorrect], y: Map[TaxCode, AmountPaidWithCorrect]) =>
      (x.toSeq ++ y.toSeq)
        .groupBy(_._1)
        .mapValues(_.map(_._2).reduceOption(_ |+| _).getOrElse(AmountPaidWithCorrect.empty))

  implicit val reimbursementClaimsFormat: Format[Map[TaxCode, AmountPaidWithCorrect]] =
    MapFormat[TaxCode, AmountPaidWithCorrect]

  implicit val format: Format[MultipleOverpaymentsClaim] = Json.format[MultipleOverpaymentsClaim]
}

final case class MultipleOverpaymentsClaimRequest(claim: MultipleOverpaymentsClaim)

object MultipleOverpaymentsClaimRequest {
  implicit val format: Format[MultipleOverpaymentsClaimRequest] = Json.format[MultipleOverpaymentsClaimRequest]
}
