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

import cats.data.NonEmptyList
import cats.implicits.catsSyntaxTuple2Semigroupal
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.ContactInformation
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.{BankAccountDetails, ConsigneeDetails, DeclarantDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.{BigDecimalOps, MapFormat}

import java.util.UUID

final case class SingleOverpaymentsClaim(
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
  supportingEvidences: Seq[EvidenceDocument]
)

object SingleOverpaymentsClaim {
  implicit val format: Format[SingleOverpaymentsClaim] = Json.format[SingleOverpaymentsClaim]
  implicit val reimbursementClaimsFormat: Format[Map[TaxCode, BigDecimal]] =
    MapFormat[TaxCode, BigDecimal]
}

final case class SingleOverpaymentsClaimRequest(claim: SingleOverpaymentsClaim)

object SingleOverpaymentsClaimRequest {
  implicit val format: Format[SingleOverpaymentsClaimRequest] = Json.format[SingleOverpaymentsClaimRequest]
}