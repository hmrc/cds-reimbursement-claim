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

import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.{DeclarationResponse, OverpaymentDeclarationDisplayResponse, ResponseDetail, _}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.{DisplayDeclaration, DisplayResponseDetail, MaskedBankDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.DeclarationGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
import uk.gov.hmrc.cdsreimbursementclaim.utils.TimeUtils

class DeclarationTransformerServiceSpec extends AnyWordSpec with Matchers with MockFactory {

  val transformer = new DefaultDeclarationTransformerService()

  "Declaration transformer" when {

    "passed a declaration response" must {

      "return a display declaration if response details exist" in {

        val bankDetails = BankDetails(
          Some(
            ConsigneeBankDetails(
              accountHolderName = "Test Account",
              sortCode = "123456",
              accountNumber = "12345678"
            )
          ),
          Some(
            DeclarantBankDetails(
              accountHolderName = "Test Account",
              sortCode = "123456",
              accountNumber = "12345678"
            )
          )
        )

        val responseDetail                        = sample[ResponseDetail].copy(bankDetails = Some(bankDetails))
        val overpaymentDeclarationDisplayResponse =
          sample[OverpaymentDeclarationDisplayResponse].copy(responseDetail = Some(responseDetail))
        val declarationResponse                   = sample[DeclarationResponse].copy(overpaymentDeclarationDisplayResponse =
          overpaymentDeclarationDisplayResponse
        )

        val maskedBankDetails = MaskedBankDetails(
          Some(
            ConsigneeBankDetails(
              accountHolderName = "Test Account",
              sortCode = "Ending with 56",
              accountNumber = "Ending with 5678"
            )
          ),
          Some(
            DeclarantBankDetails(
              accountHolderName = "Test Account",
              sortCode = "Ending with 56",
              accountNumber = "Ending with 5678"
            )
          )
        )

        val displayDeclaration = DisplayDeclaration(
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
            maskedBankDetails = Some(maskedBankDetails),
            ndrcDetails = responseDetail.ndrcDetails
          )
        )

        transformer.toDeclaration(declarationResponse) shouldBe Right(Some(displayDeclaration))
      }

      "do not return a display declaration if response details does not exist" in {

        val overpaymentDeclarationDisplayResponse =
          sample[OverpaymentDeclarationDisplayResponse].copy(responseDetail = None)
        val declarationResponse                   = sample[DeclarationResponse].copy(overpaymentDeclarationDisplayResponse =
          overpaymentDeclarationDisplayResponse
        )
        transformer.toDeclaration(declarationResponse) shouldBe Right(None)
      }

    }
  }

}
