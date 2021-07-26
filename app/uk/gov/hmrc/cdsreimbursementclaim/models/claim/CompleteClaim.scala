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

import cats.data.NonEmptyList
import julienrf.json.derived
import play.api.libs.json.OFormat
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.BankAccountDetailsAnswer.CompleteBankAccountDetailAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ContactDetailsAnswer.CompleteContactDetailsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DeclarantEoriNumberAnswer.CompleteDeclarantEoriNumberAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DeclarationDetailsAnswer.CompleteDeclarationDetailsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DetailsRegisteredWithCdsAnswer.CompleteDetailsRegisteredWithCdsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DuplicateDeclarationDetailsAnswer.CompleteDuplicateDeclarationDetailsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ImporterEoriNumberAnswer.CompleteImporterEoriNumberAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ReasonAndBasisOfClaimAnswer.CompleteReasonAndBasisOfClaimAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.answers.{ClaimsAnswer, ScheduledDocumentAnswer, SupportingEvidencesAnswer}
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
    movementReferenceNumber: MovementReferenceNumber,
    maybeDuplicateMovementReferenceNumberAnswer: Option[MovementReferenceNumber],
    maybeCompleteDeclarationDetailsAnswer: Option[CompleteDeclarationDetailsAnswer],
    maybeCompleteDuplicateDeclarationDetailsAnswer: Option[CompleteDuplicateDeclarationDetailsAnswer],
    declarantTypeAnswer: DeclarantTypeAnswer,
    completeDetailsRegisteredWithCdsAnswer: CompleteDetailsRegisteredWithCdsAnswer,
    maybeContactDetailsAnswer: Option[CompleteContactDetailsAnswer],
    maybeBasisOfClaimAnswer: Option[BasisOfClaim],
    maybeCompleteBankAccountDetailAnswer: Option[CompleteBankAccountDetailAnswer],
    supportingEvidencesAnswer: SupportingEvidencesAnswer,
    commodityDetailsAnswer: CommodityDetails,
    maybeCompleteReasonAndBasisOfClaimAnswer: Option[CompleteReasonAndBasisOfClaimAnswer],
    maybeDisplayDeclaration: Option[DisplayDeclaration],
    maybeDuplicateDisplayDeclaration: Option[DisplayDeclaration],
    importerEoriNumber: Option[CompleteImporterEoriNumberAnswer],
    declarantEoriNumber: Option[CompleteDeclarantEoriNumberAnswer],
    claimsAnswer: ClaimsAnswer,
    scheduledDocumentAnswer: Option[ScheduledDocumentAnswer]
  ) extends CompleteClaim

  implicit class CompleteClaimOps(private val completeClaim: CompleteClaim) {

    def reasonAndBasisOfClaim: Option[CompleteReasonAndBasisOfClaimAnswer] =
      completeClaim.get(_.maybeCompleteReasonAndBasisOfClaimAnswer)

    def bankDetails: Option[CompleteBankAccountDetailAnswer] =
      completeClaim.get(_.maybeCompleteBankAccountDetailAnswer)

    def entryDeclarationDetails: Option[CompleteDeclarationDetailsAnswer] =
      completeClaim.get(_.maybeCompleteDeclarationDetailsAnswer)

    def duplicateEntryDeclarationDetails: Option[CompleteDuplicateDeclarationDetailsAnswer] =
      completeClaim.get(_.maybeCompleteDuplicateDeclarationDetailsAnswer)

    def documents: NonEmptyList[UploadDocument] = {
      val evidences         = completeClaim.get(_.supportingEvidencesAnswer)
      val scheduledDocument =
        completeClaim
          .get(_.scheduledDocumentAnswer)
          .map(_.uploadDocument)
          .toList

      evidences ++ scheduledDocument
    }

    def referenceNumberType: Either[EntryNumber, MRN] =
      completeClaim.get(_.movementReferenceNumber.value)

    def correlationId: UUID = completeClaim.get(_.id)

    def declarantTypeAnswer: DeclarantTypeAnswer =
      completeClaim.get(_.declarantTypeAnswer)

    def basisOfClaim: Option[BasisOfClaim] =
      completeClaim.get(_.maybeBasisOfClaimAnswer)

    def claims: NonEmptyList[Claim] = completeClaim
      .get(_.claimsAnswer)
      .map(claim =>
        claim.copy(
          claimAmount = roundedTwoDecimalPlaces(claim.claimAmount),
          paidAmount = roundedTwoDecimalPlaces(claim.paidAmount)
        )
      )

    def commodityDetails: CommodityDetails =
      completeClaim.get(_.commodityDetailsAnswer)

    def detailsRegisteredWithCds: DetailsRegisteredWithCdsFormData =
      completeClaim.get(_.completeDetailsRegisteredWithCdsAnswer.detailsRegisteredWithCds)

    def claimantDetailsAsImporter: Option[ContactDetailsFormData] =
      completeClaim.get(_.maybeContactDetailsAnswer).map(_.contactDetailsFormData)

    def displayDeclaration: Option[DisplayDeclaration] =
      completeClaim.get(_.maybeDisplayDeclaration)

    def duplicateDisplayDeclaration: Option[DisplayDeclaration] =
      completeClaim.get(_.maybeDuplicateDisplayDeclaration)

    def enteredBankDetails: Option[CompleteBankAccountDetailAnswer] =
      completeClaim.get(_.maybeCompleteBankAccountDetailAnswer)

    def consigneeDetails: Option[ConsigneeDetails] =
      completeClaim.get(_.maybeDisplayDeclaration.flatMap(s => s.displayResponseDetail.consigneeDetails))

    def declarantDetails: Option[DeclarantDetails] =
      completeClaim.get(_.maybeDisplayDeclaration.map(s => s.displayResponseDetail.declarantDetails))

    def get[A](f: CompleteC285Claim => A): A =
      f(completeClaim.asInstanceOf[CompleteC285Claim])
  }

  implicit val format: OFormat[CompleteClaim] = derived.oformat[CompleteClaim]()
}
