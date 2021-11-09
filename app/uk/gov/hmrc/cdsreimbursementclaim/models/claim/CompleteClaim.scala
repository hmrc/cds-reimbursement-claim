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
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.answers._
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.SelectNumberOfClaimsAnswer._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.BasisOfClaim
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.MoneyUtils._

import java.util.UUID

final case class CompleteClaim(
  id: UUID,
  movementReferenceNumber: MRN,
  maybeDuplicateMovementReferenceNumberAnswer: Option[MRN],
  declarantTypeAnswer: DeclarantTypeAnswer,
  detailsRegisteredWithCdsAnswer: DetailsRegisteredWithCdsAnswer,
  mrnContactDetailsAnswer: Option[MrnContactDetails],
  mrnContactAddressAnswer: Option[ContactAddress],
  maybeBasisOfClaimAnswer: Option[BasisOfClaim],
  maybeBankAccountDetailsAnswer: Option[BankAccountDetails],
  supportingEvidencesAnswer: SupportingEvidencesAnswer,
  commodityDetailsAnswer: CommodityDetails,
  maybeDisplayDeclaration: Option[DisplayDeclaration],
  maybeDuplicateDisplayDeclaration: Option[DisplayDeclaration],
  importerEoriNumber: Option[ImporterEoriNumber],
  declarantEoriNumber: Option[DeclarantEoriNumber],
  claimedReimbursementsAnswer: ClaimedReimbursementsAnswer,
  scheduledDocumentAnswer: Option[ScheduledDocumentAnswer],
  reimbursementMethodAnswer: Option[ReimbursementMethodAnswer],
  typeOfClaim: Option[SelectNumberOfClaimsAnswer],
  associatedMRNsAnswer: Option[List[MRN]],
  maybeAssociatedMRNsClaimsAnswer: Option[List[ClaimedReimbursementsAnswer]]
)

object CompleteClaim {

  implicit class CompleteClaimOps(private val completeClaim: CompleteClaim) {

    def documents: NonEmptyList[UploadDocument] = {
      val evidences         = completeClaim.supportingEvidencesAnswer
      val scheduledDocument =
        completeClaim.scheduledDocumentAnswer
          .map(_.uploadDocument)
          .toList

      evidences ++ scheduledDocument
    }

    def claims: NonEmptyList[ClaimedReimbursement] =
      completeClaim.claimedReimbursementsAnswer
        .map(claim =>
          claim.copy(
            claimAmount = roundedTwoDecimalPlaces(claim.claimAmount),
            paidAmount = roundedTwoDecimalPlaces(claim.paidAmount)
          )
        )

    def totalReimbursementAmount: BigDecimal = completeClaim.typeOfClaim match {
      case Some(Multiple) =>
        completeClaim.maybeAssociatedMRNsClaimsAnswer
          .getOrElse(List())
          .foldLeft(totalClaimedDuty(completeClaim.claimedReimbursementsAnswer))(
            (accumulator, claimedReimbursementsAnswer) => accumulator + totalClaimedDuty(claimedReimbursementsAnswer)
          )
      case _              => totalClaimedDuty(completeClaim.claimedReimbursementsAnswer)
    }

    private def totalClaimedDuty(claimedReimbursementsAnswer: ClaimedReimbursementsAnswer): BigDecimal =
      claimedReimbursementsAnswer.foldLeft(BigDecimal(0)) { (accumulator, claim) =>
        accumulator + claim.claimAmount
      }

    def consigneeDetails: Option[ConsigneeDetails] =
      completeClaim.maybeDisplayDeclaration.flatMap(s => s.displayResponseDetail.consigneeDetails)

    def declarantDetails: Option[DeclarantDetails] =
      completeClaim.maybeDisplayDeclaration.map(s => s.displayResponseDetail.declarantDetails)
  }

  implicit val format: Format[CompleteClaim] = Json.format[CompleteClaim]
}
