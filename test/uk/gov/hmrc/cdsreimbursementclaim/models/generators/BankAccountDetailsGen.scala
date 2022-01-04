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

package uk.gov.hmrc.cdsreimbursementclaim.models.generators

import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.magnolia.Typeclass
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{AccountName, AccountNumber, SortCode}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.{BankAccountDetails, BankDetails}

object BankAccountDetailsGen {

  lazy val genAccountName: Gen[AccountName] =
    Gen
      .nonEmptyListOf(Gen.alphaChar)
      .map(_.take(40).mkString)
      .map(AccountName(_))

  lazy val genSortCode: Gen[SortCode] =
    Gen
      .listOfN(6, Gen.numChar)
      .map(_.mkString)
      .map(SortCode(_))

  lazy val genAccountNumber: Gen[AccountNumber] =
    Gen
      .listOfN(8, Gen.numChar)
      .map(_.mkString)
      .map(AccountNumber(_))

  lazy val genBankAccountDetails: Gen[BankAccountDetails] =
    for {
      accountName   <- genAccountName
      sortCode      <- genSortCode
      accountNumber <- genAccountNumber
    } yield BankAccountDetails(accountName, sortCode, accountNumber)

  lazy val genBankDetails: Gen[BankDetails] =
    for {
      consigneeBankDetails <- genBankAccountDetails
      declarantBankDetails <- genBankAccountDetails
    } yield BankDetails(Some(consigneeBankDetails), Some(declarantBankDetails))

  lazy val genMaskedBankDetails: Gen[BankDetails] = {

    def mask(value: String): String =
      s"Ends with ${value.substring(value.length - 2)}"

    for {
      consigneeBankDetails <- genBankAccountDetails
      declarantBankDetails <- genBankAccountDetails
    } yield BankDetails(
      consigneeBankDetails = Some(
        consigneeBankDetails.copy(
          sortCode = SortCode(mask(consigneeBankDetails.sortCode.value)),
          accountNumber = AccountNumber(mask(consigneeBankDetails.accountNumber.value))
        )
      ),
      declarantBankDetails = Some(
        declarantBankDetails.copy(
          sortCode = SortCode(mask(declarantBankDetails.sortCode.value)),
          accountNumber = AccountNumber(mask(declarantBankDetails.accountNumber.value))
        )
      )
    )
  }

  implicit lazy val arbitraryBankAccountDetails: Typeclass[BankAccountDetails] =
    Arbitrary(genBankAccountDetails)

  implicit lazy val arbitraryBankDetails: Typeclass[BankDetails] =
    Arbitrary(genBankDetails)
}
