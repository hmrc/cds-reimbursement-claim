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
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.BankAccountDetailsAnswer.CompleteBankAccountDetailAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.BasisOfClaimAnswer.CompleteBasisOfClaimAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ClaimantDetailsAsImporterCompanyAnswer.CompleteClaimantDetailsAsImporterCompanyAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ClaimantDetailsAsIndividualAnswer.CompleteClaimantDetailsAsIndividualAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ClaimsAnswer.CompleteClaimsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.CommoditiesDetailsAnswer.CompleteCommodityDetailsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DeclarantEoriNumberAnswer.CompleteDeclarantEoriNumberAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DeclarantTypeAnswer.CompleteDeclarantTypeAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DeclarationDetailsAnswer.CompleteDeclarationDetailsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DuplicateDeclarationDetailsAnswer.CompleteDuplicateDeclarationDetailsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DuplicateMovementReferenceNumberAnswer.CompleteDuplicateMovementReferenceNumberAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.EUDutyAmountAnswers.CompleteEUDutyAmountAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ImporterEoriNumberAnswer.CompleteImporterEoriNumberAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.MovementReferenceNumberAnswer.CompleteMovementReferenceNumberAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ReasonAndBasisOfClaimAnswer.CompleteReasonAndBasisOfClaimAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.SupportingEvidenceAnswer.CompleteSupportingEvidenceAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.UKDutyAmountAnswers.CompleteUKDutyAmountAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.BasisOfClaim
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.{EntryNumber, MRN}

import java.util.UUID

sealed trait CompleteClaim extends Product with Serializable {
  val id: UUID
}

object CompleteClaim {

  final case class CompleteC285Claim(
    id: UUID,
    completeMovementReferenceNumberAnswer: CompleteMovementReferenceNumberAnswer,
    maybeCompleteDuplicateMovementReferenceNumberAnswer: Option[CompleteDuplicateMovementReferenceNumberAnswer],
    maybeCompleteDeclarationDetailsAnswer: Option[CompleteDeclarationDetailsAnswer],
    maybeCompleteDuplicateDeclarationDetailsAnswer: Option[CompleteDuplicateDeclarationDetailsAnswer],
    completeDeclarantTypeAnswer: CompleteDeclarantTypeAnswer,
    completeClaimantDetailsAsIndividualAnswer: CompleteClaimantDetailsAsIndividualAnswer,
    maybeClaimantDetailsAsImporterCompanyAnswer: Option[CompleteClaimantDetailsAsImporterCompanyAnswer],
    maybeBasisOfClaimAnswer: Option[CompleteBasisOfClaimAnswer],
    maybeCompleteBankAccountDetailAnswer: Option[CompleteBankAccountDetailAnswer],
    supportingEvidenceAnswers: CompleteSupportingEvidenceAnswer,
    maybeCompleteUKDutyAmountAnswer: Option[CompleteUKDutyAmountAnswer],
    maybeCompleteEUDutyAmountAnswer: Option[CompleteEUDutyAmountAnswer],
    completeClaimAnswer: CompleteClaimsAnswer,
    completeCommodityDetailsAnswer: CompleteCommodityDetailsAnswer,
    maybeCompleteReasonAndBasisOfClaimAnswer: Option[CompleteReasonAndBasisOfClaimAnswer],
    maybeDisplayDeclaration: Option[DisplayDeclaration],
    maybeDuplicateDisplayDeclaration: Option[DisplayDeclaration],
    importerEoriNumber: Option[CompleteImporterEoriNumberAnswer],
    declarantEoriNumber: Option[CompleteDeclarantEoriNumberAnswer]
  ) extends CompleteClaim

  implicit class CompleteClaimOps(private val completeClaim: CompleteClaim) {

    def evidences: List[SupportingEvidence] = completeClaim match {
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
            supportingEvidenceAnswers,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _
          ) =>
        supportingEvidenceAnswers.evidences
    }

    def movementReferenceNumber: Either[EntryNumber, MRN] = completeClaim match {
      case CompleteC285Claim(
            _,
            completeMovementReferenceNumberAnswer,
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
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _
          ) =>
        completeMovementReferenceNumberAnswer.movementReferenceNumber
    }

    def correlationId: UUID = completeClaim match {
      case CompleteC285Claim(
            id,
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
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _
          ) =>
        id
    }

    def declarantType: CompleteDeclarantTypeAnswer = completeClaim match {
      case CompleteC285Claim(
            _,
            _,
            _,
            _,
            _,
            completeDeclarantTypeAnswer,
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
            _,
            _,
            _,
            _
          ) =>
        completeDeclarantTypeAnswer
    }

    def basisOfClaim: Option[BasisOfClaim] = completeClaim match {
      case CompleteC285Claim(
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            maybeBasisForClaim,
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
            _
          ) =>
        maybeBasisForClaim match {
          case Some(basisOfClaimAnswer) => Some(basisOfClaimAnswer.basisOfClaim)
          case None                     => None
        }
    }

    def claims: List[Claim] = completeClaim match {
      case CompleteC285Claim(_, _, _, _, _, _, _, _, _, _, _, _, _, completeClaimAnswers, _, _, _, _, _, _) =>
        completeClaimAnswers.claims
    }

    def commodityDetails: String = completeClaim match {
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
            _,
            _,
            _,
            _,
            completeCommodityDetailsAnswer,
            _,
            _,
            _,
            _,
            _
          ) =>
        completeCommodityDetailsAnswer.commodityDetails.value
    }

    def claimantDetailsAsIndividual: ClaimantDetailsAsIndividual = completeClaim match {
      case CompleteC285Claim(
            _,
            _,
            _,
            _,
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
            _,
            _,
            _,
            _,
            _,
            _
          ) =>
        completeClaimantDetailsAsIndividualAnswer.claimantDetailsAsIndividual
    }

    def claimantDetailsAsImporter: Option[ClaimantDetailsAsImporterCompany] = completeClaim match {
      case CompleteC285Claim(
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            maybeClaimantDetailsAsImporterCompanyAnswer,
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
            _,
            _
          ) =>
        maybeClaimantDetailsAsImporterCompanyAnswer match {
          case Some(completeClaimantDetailsAsImporterCompanyAnswer) =>
            Some(completeClaimantDetailsAsImporterCompanyAnswer.claimantDetailsAsImporterCompany)
          case None                                                 => None
        }
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
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            maybeDisplayDeclaration,
            _,
            _,
            _
          ) =>
        maybeDisplayDeclaration
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
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            maybeDuplicateDisplayDeclaration,
            _,
            _
          ) =>
        maybeDuplicateDisplayDeclaration
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
            maybeCompleteBankAccountDetailAnswer,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _,
            _
          ) =>
        maybeCompleteBankAccountDetailAnswer
    }
    def consigneeDetails: Option[ConsigneeDetails]                  = completeClaim match {
      case CompleteC285Claim(_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, maybeDisplayDeclaration, _, _, _) =>
        maybeDisplayDeclaration.flatMap(s => s.displayResponseDetail.consigneeDetails)
    }

    def declarantDetails: Option[DeclarantDetails] = completeClaim match {
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
            _,
            _,
            _,
            _,
            _,
            _,
            maybeDisplayDeclaration,
            _,
            _,
            _
          ) =>
        maybeDisplayDeclaration.map(s => s.displayResponseDetail.declarantDetails)
    }

  }

  implicit val format: OFormat[CompleteClaim] = derived.oformat[CompleteClaim]()
}
