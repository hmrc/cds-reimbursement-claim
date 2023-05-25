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

import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{CaseType, DeclarationMode}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.BankAccountDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN

import java.time.LocalDate
import collection.immutable.Seq

trait RejectedGoodsClaim extends HasClaimantInformation {

  val claimantType: ClaimantType

  val claimantInformation: ClaimantInformation

  val basisOfClaim: BasisOfRejectedGoodsClaim

  val basisOfClaimSpecialCircumstances: Option[String]

  val methodOfDisposal: MethodOfDisposal

  val detailsOfRejectedGoods: String

  val inspectionDate: LocalDate

  val inspectionAddress: InspectionAddress

  val reimbursementMethod: ReimbursementMethodAnswer

  val bankAccountDetails: Option[BankAccountDetails]

  val supportingEvidences: Seq[EvidenceDocument]

  def leadMrn: MRN

  def totalReimbursementAmount: BigDecimal

  def getClaimsOverMrns: List[(MRN, Map[TaxCode, BigDecimal])]

  def declarationMode: DeclarationMode

  def caseType: CaseType

  def documents: Seq[EvidenceDocument]
}
