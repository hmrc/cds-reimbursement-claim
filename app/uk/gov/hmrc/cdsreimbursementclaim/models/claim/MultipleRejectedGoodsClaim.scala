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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{Format, JsPath}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{CaseType, DeclarationMode}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.CaseType.Bulk
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.DeclarationMode.AllDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.BankAccountDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.MapFormat
import java.time.LocalDate
import play.api.libs.json.Reads.minLength

@SuppressWarnings(Array("org.wartremover.warts.TraversableOps", "org.wartremover.warts.IterableOps"))
final case class MultipleRejectedGoodsClaim(
  movementReferenceNumbers: List[MRN],
  claimantType: ClaimantType,
  payeeType: PayeeType,
  claimantInformation: ClaimantInformation,
  basisOfClaim: BasisOfRejectedGoodsClaim,
  basisOfClaimSpecialCircumstances: Option[String],
  methodOfDisposal: MethodOfDisposal,
  detailsOfRejectedGoods: String,
  inspectionDate: LocalDate,
  inspectionAddress: InspectionAddress,
  reimbursementClaims: Map[MRN, Map[TaxCode, BigDecimal]],
  reimbursementMethod: ReimbursementMethodAnswer,
  bankAccountDetails: Option[BankAccountDetails],
  supportingEvidences: Seq[EvidenceDocument]
) extends RejectedGoodsClaim {
  override def leadMrn: MRN = movementReferenceNumbers.head

  override def totalReimbursementAmount: BigDecimal =
    reimbursementClaims.flatMap(_._2.values).sum

  override def getClaimsOverMrns: List[(MRN, Map[TaxCode, BigDecimal])] =
    reimbursementClaims.toList

  override def caseType: CaseType = Bulk

  override def declarationMode: DeclarationMode = AllDeclaration

  override def documents: Seq[EvidenceDocument] = supportingEvidences
}

object MultipleRejectedGoodsClaim {

  implicit lazy val reimbursementFormat: Format[Map[TaxCode, BigDecimal]] =
    MapFormat[TaxCode, BigDecimal]

  implicit lazy val mrnsReimbursementFormat: Format[Map[MRN, Map[TaxCode, BigDecimal]]] =
    MapFormat[MRN, Map[TaxCode, BigDecimal]]

  implicit val format: Format[MultipleRejectedGoodsClaim] =
    Format(
      (
        (JsPath \ "movementReferenceNumbers").read[List[MRN]](minLength[List[MRN]](2)) and
          (JsPath \ "claimantType").read[ClaimantType] and
          (JsPath \ "payeeType").read[PayeeType] and
          (JsPath \ "claimantInformation").read[ClaimantInformation] and
          (JsPath \ "basisOfClaim").read[BasisOfRejectedGoodsClaim] and
          (JsPath \ "basisOfClaimSpecialCircumstances").readNullable[String] and
          (JsPath \ "methodOfDisposal").read[MethodOfDisposal] and
          (JsPath \ "detailsOfRejectedGoods").read[String] and
          (JsPath \ "inspectionDate").read[LocalDate] and
          (JsPath \ "inspectionAddress").read[InspectionAddress] and
          (JsPath \ "reimbursementClaims").read[Map[MRN, Map[TaxCode, BigDecimal]]] and
          (JsPath \ "reimbursementMethod").read[ReimbursementMethodAnswer] and
          (JsPath \ "bankAccountDetails").readNullable[BankAccountDetails] and
          (JsPath \ "supportingEvidences").read[Seq[EvidenceDocument]]
      )(MultipleRejectedGoodsClaim(_, _, _, _, _, _, _, _, _, _, _, _, _, _)),
      (
        (JsPath \ "movementReferenceNumbers").write[List[MRN]] and
          (JsPath \ "claimantType").write[ClaimantType] and
          (JsPath \ "payeeType").write[PayeeType] and
          (JsPath \ "claimantInformation").write[ClaimantInformation] and
          (JsPath \ "basisOfClaim").write[BasisOfRejectedGoodsClaim] and
          (JsPath \ "basisOfClaimSpecialCircumstances").writeNullable[String] and
          (JsPath \ "methodOfDisposal").write[MethodOfDisposal] and
          (JsPath \ "detailsOfRejectedGoods").write[String] and
          (JsPath \ "inspectionDate").write[LocalDate] and
          (JsPath \ "inspectionAddress").write[InspectionAddress] and
          (JsPath \ "reimbursementClaims").write[Map[MRN, Map[TaxCode, BigDecimal]]] and
          (JsPath \ "reimbursementMethod").write[ReimbursementMethodAnswer] and
          (JsPath \ "bankAccountDetails").writeNullable[BankAccountDetails] and
          (JsPath \ "supportingEvidences").write[Seq[EvidenceDocument]]
      )(Tuple.fromProductTyped(_))
    )
}
