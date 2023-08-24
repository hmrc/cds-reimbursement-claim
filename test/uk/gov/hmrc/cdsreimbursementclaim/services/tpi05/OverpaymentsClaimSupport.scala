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

package uk.gov.hmrc.cdsreimbursementclaim.services.tpi05

import uk.gov.hmrc.cdsreimbursementclaim.models.claim.OverpaymentsClaim
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.{BankDetail, BankDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant

trait OverpaymentsClaimSupport {

  implicit class OverpaymentsClaimOps(claim: OverpaymentsClaim) {

    def firstNonEmptyBankDetails(maybeBankDetails: Option[response.BankDetails]): Option[BankDetails] =
      (maybeBankDetails, claim.bankAccountDetailsAnswer) match {
        case (acc14BankDetailsOpt, Some(bankAccountDetails)) =>
          Some(
            Claimant.basedOn(claim.claimantType) match {
              case Claimant.Importer       =>
                BankDetails(
                  consigneeBankDetails = Some(BankDetail.from(bankAccountDetails)),
                  declarantBankDetails = acc14BankDetailsOpt.flatMap(_.declarantBankDetails.map(BankDetail.from))
                )
              case Claimant.Representative =>
                BankDetails(
                  consigneeBankDetails = acc14BankDetailsOpt.flatMap(_.consigneeBankDetails.map(BankDetail.from)),
                  declarantBankDetails = Some(BankDetail.from(bankAccountDetails))
                )
            }
          )
        case (Some(acc14BankDetails), _)                     =>
          Some(
            BankDetails(
              declarantBankDetails = acc14BankDetails.declarantBankDetails.map(BankDetail.from),
              consigneeBankDetails = acc14BankDetails.consigneeBankDetails.map(BankDetail.from)
            )
          )
        case _                                               => None
      }
  }
}
