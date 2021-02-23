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

import julienrf.json.derived
import play.api.libs.json.OFormat
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.BasisOfClaim
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.{DisplayDeclaration, response}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.{EntryNumber, MRN}
import uk.gov.hmrc.cdsreimbursementclaim.models.EitherUtils._

import java.util.UUID

sealed trait CompleteClaim

object CompleteClaim {

  //TODO: complete definition
  final case class CompleteC285Claim(
    id: UUID,
    movementReferenceNumber: Either[EntryNumber, MRN],
    completeClaimantDetailsAsIndividualAnswer: CompleteClaimantDetailsAsIndividualAnswer,
    supportingEvidences: CompleteSupportingEvidenceAnswer,
    declarantType: CompleteDeclarantTypeAnswer,
    maybeReasonForClaim: Option[BasisOfClaim],
    completeClaimAnswer: CompleteClaimAnswer,
    completeCommodityDetailsAnswer: CompleteCommodityDetailsAnswer,
    declaration: Option[DisplayDeclaration],
    duplicateDeclaration: Option[DisplayDeclaration],
    maybeCompleteBankDetailsAnswer: Option[CompleteBankAccountDetailAnswer]
  ) extends CompleteClaim

  implicit class CompleteClaimOps(private val completeClaim: CompleteClaim) {

    def evidences: List[SupportingEvidence] = completeClaim match {
      case CompleteC285Claim(_, _, _, completeSupportingEvidenceAnswer, _, _, _, _, _, _, _) =>
        completeSupportingEvidenceAnswer.evidences
    }

    def movementReferenceNumber: Either[EntryNumber, MRN] = completeClaim match {
      case CompleteC285Claim(_, movementReferenceNumber, _, _, _, _, _, _, _, _, _) => movementReferenceNumber
    }

    def correlationId: UUID = completeClaim match {
      case CompleteC285Claim(id, _, _, _, _, _, _, _, _, _, _) => id
    }

    def declarantType: CompleteDeclarantTypeAnswer = completeClaim match {
      case CompleteC285Claim(_, _, _, _, declarantType, _, _, _, _, _, _) => declarantType
    }

    def basisOfClaim: Option[BasisOfClaim] = completeClaim match {
      case CompleteC285Claim(_, _, _, _, _, maybeReasonForClaim, _, _, _, _, _) => maybeReasonForClaim
    }

    def claims: List[Claim] = completeClaim match {
      case CompleteC285Claim(_, _, _, _, _, _, completeClaimAnswer, _, _, _, _) => completeClaimAnswer.claims
    }

    def commodityDetails: String = completeClaim match {
      case CompleteC285Claim(_, _, _, _, _, _, _, completeCommodityDetailsAnswer, _, _, _) =>
        completeCommodityDetailsAnswer.commodityDetails.value
    }

    def claimantDetailsAsIndividual: ClaimantDetailsAsIndividual = completeClaim match {
      case CompleteC285Claim(
            _,
            _,
            completeClaimantDetailsAsIndividualAnswer,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _
          ) =>
        completeClaimantDetailsAsIndividualAnswer.claimantDetailsAsIndividual
    }

    def displayDeclaration: Option[DisplayDeclaration] = completeClaim match {
      case CompleteC285Claim(
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            declaration,
            _,
            _
          ) =>
        declaration
    }

    def duplicateDisplayDeclaration: Option[DisplayDeclaration] = completeClaim match {
      case CompleteC285Claim(
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            duplicateDeclaration,
            _
          ) =>
        duplicateDeclaration
    }

    def enteredBankDetails: Option[CompleteBankAccountDetailAnswer] = completeClaim match {
      case CompleteC285Claim(
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            maybeCompleteBankAccountDetailAnswer
          ) =>
        maybeCompleteBankAccountDetailAnswer
    }

    def consigneeDetails: Option[response.ConsigneeDetails] = completeClaim match {
      case CompleteC285Claim(
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            declaration,
            _,
            _
          ) =>
        declaration.flatMap(d => d.displayResponseDetail.consigneeDetails)
    }

    def declarantDetails: Option[response.DeclarantDetails] = completeClaim match {
      case CompleteC285Claim(
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            declaration,
            _,
            _
          ) =>
        declaration.map(d => d.displayResponseDetail.declarantDetails)
    }

  }

  implicit val format: OFormat[CompleteClaim] = derived.oformat[CompleteClaim]()
}
