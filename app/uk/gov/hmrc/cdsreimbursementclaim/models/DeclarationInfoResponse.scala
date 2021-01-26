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

package uk.gov.hmrc.cdsreimbursementclaim.models

import play.api.libs.json.{Json, OFormat}

/** @param overpaymentDeclarationDisplayResponse
  */
final case class DeclarationInfoResponse(
  overpaymentDeclarationDisplayResponse: OverpaymentDeclarationDisplayResponse
)

object DeclarationInfoResponse {
  implicit val returnParametersReads: OFormat[ReturnParameters]                                            = Json.format
  implicit val establishmentAddressReads: OFormat[EstablishmentAddress]                                    = Json.format
  implicit val contactDetailsReads: OFormat[ContactDetails]                                                = Json.format
  implicit val declarantDetailsReads: OFormat[DeclarantDetails]                                            = Json.format
  implicit val consigneeBankDetailsReads: OFormat[ConsigneeBankDetails]                                    = Json.format
  implicit val declarantBankDetailsReads: OFormat[DeclarantBankDetails]                                    = Json.format
  implicit val bankDetailsReads: OFormat[BankDetails]                                                      = Json.format
  implicit val taxDetailsReads: OFormat[TaxDetails]                                                        = Json.format
  implicit val consigneeDetailsReads: OFormat[ConsigneeDetails]                                            = Json.format
  implicit val accountDetailsReads: OFormat[AccountDetails]                                                = Json.format
  implicit val securityDetailsReads: OFormat[SecurityDetails]                                              = Json.format
  implicit val ndrcDetailsReads: OFormat[NdrcDetails]                                                      = Json.format
  implicit val responseDetailReads: OFormat[ResponseDetail]                                                = Json.format
  implicit val responseCommonReads: OFormat[ResponseCommon]                                                = Json.format
  implicit val overpaymentDeclarationDisplayResponseReader: OFormat[OverpaymentDeclarationDisplayResponse] = Json.format
  implicit val declarationInfoResponseReader: OFormat[DeclarationInfoResponse]                             = Json.format
}

/** @param responseCommon
  * @param responseDetail
  */
final case class OverpaymentDeclarationDisplayResponse(
  responseCommon: ResponseCommon,
  responseDetail: Option[ResponseDetail]
)

/** @param status Possible values are
  *                    OK
  *                    NOT_OK
  * @param statusText Status Text
  * @param processingDate
  * @param returnParameters
  */
final case class ResponseCommon(
  status: String,
  statusText: Option[String],
  processingDate: String,
  returnParameters: Option[List[ReturnParameters]]
)

/** Return Parameters
  *
  * @param paramName Parameter Name. Possible values are:
  *                       ERRORCODE
  *                       ERRORTEXT
  * @param paramValue Parameter Value
  */
final case class ReturnParameters(
  paramName: String,
  paramValue: String
)

/** @param declarationId
  * @param acceptanceDate
  * @param declarantReferenceNumber
  * @param securityReason Security Reason Codes Description:
  *                       "MDP"  Missing Document Preference
  *                       "MDL"  Missing Document License Quota
  *                       "ACS"  Account Sales
  *                       "CEP"  CAP Entry Price
  *                       "CSD"  CAP Safeguard Duties
  *                       "T24"  Temporary Admission (2 years Expiration)
  *                       "TA6"  Temporary Admission (6 months Expiration)
  *                       "TA3"  Temporary Admission (3 months Expiration)
  *                       "TA2"  Temporary Admission (2 months Expiration)
  *                       "IPR"  Inward Processing Relief
  *                       "OPR"  Outward Processing Relief
  *                       "ENU"  End-use (Authorisation by Declaration)
  *                       "RED"  Revenue Dispute
  *                       "MOD"  Manual Override Deposit
  *                       "MDC" Missing Document CSDR              "CRQ" Critical Quota
  *                       "PDD" Provisional Dumping Duties (both Anti-Dumping and Countervailing)
  * @param btaDueDate
  * @param procedureCode
  * @param btaSource
  * @param declarantDetails
  * @param consigneeDetails
  * @param accountDetails
  * @param bankDetails
  * @param securityDetails
  * @param ndrcDetails
  */
