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
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.{BankAccountDetails, ConsigneeDetails, DeclarantDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps

import java.util.UUID

final case class C285Claim(
  id: UUID,
  typeOfClaim: TypeOfClaimAnswer,
  movementReferenceNumber: MRN,
  duplicateMovementReferenceNumberAnswer: Option[MRN],
  declarantTypeAnswer: DeclarantTypeAnswer,
  detailsRegisteredWithCdsAnswer: DetailsRegisteredWithCdsAnswer,
  mrnContactDetailsAnswer: Option[ContactDetails],
  mrnContactAddressAnswer: Option[ContactAddress],
  basisOfClaimAnswer: BasisOfClaim,
  bankAccountDetailsAnswer: Option[BankAccountDetails],
  documents: NonEmptyList[EvidenceDocument],
  commodityDetailsAnswer: CommodityDetailsAnswer,
  displayDeclaration: Option[DisplayDeclaration],
  duplicateDisplayDeclaration: Option[DisplayDeclaration],
  importerEoriNumber: Option[ImporterEoriNumberAnswer],
  declarantEoriNumber: Option[DeclarantEoriNumberAnswer],
  claimedReimbursementsAnswer: ClaimedReimbursementsAnswer,
  reimbursementMethodAnswer: ReimbursementMethodAnswer,
  associatedMRNsAnswer: Option[AssociatedMRNsAnswer],
  associatedMRNsClaimsAnswer: Option[AssociatedMRNsClaimsAnswer]
) {

  lazy val consigneeDetails: Option[ConsigneeDetails] =
    displayDeclaration.flatMap(s => s.displayResponseDetail.consigneeDetails)

  lazy val declarantDetails: Option[DeclarantDetails] =
    displayDeclaration.map(s => s.displayResponseDetail.declarantDetails)

  def multipleClaims: List[(MRN, ClaimedReimbursementsAnswer)] = {
    val mrns   = movementReferenceNumber :: associatedMRNsAnswer.toList.flatMap(_.toList)
    val claims = claimedReimbursementsAnswer :: associatedMRNsClaimsAnswer.toList.flatMap(_.toList)
    mrns zip claims
  }

  def claims: NonEmptyList[ClaimedReimbursement] =
    claimedReimbursementsAnswer
      .map(claim =>
        claim.copy(
          claimAmount = claim.claimAmount.roundToTwoDecimalPlaces,
          paidAmount = claim.paidAmount.roundToTwoDecimalPlaces
        )
      )

  def totalReimbursementAmount: BigDecimal = {
    def totalClaimedDuty(claimedReimbursementsAnswer: ClaimedReimbursementsAnswer): BigDecimal =
      claimedReimbursementsAnswer.foldLeft(BigDecimal(0)) { (accumulator, claim) =>
        accumulator + claim.claimAmount
      }

    typeOfClaim match {
      case TypeOfClaimAnswer.Multiple =>
        associatedMRNsClaimsAnswer.toList
          .flatMap(_.toList)
          .foldLeft(totalClaimedDuty(claimedReimbursementsAnswer))((accumulator, claimedReimbursementsAnswer) =>
            accumulator + totalClaimedDuty(claimedReimbursementsAnswer)
          )
      case _                          => totalClaimedDuty(claimedReimbursementsAnswer)
    }
  }
}

object C285Claim {

  implicit val format: Format[C285Claim] = Json.format[C285Claim]
}
