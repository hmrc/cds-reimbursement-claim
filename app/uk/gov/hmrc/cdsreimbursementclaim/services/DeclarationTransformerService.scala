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

package uk.gov.hmrc.cdsreimbursementclaim.services

import com.google.inject.{ImplementedBy, Inject}
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{AccountName, AccountNumber, SortCode}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.{DisplayDeclaration, DisplayResponseDetail}
import uk.gov.hmrc.cdsreimbursementclaim.services.DefaultDeclarationTransformerService.{maskBankDetails, toDisplayResponseDetails}
import uk.gov.hmrc.cdsreimbursementclaim.utils.{Logging, TimeUtils}

import javax.inject.Singleton

@ImplementedBy(classOf[DefaultDeclarationTransformerService])
trait DeclarationTransformerService {
  def toDeclaration(declarationResponse: DeclarationResponse): Either[Error, Option[DisplayDeclaration]]
}

@Singleton
class DefaultDeclarationTransformerService @Inject() () extends DeclarationTransformerService with Logging {
  override def toDeclaration(declarationResponse: DeclarationResponse): Either[Error, Option[DisplayDeclaration]] =
    declarationResponse.overpaymentDeclarationDisplayResponse.responseDetail match {
      case Some(responseDetail) =>
        Right(
          Some(
            DisplayDeclaration(
              toDisplayResponseDetails(
                responseDetail,
                responseDetail.bankDetails.map(bankDetails => maskBankDetails(bankDetails))
              )
            )
          )
        )
      case None                 => Right(None)
    }

}

object DefaultDeclarationTransformerService {

  def toDisplayResponseDetails(
    responseDetail: ResponseDetail,
    maskedBankDetails: Option[BankDetails]
  ): DisplayResponseDetail =
    DisplayResponseDetail(
      declarationId = responseDetail.declarationId,
      acceptanceDate = TimeUtils
        .toDisplayAcceptanceDateFormat(responseDetail.acceptanceDate)
        .getOrElse("could not convert acceptance date"),
      declarantReferenceNumber = responseDetail.declarantReferenceNumber,
      securityReason = responseDetail.securityReason,
      btaDueDate = responseDetail.btaDueDate,
      procedureCode = responseDetail.procedureCode,
      btaSource = responseDetail.btaSource,
      declarantDetails = responseDetail.declarantDetails,
      consigneeDetails = responseDetail.consigneeDetails,
      accountDetails = responseDetail.accountDetails,
      bankDetails = responseDetail.bankDetails,
      maskedBankDetails = maskedBankDetails,
      ndrcDetails = responseDetail.ndrcDetails
    )

  def maskBankDetails(bankDetails: BankDetails): BankDetails = {
    val consigneeBankDetails: Option[BankAccountDetails] =
      bankDetails.consigneeBankDetails
        .map(consigneeBankDetails =>
          maskBankAccount(
            consigneeBankDetails.accountName,
            consigneeBankDetails.sortCode,
            consigneeBankDetails.accountNumber
          )
        )
        .map(maskedBankAccount =>
          BankAccountDetails(
            maskedBankAccount.accountName,
            maskedBankAccount.sortCode,
            maskedBankAccount.accountNumber
          )
        )

    val declarantBankDetails: Option[BankAccountDetails] =
      bankDetails.declarantBankDetails
        .map(declarantBankDetails =>
          maskBankAccount(
            declarantBankDetails.accountName,
            declarantBankDetails.sortCode,
            declarantBankDetails.accountNumber
          )
        )
        .map(maskedBankAccount =>
          BankAccountDetails(
            maskedBankAccount.accountName,
            maskedBankAccount.sortCode,
            maskedBankAccount.accountNumber
          )
        )

    BankDetails(
      consigneeBankDetails,
      declarantBankDetails
    )
  }

  def maskBankAccount(
    accountHolderName: AccountName,
    sortCode: SortCode,
    accountNumber: AccountNumber
  ): BankAccountDetails = {
    def maskDigits(digits: String): String = digits.replaceAll("([0-9]+)", "Ending with ")

    val (toMaskSortCodeComponent, toDisplaySortCodeComponent)           = sortCode.value.splitAt(4)
    val maskedSortCode                                                  = maskDigits(toMaskSortCodeComponent) + toDisplaySortCodeComponent
    val (toMaskAccountNumberComponent, toDisplayAccountNumberComponent) = accountNumber.value.splitAt(4)
    val maskedAccountNumber                                             = maskDigits(toMaskAccountNumberComponent) + toDisplayAccountNumberComponent

    BankAccountDetails(
      accountHolderName,
      SortCode(maskedSortCode),
      AccountNumber(maskedAccountNumber)
    )
  }

}
