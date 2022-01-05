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
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.BankAccountDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.MapFormat

import java.time.LocalDate

final case class RejectedGoodsClaim(
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
  supportingEvidences: Seq[EvidenceDocument]
) {

  def totalReimbursementAmount: BigDecimal =
    reimbursementClaims.values.sum
}

object RejectedGoodsClaim {

  implicit val reimbursementClaimsFormat: Format[Map[TaxCode, BigDecimal]] =
    MapFormat[TaxCode, BigDecimal]

  implicit val format: Format[RejectedGoodsClaim] = Json.format[RejectedGoodsClaim]
}
