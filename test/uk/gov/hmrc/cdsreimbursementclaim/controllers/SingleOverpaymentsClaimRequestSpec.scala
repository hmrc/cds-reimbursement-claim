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

package uk.gov.hmrc.cdsreimbursementclaim.controllers

import play.api.libs.json.Json
import play.api.test.*
import play.api.test.Helpers.*
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.*
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.ContactInformation
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.BankAccountDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.{Eori, MRN}

class SingleOverpaymentsClaimRequestSpec extends ControllerSpec with ConnectorStubs {

  "The controller" should {
    "succeed returning claim reference number" when {
      "handling overpayments single claim request where claimant is a third-party user" in {

        val expectedDeclaration = DeclarationStubs.fullDeclarationWithMarkedFields

        val controller =
          buildApplication(
            authConnector = new AuthConnectorStub(),
            declarationConnector = new DeclarationConnectorStub(expectedDeclaration),
            claimConnector = new ClaimConnectorStub {}
          ).injector
            .instanceOf[SubmitClaimController]

        val request         = new SingleOverpaymentsClaimRequest(
          claim = SingleOverpaymentsClaim(
            movementReferenceNumber = MRN("claim.movementReferenceNumber"),
            duplicateMovementReferenceNumber = None,
            claimantType = ClaimantType.User,
            payeeType = PayeeType.Consignee,
            claimantInformation = ClaimantInformation(
              eori = Eori("CLAIMANTEORI"),
              fullName = "claim.claimantInformation.fullName",
              establishmentAddress = ContactInformation(
                contactPerson = Some("claim.claimantInformation.establishmentAddress.contactPerson"),
                addressLine1 = Some("claim.claimantInformation.establishmentAddress.addressLine1"),
                addressLine2 = Some("claim.claimantInformation.establishmentAddress.addressLine2"),
                addressLine3 = Some("claim.claimantInformation.establishmentAddress.addressLine3"),
                street = Some("claim.claimantInformation.establishmentAddress.street"),
                city = Some("claim.claimantInformation.establishmentAddress.city"),
                countryCode = Some("claim.claimantInformation.establishmentAddress.countryCode"),
                postalCode = Some("claim.claimantInformation.establishmentAddress.postalCode"),
                telephoneNumber = Some("claim.claimantInformation.establishmentAddress.telephoneNumber"),
                faxNumber = Some("claim.claimantInformation.establishmentAddress.faxNumber"),
                emailAddress = Some("claim.claimantInformation.establishmentAddress.emailAddress")
              ),
              contactInformation = ContactInformation(
                contactPerson = Some("claim.claimantInformation.contactInformation.contactPerson"),
                addressLine1 = Some("claim.claimantInformation.contactInformation.addressLine1"),
                addressLine2 = Some("claim.claimantInformation.contactInformation.addressLine2"),
                addressLine3 = Some("claim.claimantInformation.contactInformation.addressLine3"),
                street = Some("claim.claimantInformation.contactInformation.street"),
                city = Some("claim.claimantInformation.contactInformation.city"),
                countryCode = Some("claim.claimantInformation.contactInformation.countryCode"),
                postalCode = Some("claim.claimantInformation.contactInformation.postalCode"),
                telephoneNumber = Some("claim.claimantInformation.contactInformation.telephoneNumber"),
                faxNumber = Some("claim.claimantInformation.contactInformation.faxNumber"),
                emailAddress = Some("claim.claimantInformation.contactInformation.emailAddress")
              )
            ),
            basisOfClaim = BasisOfClaim.DutySuspension,
            additionalDetails = "claim.additionalDetails",
            reimbursements =
              Seq(Reimbursement(TaxCode.A00, BigDecimal("1.00"), ReimbursementMethodAnswer.BankAccountTransfer)),
            reimbursementMethod = ReimbursementMethodAnswer.CurrentMonthAdjustment,
            bankAccountDetails = Some(
              BankAccountDetails(
                accountName = AccountName("claim.bankAccountDetails.accountName"),
                sortCode = SortCode("claim.bankAccountDetails.sortCode"),
                accountNumber = AccountNumber("claim.bankAccountDetails.accountNumber")
              )
            ),
            supportingEvidences = Seq(),
            newEoriAndDan = None
          )
        )
        val requestBodyJson = Json.toJson(request)

        val result = controller.submitSingleOverpaymentsClaim()(fakeRequestWithJsonBody(requestBodyJson))

        status(result) shouldBe OK
        contentAsJson(result)
      }
    }
  }

}
