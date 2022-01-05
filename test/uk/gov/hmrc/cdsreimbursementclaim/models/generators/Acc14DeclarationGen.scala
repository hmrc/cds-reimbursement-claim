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

import org.scalacheck.magnolia._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.EitherValues._
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.{AcceptanceDate, TemporalAccessorOps}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.{DisplayDeclaration, DisplayResponseDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.request.{DeclarationRequest, OverpaymentDeclarationDisplayRequest, RequestCommon, RequestDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.AddressGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.BankAccountDetailsGen.{genBankDetails, genMaskedBankDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.CMAEligibleGen.genWhetherCMAEligible
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ContactDetailsGen.genContactDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen.{genEori, genMRN}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.PaymentMethodGen.genPaymentMethod
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TaxCodesGen.genTaxCode

object Acc14DeclarationGen {

  lazy val genTaxDetails: Gen[TaxDetails] = for {
    taxType <- genTaxCode.map(_.value)
    amount  <- genBigDecimal
  } yield TaxDetails(
    taxType = taxType,
    amount = amount.toString()
  )

  lazy val genNdrcDetails: Gen[NdrcDetails] = for {
    taxType          <- genTaxCode.map(_.value)
    amount           <- Gen.choose(0L, 10000L).map(_.toString)
    paymentMethod    <- genPaymentMethod
    paymentReference <- genStringWithMaxSizeOfN(18)
    cmaEligible      <- genWhetherCMAEligible
  } yield NdrcDetails(taxType, amount, paymentMethod, paymentReference, cmaEligible)

  lazy val genAccountDetails: Gen[AccountDetails] =
    for {
      accountType    <- genStringWithMaxSizeOfN(10)
      accountNumber  <- genStringWithMaxSizeOfN(10)
      eori           <- genEori.map(_.value)
      legalName      <- genStringWithMaxSizeOfN(15)
      contactDetails <- Gen.option(genContactDetails)
    } yield AccountDetails(
      accountType,
      accountNumber,
      eori,
      legalName,
      contactDetails
    )

  lazy val genEstablishmentAddress: Gen[EstablishmentAddress] =
    for {
      num          <- Gen.choose(1, 100)
      street       <- genStringWithMaxSizeOfN(7)
      addressLine2 <- Gen.option(genStringWithMaxSizeOfN(10))
      addressLine3 <- Gen.option(genStringWithMaxSizeOfN(20))
      postalCode   <- Gen.option(genPostcode)
      countryCode  <- genCountry
    } yield EstablishmentAddress(
      s"$num $street",
      addressLine2,
      addressLine3,
      postalCode.map(_.value),
      countryCode.code
    )

  lazy val genDeclarantDetails: Gen[DeclarantDetails] = for {
    eori                 <- genEori
    legalName            <- genRandomString
    establishmentAddress <- genEstablishmentAddress
    contactDetails       <- genContactDetails
  } yield DeclarantDetails(
    EORI = eori,
    legalName = legalName,
    establishmentAddress = establishmentAddress,
    contactDetails = Some(contactDetails)
  )

  lazy val genConsigneeDetails: Gen[ConsigneeDetails] = for {
    eori                 <- genEori
    legalName            <- genRandomString
    establishmentAddress <- genEstablishmentAddress
    contactDetails       <- genContactDetails
  } yield ConsigneeDetails(
    EORI = eori,
    legalName = legalName,
    establishmentAddress = establishmentAddress,
    contactDetails = Some(contactDetails)
  )

  lazy val genSecurityDetails: Gen[SecurityDetails] =
    for {
      securityDepositId <- genRandomString
      totalAmount       <- genBigDecimal
      amountPaid        <- genBigDecimal
      paymentMethod     <- genPaymentMethod
      paymentReference  <- genRandomString
      taxDetails        <- Gen.nonEmptyListOf(genTaxDetails)
    } yield SecurityDetails(
      securityDepositId = securityDepositId,
      totalAmount = totalAmount.toString(),
      amountPaid = amountPaid.toString(),
      paymentMethod = paymentMethod,
      paymentReference = paymentReference,
      taxDetails = taxDetails
    )

  lazy val genDisplayDeclaration: Gen[DisplayDeclaration] = for {
    mrn                      <- genMRN
    acceptanceDate           <- genLocalDate.map(AcceptanceDate(_))
    declarantReferenceNumber <- Gen.option(genRandomString)
    securityReason           <- Gen.option(genRandomString)
    btaDueDate               <- Gen.option(genLocalDate.map(_.toIsoLocalDate))
    procedureCode            <- genStringWithMaxSizeOfN(5)
    btaSource                <- Gen.option(genRandomString)
    declarantDetails         <- genDeclarantDetails
    consigneeDetails         <- Gen.option(genConsigneeDetails)
    accountDetails           <- Gen.option(Gen.nonEmptyListOf(genAccountDetails))
    bankDetails              <- Gen.option(genBankDetails)
    maskedBankDetails        <- Gen.option(genMaskedBankDetails)
    ndrcDetails              <- Gen.option(Gen.nonEmptyListOf(genNdrcDetails))
  } yield DisplayDeclaration(
    DisplayResponseDetail(
      declarationId = mrn.value,
      acceptanceDate = acceptanceDate.toDisplayString.toEither.value,
      declarantReferenceNumber = declarantReferenceNumber,
      securityReason = securityReason,
      btaDueDate = btaDueDate,
      procedureCode = procedureCode,
      btaSource = btaSource,
      declarantDetails = declarantDetails,
      consigneeDetails = consigneeDetails,
      accountDetails = accountDetails,
      bankDetails = bankDetails,
      maskedBankDetails = maskedBankDetails,
      ndrcDetails = ndrcDetails
    )
  )

  lazy val genResponseDetail: Gen[ResponseDetail] =
    for {
      mrn                      <- genMRN
      acceptanceDate           <- genLocalDate.map(_.toIsoLocalDate)
      declarantReferenceNumber <- Gen.option(genRandomString)
      securityReason           <- Gen.option(genRandomString)
      btaDueDate               <- Gen.option(genLocalDate.map(_.toIsoLocalDate))
      procedureCode            <- genStringWithMaxSizeOfN(5)
      btaSource                <- Gen.option(genRandomString)
      declarantDetails         <- genDeclarantDetails
      consigneeDetails         <- Gen.option(genConsigneeDetails)
      accountDetails           <- Gen.option(Gen.nonEmptyListOf(genAccountDetails))
      bankDetails              <- Gen.option(genBankDetails)
      ndrcDetails              <- Gen.option(Gen.nonEmptyListOf(genNdrcDetails))
    } yield ResponseDetail(
      declarationId = mrn.value,
      acceptanceDate = acceptanceDate,
      declarantReferenceNumber = declarantReferenceNumber,
      securityReason = securityReason,
      btaDueDate = btaDueDate,
      procedureCode = procedureCode,
      btaSource = btaSource,
      declarantDetails = declarantDetails,
      consigneeDetails = consigneeDetails,
      accountDetails = accountDetails,
      bankDetails = bankDetails,
      ndrcDetails = ndrcDetails
    )

  implicit lazy val arbitraryTaxDetails: Typeclass[TaxDetails] =
    Arbitrary(genTaxDetails)

  implicit lazy val arbitraryNdrcDetails: Typeclass[NdrcDetails] =
    Arbitrary(genNdrcDetails)

  implicit lazy val arbitraryAccountDetails: Typeclass[AccountDetails] =
    Arbitrary(genAccountDetails)

  implicit lazy val arbitraryDeclarantDetails: Typeclass[DeclarantDetails] =
    Arbitrary(genDeclarantDetails)

  implicit lazy val arbitraryConsigneeDetails: Typeclass[ConsigneeDetails] =
    Arbitrary(genConsigneeDetails)

  implicit lazy val arbitrarySecurityDetails: Typeclass[SecurityDetails] =
    Arbitrary(genSecurityDetails)

  implicit lazy val arbitraryDisplayDeclaration: Typeclass[DisplayDeclaration] =
    Arbitrary(genDisplayDeclaration)

  implicit lazy val arbitraryResponseDetail: Typeclass[ResponseDetail] =
    Arbitrary(genResponseDetail)

  implicit lazy val arbitraryRequestCommon: Typeclass[RequestCommon] =
    gen[RequestCommon]

  implicit lazy val arbitraryRequestDetail: Typeclass[RequestDetail] =
    gen[RequestDetail]

  implicit lazy val arbitraryOverpaymentDeclarationDisplayRequest: Typeclass[OverpaymentDeclarationDisplayRequest] =
    gen[OverpaymentDeclarationDisplayRequest]

  implicit lazy val arbitraryDeclarationRequest: Typeclass[DeclarationRequest] =
    gen[DeclarationRequest]

  implicit lazy val arbitraryResponseCommon: Typeclass[ResponseCommon] =
    gen[ResponseCommon]

  implicit lazy val arbitraryOverpaymentDeclarationDisplayResponse: Typeclass[OverpaymentDeclarationDisplayResponse] =
    gen[OverpaymentDeclarationDisplayResponse]

  implicit lazy val arbitraryDeclarationInfoResponse: Typeclass[DeclarationResponse] =
    gen[DeclarationResponse]
}
