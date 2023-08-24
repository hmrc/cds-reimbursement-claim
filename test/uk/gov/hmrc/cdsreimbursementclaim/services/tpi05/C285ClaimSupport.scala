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

import cats.implicits._
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ReimbursementMethodAnswer.CurrentMonthAdjustment
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{C285Claim, DeclarantTypeAnswer, ReimbursementMethodAnswer, TypeOfClaimAnswer}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant.{Importer, Representative}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.YesNo.{No, Yes}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{CaseType, Claimant, DeclarationMode, ReimbursementMethod, YesNo}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.{BankDetail, BankDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps

trait C285ClaimSupport {

  implicit class C285ClaimOps(claim: C285Claim) {

    def claimant: Claimant =
      if (claim.declarantTypeAnswer === DeclarantTypeAnswer.Importer) Importer else Representative

    def caseType: CaseType =
      Seq(TypeOfClaimAnswer.Multiple, TypeOfClaimAnswer.Scheduled)
        .find(_ === claim.typeOfClaim)
        .map(_ => CaseType.Bulk)
        .getOrElse(
          if (claim.reimbursementMethodAnswer === CurrentMonthAdjustment) CaseType.CMA
          else CaseType.Individual
        )

    def declarationMode: DeclarationMode =
      if (claim.typeOfClaim === TypeOfClaimAnswer.Multiple) DeclarationMode.AllDeclaration
      else DeclarationMode.ParentDeclaration

    def claimedAmountAsString: String =
      claim.totalReimbursementAmount.roundToTwoDecimalPlaces.toString()

    def isForPrivateImporter: YesNo =
      if (claim.declarantTypeAnswer === DeclarantTypeAnswer.Importer) Yes else No

    def reimbursementMethod: ReimbursementMethod =
      if (claim.reimbursementMethodAnswer === ReimbursementMethodAnswer.Subsidy) ReimbursementMethod.Subsidy
      else if (claim.reimbursementMethodAnswer === ReimbursementMethodAnswer.CurrentMonthAdjustment)
        ReimbursementMethod.Deferment
      else ReimbursementMethod.BankTransfer

    def firstNonEmptyBankDetails(maybeBankDetails: Option[response.BankDetails]): Option[BankDetails] =
      (maybeBankDetails, claim.bankAccountDetailsAnswer) match {
        case (acc14BankDetailsOpt, Some(bankAccountDetails)) =>
          Some(
            Claimant.basedOn(claim.declarantTypeAnswer) match {
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
