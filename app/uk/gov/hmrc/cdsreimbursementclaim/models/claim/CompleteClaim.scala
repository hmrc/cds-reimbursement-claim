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
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ContactDetailsAnswer.CompleteContactDetailsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DetailsRegisteredWithCdsAnswer.CompleteDetailsRegisteredWithCdsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ClaimsAnswer.CompleteClaimsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.CommoditiesDetailsAnswer.CompleteCommodityDetailsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DeclarantEoriNumberAnswer.CompleteDeclarantEoriNumberAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DeclarantTypeAnswer.CompleteDeclarantTypeAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DeclarationDetailsAnswer.CompleteDeclarationDetailsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DuplicateDeclarationDetailsAnswer.CompleteDuplicateDeclarationDetailsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DuplicateMovementReferenceNumberAnswer.CompleteDuplicateMovementReferenceNumberAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ImporterEoriNumberAnswer.CompleteImporterEoriNumberAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.MovementReferenceNumberAnswer.CompleteMovementReferenceNumberAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ReasonAndBasisOfClaimAnswer.CompleteReasonAndBasisOfClaimAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.SupportingEvidenceAnswer.CompleteSupportingEvidenceAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.BasisOfClaim
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.{EntryNumber, MRN}
import uk.gov.hmrc.cdsreimbursementclaim.utils.MoneyUtils._

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
    completeDetailsRegisteredWithCdsAnswer: CompleteDetailsRegisteredWithCdsAnswer,
    maybeContactDetailsAnswer: Option[CompleteContactDetailsAnswer],
    maybeBasisOfClaimAnswer: Option[CompleteBasisOfClaimAnswer],
    maybeCompleteBankAccountDetailAnswer: Option[CompleteBankAccountDetailAnswer],
    supportingEvidenceAnswers: CompleteSupportingEvidenceAnswer,
    completeCommodityDetailsAnswer: CompleteCommodityDetailsAnswer,
    maybeCompleteReasonAndBasisOfClaimAnswer: Option[CompleteReasonAndBasisOfClaimAnswer],
    maybeDisplayDeclaration: Option[DisplayDeclaration],
    maybeDuplicateDisplayDeclaration: Option[DisplayDeclaration],
    importerEoriNumber: Option[CompleteImporterEoriNumberAnswer],
    declarantEoriNumber: Option[CompleteDeclarantEoriNumberAnswer],
    completeClaimsAnswer: CompleteClaimsAnswer
  ) extends CompleteClaim

  implicit class CompleteClaimOps(private val completeClaim: CompleteClaim) {

    def reasonAndBasisOfClaim: Option[CompleteReasonAndBasisOfClaimAnswer] = completeClaim match {
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
            maybeCompleteReasonAndBasisOfClaimAnswer,
            _,
            _,
            _,
            _,
            _
          ) =>
        maybeCompleteReasonAndBasisOfClaimAnswer
    }

    def bankDetails: Option[CompleteBankAccountDetailAnswer] = completeClaim match {
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
            _
          ) =>
        maybeCompleteBankAccountDetailAnswer
    }

    def entryDeclarationDetails: Option[CompleteDeclarationDetailsAnswer] = completeClaim match {
      case CompleteC285Claim(
            _,
            _,
            _,
            maybeCompleteDeclarationDetailsAnswer,
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
        maybeCompleteDeclarationDetailsAnswer
    }

    def duplicateEntryDeclarationDetails: Option[CompleteDuplicateDeclarationDetailsAnswer] = completeClaim match {
      case CompleteC285Claim(
            _,
            _,
            _,
            _,
            maybeCompleteDuplicateDeclarationDetailsAnswer,
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
        maybeCompleteDuplicateDeclarationDetailsAnswer
    }

    def evidences: List[SupportingEvidence] = completeClaim match {
      case CompleteC285Claim(_, _, _, _, _, _, _, _, _, _, supportingEvidenceAnswers, _, _, _, _, _, _, _) =>
        supportingEvidenceAnswers.evidences
    }

    def referenceNumberType: Either[EntryNumber, MRN] = completeClaim match {
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
            _
          ) =>
        completeMovementReferenceNumberAnswer.movementReferenceNumber
    }

    def correlationId: UUID = completeClaim match {
      case CompleteC285Claim(id, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _) =>
        id
    }

    def declarantTypeAnswer: CompleteDeclarantTypeAnswer = completeClaim match {
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
            maybeBasisOfClaimAnswer,
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
        maybeBasisOfClaimAnswer.map(basisOfClaimAnswer => basisOfClaimAnswer.basisOfClaim)
    }

    def claims: List[Claim] = completeClaim match {
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
            completeClaimsAnswer
          ) =>
        completeClaimsAnswer.claims.map(claim =>
          claim.copy(
            claimAmount = roundedTwoDecimalPlaces(claim.claimAmount),
            paidAmount = roundedTwoDecimalPlaces(claim.paidAmount)
          )
        )
    }

    def commodityDetails: CommodityDetails = completeClaim match {
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
            completeCommodityDetailsAnswer,
            _,
            _,
            _,
            _,
            _,
            _
          ) =>
        completeCommodityDetailsAnswer.commodityDetails
    }

    def detailsRegisteredWithCds: DetailsRegisteredWithCdsFormData = completeClaim match {
      case CompleteC285Claim(
            _,
            _,
            _,
            _,
            _,
            _,
            detailsRegisteredWithCdsAnswer,
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
        detailsRegisteredWithCdsAnswer.detailsRegisteredWithCds

    }

    def claimantDetailsAsImporter: Option[ContactDetailsFormData] = completeClaim match {
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
            _
          ) =>
        maybeClaimantDetailsAsImporterCompanyAnswer match {
          case Some(completeClaimantDetailsAsImporterCompanyAnswer) =>
            Some(completeClaimantDetailsAsImporterCompanyAnswer.contactDetailsFormData)
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
            maybeDisplayDeclaration,
            _,
            _,
            _,
            _
          ) =>
        maybeDisplayDeclaration

    }

    def duplicateDisplayDeclaration: Option[DisplayDeclaration] = completeClaim match {
      case CompleteC285Claim(_, _, _, _, _, _, _, _, _, _, _, _, _, _, maybeDuplicateDisplayDeclaration, _, _, _) =>
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
            _
          ) =>
        maybeCompleteBankAccountDetailAnswer

    }
    def consigneeDetails: Option[ConsigneeDetails] = completeClaim match {
      case CompleteC285Claim(_, _, _, _, _, _, _, _, _, _, _, _, _, maybeDisplayDeclaration, _, _, _, _) =>
        maybeDisplayDeclaration.flatMap(s => s.displayResponseDetail.consigneeDetails)

    }

    def declarantDetails: Option[DeclarantDetails] = completeClaim match {
      case CompleteC285Claim(_, _, _, _, _, _, _, _, _, _, _, _, _, maybeDisplayDeclaration, _, _, _, _) =>
        maybeDisplayDeclaration.map(s => s.displayResponseDetail.declarantDetails)
    }
  }

  implicit val format: OFormat[CompleteClaim] = derived.oformat[CompleteClaim]()
}
