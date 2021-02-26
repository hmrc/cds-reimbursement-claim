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

package uk.gov.hmrc.cdsreimbursementclaim.models.generators

import org.scalacheck.Gen
import org.scalacheck.ScalacheckShapeless._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.request.{DeclarationRequest, OverpaymentDeclarationDisplayRequest, RequestCommon, RequestDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.{DisplayDeclaration, MaskedBankDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN

object DeclarationGen extends GenUtils {
  implicit val declarationGen: Gen[DisplayDeclaration]                                              = gen[DisplayDeclaration]
  implicit val declarationRequestGen: Gen[DeclarationRequest]                                       = gen[DeclarationRequest]
  implicit val overpaymentDeclarationDisplayRequestGen: Gen[OverpaymentDeclarationDisplayRequest]   =
    gen[OverpaymentDeclarationDisplayRequest]
  implicit val requestCommonGen: Gen[RequestCommon]                                                 = gen[RequestCommon]
  implicit val requestDetailGen: Gen[RequestDetail]                                                 = gen[RequestDetail]
  implicit val mrnGen: Gen[MRN]                                                                     = gen[MRN]
  implicit val maskedBankDetails: Gen[MaskedBankDetails]                                            = gen[MaskedBankDetails]
  implicit val bankDetailsGen: Gen[BankDetails]                                                     = gen[BankDetails]
  implicit val accountDetailsGen: Gen[AccountDetails]                                               = gen[AccountDetails]
  implicit val declarantDetailsGen: Gen[DeclarantDetails]                                           = gen[DeclarantDetails]
  implicit val contactDetailsGen: Gen[ContactDetails]                                               = gen[ContactDetails]
  implicit val consigneeDetailsGen: Gen[ConsigneeDetails]                                           = gen[ConsigneeDetails]
  implicit val establishmentAddressGen: Gen[EstablishmentAddress]                                   = gen[EstablishmentAddress]
  implicit val consigneeBankDetailsGen: Gen[ConsigneeBankDetails]                                   = gen[ConsigneeBankDetails]
  implicit val declarantBankDetailsGen: Gen[DeclarantBankDetails]                                   = gen[DeclarantBankDetails]
  implicit val securityDetailsGen: Gen[SecurityDetails]                                             = gen[SecurityDetails]
  implicit val taxDetailsGen: Gen[TaxDetails]                                                       = gen[TaxDetails]
  implicit val ndrcDetailsGen: Gen[NdrcDetails]                                                     = gen[NdrcDetails]
  implicit val declarationInfoResponseGen: Gen[DeclarationResponse]                                 = gen[DeclarationResponse]
  implicit val responseCommonGen: Gen[ResponseCommon]                                               = gen[ResponseCommon]
  implicit val responseDetailGen: Gen[ResponseDetail]                                               = gen[ResponseDetail]
  implicit val overpaymentDeclarationDisplayResponseGen: Gen[OverpaymentDeclarationDisplayResponse] =
    gen[OverpaymentDeclarationDisplayResponse]
}
