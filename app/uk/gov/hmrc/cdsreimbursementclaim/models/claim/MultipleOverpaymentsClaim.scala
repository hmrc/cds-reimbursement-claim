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
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.Reads.minLength
import play.api.libs.json.{Format, JsPath, Json}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.BankAccountDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.MapFormat

@SuppressWarnings(Array("org.wartremover.warts.TraversableOps", "org.wartremover.warts.IterableOps"))
final case class MultipleOverpaymentsClaim(
  movementReferenceNumbers: List[MRN],
  claimantType: ClaimantType,
  payeeType: PayeeType,
  claimantInformation: ClaimantInformation,
  basisOfClaim: BasisOfClaim,
  additionalDetails: String,
  reimbursementClaims: Map[MRN, Map[TaxCode, BigDecimal]],
  reimbursementMethod: ReimbursementMethodAnswer,
  bankAccountDetails: Option[BankAccountDetails],
  supportingEvidences: Seq[EvidenceDocument]
) extends OverpaymentsClaim {

  def leadMrn: MRN = movementReferenceNumbers.head

  def documents: Seq[EvidenceDocument] = supportingEvidences

  lazy val combinedReimbursementClaims: Map[TaxCode, BigDecimal] =
    reimbursementClaims.values.reduceOption((x, y) => x |+| y).getOrElse(Map.empty)

  def totalReimbursementAmount: BigDecimal =
    reimbursementClaims.flatMap(_._2.values).sum

  override def bankAccountDetailsAnswer: Option[BankAccountDetails] = bankAccountDetails
}

object MultipleOverpaymentsClaim {

  implicit lazy val reimbursementFormat: Format[Map[TaxCode, BigDecimal]] =
    MapFormat[TaxCode, BigDecimal]

  implicit lazy val mrnsReimbursementFormat: Format[Map[MRN, Map[TaxCode, BigDecimal]]] =
    MapFormat[MRN, Map[TaxCode, BigDecimal]]

  implicit val format: Format[MultipleOverpaymentsClaim] =
    Format(
      (
        (JsPath \ "movementReferenceNumbers").read[List[MRN]](minLength[List[MRN]](2)) and
          (JsPath \ "claimantType").read[ClaimantType] and
          (JsPath \ "payeeType").read[PayeeType] and
          (JsPath \ "claimantInformation").read[ClaimantInformation] and
          (JsPath \ "basisOfClaim").read[BasisOfClaim] and
          (JsPath \ "additionalDetails").read[String] and
          (JsPath \ "reimbursementClaims").read[Map[MRN, Map[TaxCode, BigDecimal]]] and
          (JsPath \ "reimbursementMethod").read[ReimbursementMethodAnswer] and
          (JsPath \ "bankAccountDetails").readNullable[BankAccountDetails] and
          (JsPath \ "supportingEvidences").read[Seq[EvidenceDocument]]
      )(MultipleOverpaymentsClaim(_, _, _, _, _, _, _, _, _, _)),
      (
        (JsPath \ "movementReferenceNumbers").write[List[MRN]] and
          (JsPath \ "claimantType").write[ClaimantType] and
          (JsPath \ "payeeType").write[PayeeType] and
          (JsPath \ "claimantInformation").write[ClaimantInformation] and
          (JsPath \ "basisOfClaim").write[BasisOfClaim] and
          (JsPath \ "additionalDetails").write[String] and
          (JsPath \ "reimbursementClaims").write[Map[MRN, Map[TaxCode, BigDecimal]]] and
          (JsPath \ "reimbursementMethod").write[ReimbursementMethodAnswer] and
          (JsPath \ "bankAccountDetails").writeNullable[BankAccountDetails] and
          (JsPath \ "supportingEvidences").write[Seq[EvidenceDocument]]
      )(unlift(MultipleOverpaymentsClaim.unapply))
    )
}

final case class MultipleOverpaymentsClaimRequest(claim: MultipleOverpaymentsClaim)

object MultipleOverpaymentsClaimRequest {
  implicit val format: Format[MultipleOverpaymentsClaimRequest] = Json.format[MultipleOverpaymentsClaimRequest]
}
