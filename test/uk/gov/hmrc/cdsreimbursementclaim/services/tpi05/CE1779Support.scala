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

package uk.gov.hmrc.cdsreimbursementclaim.services.tpi05

import cats.implicits.catsSyntaxEq
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ReimbursementMethodAnswer.CurrentMonthAdjustment
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimantType, SingleRejectedGoodsClaim}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.{BankDetail, BankDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{Claimant, ReimbursementMethod}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant.{Importer, Representative}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps

trait CE1779Support {

  implicit class RejectedGoodsClaimOps(claim: SingleRejectedGoodsClaim) {

    def claimant: Claimant =
      if (claim.claimantType === ClaimantType.Consignee) Importer else Representative

    def claimedAmountAsString: String =
      claim.totalReimbursementAmount.roundToTwoDecimalPlaces.toString()

    def tpi05ReimbursementMethod: ReimbursementMethod =
      if (claim.reimbursementMethod === CurrentMonthAdjustment) ReimbursementMethod.Deferment
      else ReimbursementMethod.BankTransfer

    def firstNonEmptyBankDetails(maybeBankDetails: Option[response.BankDetails]): Option[BankDetails] =
      (maybeBankDetails, claim.bankAccountDetails) match {
        case (_, Some(bankAccountDetails)) =>
          Some(
            BankDetails(
              consigneeBankDetails = Some(BankDetail.from(bankAccountDetails)),
              declarantBankDetails = Some(BankDetail.from(bankAccountDetails))
            )
          )
        case (Some(acc14BankDetails), _)   =>
          Some(
            BankDetails(
              declarantBankDetails = acc14BankDetails.declarantBankDetails.map(BankDetail.from),
              consigneeBankDetails = acc14BankDetails.consigneeBankDetails.map(BankDetail.from)
            )
          )
        case _                             => None
      }
  }
}
