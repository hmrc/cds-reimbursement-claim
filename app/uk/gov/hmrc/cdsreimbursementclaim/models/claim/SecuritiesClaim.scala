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

import play.api.libs.json.Format
import play.api.libs.json.Json
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.TemporaryAdmissionMethodOfDisposal
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.BankAccountDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.MapFormat

final case class SecuritiesClaim(
  movementReferenceNumber: MRN,
  claimantType: ClaimantType,
  claimantInformation: ClaimantInformation,
  reasonForSecurity: ReasonForSecurity,
  securitiesReclaims: Map[String, Map[TaxCode, BigDecimal]],
  bankAccountDetails: Option[BankAccountDetails],
  supportingEvidences: Seq[EvidenceDocument],
  temporaryAdmissionMethodOfDisposal: Option[TemporaryAdmissionMethodOfDisposal],
  exportMovementReferenceNumber: Option[MRN]
)

object SecuritiesClaim {

  implicit val taxCodeToBigDecimalMapFormat: Format[Map[TaxCode, BigDecimal]] =
    MapFormat[TaxCode, BigDecimal]

  implicit val securitiesReclaimsMapFormat: Format[Map[String, Map[TaxCode, BigDecimal]]] =
    MapFormat[String, Map[TaxCode, BigDecimal]]

  implicit val format: Format[SecuritiesClaim] =
    Json.format[SecuritiesClaim]
}
