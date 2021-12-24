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
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.{DisplayDeclaration, DisplayResponseDetail}

object DeclarationGen {
  import IdGen._
  import BankAccountDetailsGen._

  implicit lazy val requestCommonGen: Typeclass[RequestCommon]                                                 = gen[RequestCommon]
  implicit lazy val requestDetailGen: Typeclass[RequestDetail]                                                 = gen[RequestDetail]
  implicit lazy val overpaymentDeclarationDisplayRequestGen: Typeclass[OverpaymentDeclarationDisplayRequest]   =
    gen[OverpaymentDeclarationDisplayRequest]
  implicit lazy val declarationRequestGen: Typeclass[DeclarationRequest]                                       = gen[DeclarationRequest]
  implicit lazy val establishmentAddressGen: Typeclass[EstablishmentAddress]                                   = gen[EstablishmentAddress]
  implicit lazy val contactDetailsGen: Typeclass[ContactDetails]                                               = gen[ContactDetails]
  implicit lazy val declarantDetailsGen: Typeclass[DeclarantDetails]                                           = gen[DeclarantDetails]
  implicit lazy val accountDetailsGen: Typeclass[AccountDetails]                                               = gen[AccountDetails]
  implicit lazy val consigneeDetailsGen: Typeclass[ConsigneeDetails]                                           = gen[ConsigneeDetails]
  implicit lazy val bankDetailsGen: Typeclass[BankDetails]                                                     = gen[BankDetails]
  implicit lazy val taxDetailsGen: Typeclass[TaxDetails]                                                       = gen[TaxDetails]
  implicit lazy val securityDetailsGen: Typeclass[SecurityDetails]                                             = gen[SecurityDetails]
  implicit lazy val ndrcDetailsGen: Typeclass[NdrcDetails]                                                     = gen[NdrcDetails]
  implicit lazy val responseCommonGen: Typeclass[ResponseCommon]                                               = gen[ResponseCommon]
  implicit lazy val responseDetailGen: Typeclass[ResponseDetail]                                               = gen[ResponseDetail]
  implicit lazy val overpaymentDeclarationDisplayResponseGen: Typeclass[OverpaymentDeclarationDisplayResponse] =
    gen[OverpaymentDeclarationDisplayResponse]
  implicit lazy val declarationInfoResponseGen: Typeclass[DeclarationResponse]                                 = gen[DeclarationResponse]
  implicit lazy val displayDeclarationGen: Typeclass[DisplayResponseDetail]                                    = gen[DisplayResponseDetail]
  implicit lazy val declarationGen: Typeclass[DisplayDeclaration]                                              = gen[DisplayDeclaration]
}
