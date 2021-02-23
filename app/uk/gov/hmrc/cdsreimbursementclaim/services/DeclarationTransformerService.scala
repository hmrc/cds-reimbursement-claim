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
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.{DisplayDeclaration, DisplayResponseDetail, MaskedBankAccount, MaskedBankDetails}
import uk.gov.hmrc.cdsreimbursementclaim.services.DefaultDeclarationTransformerService.{maskBankDetails, toDisplayResponseDetails}
import uk.gov.hmrc.cdsreimbursementclaim.utils.{Logging, TimeUtils}

import javax.inject.Singleton

@ImplementedBy(classOf[DefaultDeclarationTransformerService])
trait DeclarationTransformerService {
  def toDeclaration(declarationResponse: DeclarationResponse): Either[Error, DisplayDeclaration]
}

@Singleton
class DefaultDeclarationTransformerService @Inject() () extends DeclarationTransformerService with Logging {
  override def toDeclaration(declarationResponse: DeclarationResponse): Either[Error, DisplayDeclaration] =
    declarationResponse.overpaymentDeclarationDisplayResponse.responseDetail match {
      case Some(responseDetail) =>
        Right(
          DisplayDeclaration(
            toDisplayResponseDetails(
              responseDetail,
              responseDetail.bankDetails.map(bankDetails => maskBankDetails(bankDetails))
            )
          )
        )
      case None                 => Left(Error("could not find declaration detail"))
    }

}

object DefaultDeclarationTransformerService {

  def toDisplayResponseDetails(
    responseDetail: ResponseDetail,
    maskedBankDetails: Option[MaskedBankDetails]
  ): DisplayResponseDetail =
    DisplayResponseDetail(
      declarationId = responseDetail.declarationId,
      acceptanceDate = TimeUtils
        .acceptanceDateDisplayFormat(responseDetail.acceptanceDate)
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

  def maskBankDetails(bankDetails: BankDetails): MaskedBankDetails = {
    val consigneeBankDetails: Option[ConsigneeBankDetails] =
      bankDetails.consigneeBankDetails
        .map(consigneeBankDetails =>
          maskBankAccount(
            consigneeBankDetails.accountHolderName,
            consigneeBankDetails.sortCode,
            consigneeBankDetails.accountNumber
          )
        )
        .map(maskedBankAccount =>
          ConsigneeBankDetails(
            maskedBankAccount.accountHolderName,
            maskedBankAccount.sortCode,
            maskedBankAccount.accountNumber
          )
        )

    val declarantBankDetails: Option[DeclarantBankDetails] =
      bankDetails.declarantBankDetails
        .map(declarantBankDetails =>
          maskBankAccount(
            declarantBankDetails.accountHolderName,
            declarantBankDetails.sortCode,
            declarantBankDetails.accountNumber
          )
        )
        .map(maskedBankAccount =>
          DeclarantBankDetails(
            maskedBankAccount.accountHolderName,
            maskedBankAccount.sortCode,
            maskedBankAccount.accountNumber
          )
        )

    MaskedBankDetails(
      consigneeBankDetails,
      declarantBankDetails
    )
  }

  def maskBankAccount(accountHolderName: String, sortCode: String, accountNumber: String): MaskedBankAccount = {
    def maskDigits(digits: String): String = digits.replaceAll("[0-9]", "*")

    val (toMaskSortCodeComponent, toDisplaySortCodeComponent)           = sortCode.splitAt(4)
    val maskedSortCode                                                  = maskDigits(toMaskSortCodeComponent) + toDisplaySortCodeComponent
    val (toMaskAccountNumberComponent, toDisplayAccountNumberComponent) = accountNumber.splitAt(4)
    val maskedAccountNumber                                             = maskDigits(toMaskAccountNumberComponent) + toDisplayAccountNumberComponent

    MaskedBankAccount(
      accountHolderName,
      maskedSortCode,
      maskedAccountNumber
    )
  }

}
