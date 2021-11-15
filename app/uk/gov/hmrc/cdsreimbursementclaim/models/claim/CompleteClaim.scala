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
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.BasisOfClaimAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.MoneyUtils._

import java.util.UUID

final case class CompleteClaim(
  id: UUID,
  typeOfClaim: TypeOfClaimAnswer,
  movementReferenceNumber: MRN,
  duplicateMovementReferenceNumberAnswer: Option[MRN],
  declarantTypeAnswer: DeclarantTypeAnswer,
  detailsRegisteredWithCdsAnswer: DetailsRegisteredWithCdsAnswer,
  mrnContactDetailsAnswer: Option[MrnContactDetails],
  mrnContactAddressAnswer: Option[ContactAddress],
  basisOfClaimAnswer: BasisOfClaimAnswer,
  bankAccountDetailsAnswer: Option[BankAccountDetails],
  documents: NonEmptyList[UploadDocument],
  commodityDetailsAnswer: CommodityDetailsAnswer,
  displayDeclaration: Option[DisplayDeclaration],
  duplicateDisplayDeclaration: Option[DisplayDeclaration],
  importerEoriNumber: Option[ImporterEoriNumberAnswer],
  declarantEoriNumber: Option[DeclarantEoriNumberAnswer],
  claimedReimbursementsAnswer: ClaimedReimbursementsAnswer,
  reimbursementMethodAnswer: Option[ReimbursementMethodAnswer],
  associatedMRNsAnswer: Option[AssociatedMRNsAnswer],
  associatedMRNsClaimsAnswer: Option[AssociatedMRNsClaimsAnswer]
)

object CompleteClaim {

  implicit class CompleteClaimOps(private val completeClaim: CompleteClaim) {

    def claims: NonEmptyList[ClaimedReimbursement] =
      completeClaim.claimedReimbursementsAnswer
        .map(claim =>
          claim.copy(
            claimAmount = roundedTwoDecimalPlaces(claim.claimAmount),
            paidAmount = roundedTwoDecimalPlaces(claim.paidAmount)
          )
        )

    def totalReimbursementAmount: BigDecimal = completeClaim.typeOfClaim match {
      case TypeOfClaimAnswer.Multiple =>
        completeClaim.associatedMRNsClaimsAnswer.toList
          .flatMap(_.toList)
          .foldLeft(totalClaimedDuty(completeClaim.claimedReimbursementsAnswer))(
            (accumulator, claimedReimbursementsAnswer) => accumulator + totalClaimedDuty(claimedReimbursementsAnswer)
          )
      case _                          => totalClaimedDuty(completeClaim.claimedReimbursementsAnswer)
    }

    private def totalClaimedDuty(claimedReimbursementsAnswer: ClaimedReimbursementsAnswer): BigDecimal =
      claimedReimbursementsAnswer.foldLeft(BigDecimal(0)) { (accumulator, claim) =>
        accumulator + claim.claimAmount
      }

    def consigneeDetails: Option[ConsigneeDetails] =
      completeClaim.displayDeclaration.flatMap(s => s.displayResponseDetail.consigneeDetails)

    def declarantDetails: Option[DeclarantDetails] =
      completeClaim.displayDeclaration.map(s => s.displayResponseDetail.declarantDetails)
  }

  implicit val format: Format[CompleteClaim] = Json.format[CompleteClaim]
}
