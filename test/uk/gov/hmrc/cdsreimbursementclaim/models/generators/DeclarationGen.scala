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

import org.scalacheck.magnolia._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.request.{DeclarationRequest, OverpaymentDeclarationDisplayRequest, RequestCommon, RequestDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.{DisplayDeclaration, DisplayResponseDetail, MaskedBankDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN

object DeclarationGen {
  implicit val mrnGen: Typeclass[MRN]                                                                     = gen[MRN]
  implicit val requestCommonGen: Typeclass[RequestCommon]                                                 = gen[RequestCommon]
  implicit val requestDetailGen: Typeclass[RequestDetail]                                                 = gen[RequestDetail]
  implicit val overpaymentDeclarationDisplayRequestGen: Typeclass[OverpaymentDeclarationDisplayRequest]   =
    gen[OverpaymentDeclarationDisplayRequest]
  implicit val declarationRequestGen: Typeclass[DeclarationRequest]                                       = gen[DeclarationRequest]
  implicit val establishmentAddressGen: Typeclass[EstablishmentAddress]                                   = gen[EstablishmentAddress]
  implicit val contactDetailsGen: Typeclass[ContactDetails]                                               = gen[ContactDetails]
  implicit val declarantDetailsGen: Typeclass[DeclarantDetails]                                           = gen[DeclarantDetails]
  implicit val accountDetailsGen: Typeclass[AccountDetails]                                               = gen[AccountDetails]
  implicit val consigneeDetailsGen: Typeclass[ConsigneeDetails]                                           = gen[ConsigneeDetails]
  implicit val consigneeBankDetailsGen: Typeclass[ConsigneeBankDetails]                                   = gen[ConsigneeBankDetails]
  implicit val declarantBankDetailsGen: Typeclass[DeclarantBankDetails]                                   = gen[DeclarantBankDetails]
  implicit val bankDetailsGen: Typeclass[BankDetails]                                                     = gen[BankDetails]
  implicit val maskedBankDetailsGen: Typeclass[MaskedBankDetails]                                         = gen[MaskedBankDetails]
  implicit val taxDetailsGen: Typeclass[TaxDetails]                                                       = gen[TaxDetails]
  implicit val securityDetailsGen: Typeclass[SecurityDetails]                                             = gen[SecurityDetails]
  implicit val ndrcDetailsGen: Typeclass[NdrcDetails]                                                     = gen[NdrcDetails]
  implicit val responseCommonGen: Typeclass[ResponseCommon]                                               = gen[ResponseCommon]
  implicit val responseDetailGen: Typeclass[ResponseDetail]                                               = gen[ResponseDetail]
  implicit val overpaymentDeclarationDisplayResponseGen: Typeclass[OverpaymentDeclarationDisplayResponse] =
    gen[OverpaymentDeclarationDisplayResponse]
  implicit val declarationInfoResponseGen: Typeclass[DeclarationResponse]                                 = gen[DeclarationResponse]
  implicit val displayDeclarationGen: Typeclass[DisplayResponseDetail]                                    = gen[DisplayResponseDetail]
  implicit val declarationGen: Typeclass[DisplayDeclaration]                                              = gen[DisplayDeclaration]
}
