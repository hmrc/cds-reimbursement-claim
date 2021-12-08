/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.{Eori, MRN}

import java.time.LocalDate

final case class CE1779Claim(
  movementReferenceNumber: MRN,
  declarantType: DeclarantTypeAnswer,
  basisOfClaim: BasisOfRejectedGoodsClaim,
  methodOfDisposal: MethodOfDisposal,
  detailsOfRejectedGoods: String,
  inspectionDate: LocalDate,
  inspectionAddress: InspectionAddress,
  reimbursementClaims: Map[TaxCode, BigDecimal],
  supportingEvidences: Map[UploadDocument, DocumentTypeRejectedGoods],
  basisOfClaimSpecialCircumstances: Option[String],
  reimbursementMethod: ReimbursementMethodAnswer,
  consigneeEoriNumber: Eori,
  declarantEoriNumber: Eori,
  contactDetails: MrnContactDetails,
  contactAddress: ContactAddress,
  bankAccountDetailsAndType: Option[(BankAccountDetails, BankAccountType)]
) extends ReimbursementClaim

object CE1779Claim {

  implicit val format: Format[CE1779Claim] = Json.format[CE1779Claim]
}