final case class ResponseDetail(
  declarationId: String,
  acceptanceDate: String,
  declarantReferenceNumber: Option[String],
  securityReason: Option[String],
  btaDueDate: Option[String],
  procedureCode: String,
  btaSource: Option[String],
  declarantDetails: DeclarantDetails,
  consigneeDetails: Option[ConsigneeDetails],
  accountDetails: Option[List[AccountDetails]],
  bankDetails: Option[BankDetails],
  securityDetails: Option[List[SecurityDetails]],
  ndrcDetails: Option[List[NdrcDetails]]
)

/** @param declarantEORI
  * @param legalName
  * @param establishmentAddress
  * @param contactDetails
  */
final case class DeclarantDetails(
  declarantEORI: String,
  legalName: String,
  establishmentAddress: EstablishmentAddress,
  contactDetails: Option[ContactDetails]
)

/** @param addressLine1 35 character address line
  * @param addressLine2 35 character address line
  * @param addressLine3 35 character address line
  * @param postalCode
  * @param countryCode List of ISO Country Codes
  */
final case class EstablishmentAddress(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String],
  postalCode: Option[String],
  countryCode: String
)

/** @param contactName
  * @param addressLine1 35 character address line
  * @param addressLine2 35 character address line
  * @param addressLine3 35 character address line
  * @param addressLine4 35 character address line
  * @param postalCode
  * @param countryCode List of ISO Country Codes
  * @param telephone
  * @param emailAddress
  */
final case class ContactDetails(
  contactName: Option[String],
  addressLine1: Option[String],
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  postalCode: Option[String],
  countryCode: Option[String],
  telephone: Option[String],
  emailAddress: Option[String]
)

/** @param consigneeEORI
  * @param legalName
  * @param establishmentAddress
  * @param contactDetails
  */
final case class ConsigneeDetails(
  consigneeEORI: String,
  legalName: String,
  establishmentAddress: EstablishmentAddress,
  contactDetails: Option[ContactDetails]
)

/** @param accountType
  * @param accountNumber
  * @param eori
  * @param legalName
  * @param contactDetails
  */
final case class AccountDetails(
  accountType: String,
  accountNumber: String,
  eori: String,
  legalName: String,
  contactDetails: Option[ContactDetails]
)

/** @param consigneeBankDetails
  * @param declarantBankDetails
  */
final case class BankDetails(
  consigneeBankDetails: Option[ConsigneeBankDetails],
  declarantBankDetails: Option[DeclarantBankDetails]
)

/** @param accountHolderName
  * @param sortCode
  * @param accountNumber
  */
final case class ConsigneeBankDetails(
  accountHolderName: String,
  sortCode: String,
  accountNumber: String
)

/** @param accountHolderName
  * @param sortCode
  * @param accountNumber
  */
final case class DeclarantBankDetails(
  accountHolderName: String,
  sortCode: String,
  accountNumber: String
)

/** @param securityDepositId
  * @param totalAmount
  * @param amountPaid
  * @param paymentMethod Possible Values:
  *                      001 = Immediate Payment
  *                      002 = Duty Deferment
  *                      003 = Cash Account
  *                      004 = Guarantee Account
  *                      005 = Individual Guarantee
  * @param paymentReference
  * @param taxDetails
  */
final case class SecurityDetails(
  securityDepositId: String,
  totalAmount: String,
  amountPaid: String,
  paymentMethod: String,
  paymentReference: String,
  taxDetails: List[TaxDetails]
)

/** @param taxType
  * @param amount
  */
final case class TaxDetails(
  taxType: String,
  amount: String
)

/** @param taxType
  * @param amount
  * @param paymentMethod Possible Values:
  *                      001 = Immediate Payment
  *                      002 = Duty Deferment
  *                      003 = Cash Account
  * @param paymentReference
  * @param cmaEligible Possible Values:
  *                    0 - CMA Not Eligible
  *                    1- CMA Eligible
  *                    Note:For security related and non-security immediate payment related declarations,CMA Eligible will not be returned.
  */
final case class NdrcDetails(
  taxType: String,
  amount: String,
  paymentMethod: String,
  paymentReference: String,
  cmaEligible: Option[String]
)
