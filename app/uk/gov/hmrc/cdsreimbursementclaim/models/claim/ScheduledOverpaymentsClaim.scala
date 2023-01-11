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
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.BankAccountDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.MapFormat

final case class ScheduledOverpaymentsClaim(
  movementReferenceNumber: MRN,
  duplicateMovementReferenceNumber: Option[MRN],
  claimantType: ClaimantType,
  claimantInformation: ClaimantInformation,
  basisOfClaim: BasisOfClaim,
  whetherNorthernIreland: Boolean,
  additionalDetails: String,
  reimbursementClaims: Map[TaxCode, BigDecimal],
  reimbursementMethod: ReimbursementMethodAnswer,
  bankAccountDetails: Option[BankAccountDetails],
  supportingEvidences: Seq[EvidenceDocument],
  scheduledDocument: EvidenceDocument
) {
  def documents: Seq[EvidenceDocument] = scheduledDocument +: supportingEvidences
}

object ScheduledOverpaymentsClaim {
  implicit val reimbursementClaimsFormat: Format[Map[TaxCode, BigDecimal]] =
    MapFormat[TaxCode, BigDecimal]
  implicit val format: Format[ScheduledOverpaymentsClaim]                  = Json.format[ScheduledOverpaymentsClaim]
}

final case class ScheduledOverpaymentsClaimRequest(claim: ScheduledOverpaymentsClaim)

object ScheduledOverpaymentsClaimRequest {
  implicit val format: Format[ScheduledOverpaymentsClaimRequest] = Json.format[ScheduledOverpaymentsClaimRequest]
}
