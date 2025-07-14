/*
 * Copyright 2025 HM Revenue & Customs
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

object DeclarationStubs {

  val fullDeclarationWithMarkedFields =
    s"""|{
        |    "overpaymentDeclarationDisplayResponse": {
        |        "responseCommon": {
        |            "status": "OK",
        |            "processingDate": "2025-05-21T11:05:29Z"
        |        },
        |        "responseDetail": {
        |            "declarationId": "acc14.responseDetail.declarationId",
        |            "acceptanceDate": "2024-02-16",
        |            "declarantReferenceNumber": "acc14.responseDetail.declarantReferenceNumber",
        |            "procedureCode": "acc14.responseDetail.procedureCode",
        |            "declarantDetails": {
        |                "declarantEORI": "acc14.responseDetail.declarantDetails.declarantEORI",
        |                "legalName": "acc14.responseDetail.declarantDetails.legalName",
        |                "establishmentAddress": {
        |                    "addressLine1": "acc14.responseDetail.declarantDetails.establishmentAddress.addressLine1",
        |                    "addressLine3": "acc14.responseDetail.declarantDetails.establishmentAddress.addressLine3",
        |                    "postalCode": "acc14.responseDetail.declarantDetails.establishmentAddress.postalCode",
        |                    "countryCode": "acc14.responseDetail.declarantDetails.establishmentAddress.countryCode"
        |                },
        |                "contactDetails": {
        |                    "addressLine1": "acc14.responseDetail.declarantDetails.contactDetails.addressLine1",
        |                    "addressLine3": "acc14.responseDetail.declarantDetails.contactDetails.addressLine3",
        |                    "postalCode": "acc14.responseDetail.declarantDetails.contactDetails.postalCode",
        |                    "countryCode": "acc14.responseDetail.declarantDetails.contactDetails.countryCode",
        |                    "emailAddress": "acc14.responseDetail.declarantDetails.contactDetails.emailAddress"
        |                }
        |            },
        |            "consigneeDetails": {
        |                "consigneeEORI": "acc14.responseDetail.consigneeDetails.consigneeEORI",
        |                "legalName": "acc14.responseDetail.consigneeDetails.legalName",
        |                "establishmentAddress": {
        |                    "addressLine1": "acc14.responseDetail.consigneeDetails.establishmentAddress.addressLine1",
        |                    "addressLine2": "acc14.responseDetail.consigneeDetails.establishmentAddress.addressLine2",
        |                    "addressLine3": "acc14.responseDetail.consigneeDetails.establishmentAddress.addressLine3",
        |                    "postalCode": "acc14.responseDetail.consigneeDetails.establishmentAddress.postalCode",
        |                    "countryCode": "acc14.responseDetail.consigneeDetails.establishmentAddress.countryCode"
        |                },
        |                "contactDetails": {
        |                    "addressLine1": "acc14.responseDetail.consigneeDetails.contactDetails.addressLine1",
        |                    "postalCode": "acc14.responseDetail.consigneeDetails.contactDetails.postalCode",
        |                    "countryCode": "acc14.responseDetail.consigneeDetails.contactDetails.countryCode",
        |                    "emailAddress": "acc14.responseDetail.consigneeDetails.contactDetails.emailAddress"
        |                }
        |            },
        |            "accountDetails": [
        |                {
        |                    "accountType": "acc14.responseDetail.accountDetails.accountType",
        |                    "accountNumber": "acc14.responseDetail.accountDetails.accountNumber",
        |                    "eori": "acc14.responseDetail.accountDetails.eori",
        |                    "legalName": "acc14.responseDetail.accountDetails.legalName",
        |                    "contactDetails": {
        |                        "contactName": "acc14.responseDetail.accountDetails.contactDetails.contactName",
        |                        "addressLine1": "acc14.responseDetail.accountDetails.contactDetails.addressLine1",
        |                        "addressLine2": "acc14.responseDetail.accountDetails.contactDetails.addressLine2",
        |                        "addressLine3": "acc14.responseDetail.accountDetails.contactDetails.addressLine3",
        |                        "addressLine4": "acc14.responseDetail.accountDetails.contactDetails.addressLine4",
        |                        "postalCode": "acc14.responseDetail.accountDetails.contactDetails.postalCode",
        |                        "countryCode": "acc14.responseDetail.accountDetails.contactDetails.countryCode",
        |                        "emailAddress": "acc14.responseDetail.accountDetails.contactDetails.emailAddress"
        |                    }
        |                }
        |            ],
        |            "bankDetails": {
        |                "consigneeBankDetails": {
        |                    "accountHolderName": "acc14.responseDetail.bankDetails.consigneeBankDetails.accountHolderName",
        |                    "sortCode": "acc14.responseDetail.bankDetails.consigneeBankDetails.sortCode",
        |                    "accountNumber": "acc14.responseDetail.bankDetails.consigneeBankDetails.accountNumber"
        |                },
        |                "declarantBankDetails": {
        |                    "accountHolderName": "acc14.responseDetail.bankDetails.declarantBankDetails.accountHolderName",
        |                    "sortCode": "acc14.responseDetail.bankDetails.declarantBankDetails.sortCode",
        |                    "accountNumber": "acc14.responseDetail.bankDetails.declarantBankDetails.accountNumber"
        |                }
        |            },
        |            "ndrcDetails": [
        |                {
        |                    "taxType": "A00",
        |                    "amount": "1.23",
        |                    "paymentMethod": "001",
        |                    "paymentReference": "paymentReference",
        |                    "cmaEligible": "acc14.responseDetail.ndrcDetails.A00.cmaEligible"
        |                }
        |            ]
        |        }
        |    }
        |}""".stripMargin

}
