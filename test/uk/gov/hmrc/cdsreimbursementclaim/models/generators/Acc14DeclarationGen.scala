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
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.{AcceptanceDate, CdsDateTime, ISO8601DateTime, TemporalAccessorOps}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.{DisplayDeclaration, DisplayResponseDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.request.{DeclarationRequest, OverpaymentDeclarationDisplayRequest, RequestCommon, RequestDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.AddressGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.BankAccountDetailsGen.{genBankDetails, mask}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.CMAEligibleGen.genWhetherCMAEligible
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ContactDetailsGen.genContactDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen.{genEori, genMRN}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.PaymentMethodGen.genPaymentMethod
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ReasonForSecurityGen.genReasonForSecurity
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TaxCodesGen.genTaxCode
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN

import java.text.DecimalFormat

object Acc14DeclarationGen {

  lazy val genAcceptanceDate: Gen[AcceptanceDate] =
    genLocalDate.map(AcceptanceDate(_))

  lazy val genRequestCommon: Gen[RequestCommon] =
    Gen.uuid.map { acknowledgementReference =>
      RequestCommon(
        originatingSystem = Platform.MDTP,
        receiptDate = ISO8601DateTime.now,
        acknowledgementReference = acknowledgementReference.toString
      )
    }

  lazy val genRequestDetail: Gen[RequestDetail] =
    genMRN.map { mrn =>
      RequestDetail(
        declarationId = mrn.value,
        securityReason = None
      )
    }

  lazy val genResponseCommon: Gen[ResponseCommon] =
    Gen.const(
      ResponseCommon(
        status = "OK",
        statusText = None,
        processingDate = CdsDateTime.now,
        returnParameters = None
      )
    )

  lazy val genTaxDetails: Gen[TaxDetails] = for {
    taxType <- genTaxCode.map(_.value)
    amount  <- Gen.choose(1, 100).map(BigDecimal(_))
  } yield TaxDetails(
    taxType = taxType,
    amount = amount.toString()
  )

  lazy val genNdrcDetails: Gen[NdrcDetails] = {
    val formatter = new DecimalFormat()
    formatter.setMinimumFractionDigits(2)
    formatter.setGroupingUsed(false)

    for {
      taxType          <- genTaxCode.map(_.value)
      amount           <- Gen.choose(0L, 10000L).map(formatter.format)
      paymentMethod    <- genPaymentMethod
      paymentReference <- genStringWithMaxSizeOfN(18)
      cmaEligible      <- genWhetherCMAEligible
    } yield NdrcDetails(taxType, amount, paymentMethod, paymentReference, cmaEligible)
  }

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
      paymentMethod     <- Gen.oneOf("001", "004", "005")
      paymentReference  <- genRandomString
      taxDetails        <- Gen.nonEmptyMap(genTaxDetails.map(t => (t.taxType, t))).map(_.values.toList)
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
    acceptanceDate           <- genAcceptanceDate
    declarantReferenceNumber <- Gen.option(genRandomString)
    securityReason           <- Gen.option(genRandomString)
    btaDueDate               <- Gen.option(genLocalDate.map(_.toIsoLocalDate))
    procedureCode            <- genStringWithMaxSizeOfN(5)
    btaSource                <- Gen.option(genRandomString)
    declarantDetails         <- genDeclarantDetails
    consigneeDetails         <- genConsigneeDetails
    numAccDetails            <- Gen.choose(1, 5)
    accountDetails           <- Gen.option(Gen.listOfN(numAccDetails, genAccountDetails))
    bankDetails              <- genBankDetails
    maskedBankDetails        <- Gen.const(mask(bankDetails))
    numNdrcDetails           <- Gen.choose(1, 5)
    ndrcDetails              <- Gen.listOfN(numNdrcDetails, genNdrcDetails)
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
      consigneeDetails = Some(consigneeDetails),
      accountDetails = accountDetails,
      bankDetails = Some(bankDetails),
      maskedBankDetails = Some(maskedBankDetails),
      ndrcDetails = Some(ndrcDetails)
    )
  )

  lazy val genDisplayDeclarationWithSecurities: Gen[DisplayDeclaration] = for {
    mrn                      <- genMRN
    acceptanceDate           <- genAcceptanceDate
    declarantReferenceNumber <- Gen.option(genRandomString)
    securityReason           <- Gen.some(genReasonForSecurity.map(_.acc14Code))
    btaDueDate               <- Gen.option(genLocalDate.map(_.toIsoLocalDate))
    procedureCode            <- genStringWithMaxSizeOfN(5)
    btaSource                <- Gen.option(genRandomString)
    declarantDetails         <- genDeclarantDetails
    consigneeDetails         <- genConsigneeDetails
    numAccDetails            <- Gen.choose(1, 5)
    accountDetails           <- Gen.option(Gen.listOfN(numAccDetails, genAccountDetails))
    bankDetails              <- genBankDetails
    maskedBankDetails        <- Gen.const(mask(bankDetails))
    numNdrcDetails           <- Gen.choose(1, 5)
    ndrcDetails              <- Gen.listOfN(numNdrcDetails, genNdrcDetails)
    paymentMethod            <- Gen.oneOf("001", "004", "005")
    securityDetails          <- Gen.some(
                                  Gen
                                    .nonEmptyListOf(genSecurityDetails)
                                    .map(_.groupBy(_.securityDepositId).values.map(_.headOption.toList).toList.flatten)
                                    .map(_.map(_.copy(paymentMethod = paymentMethod)))
                                )

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
      consigneeDetails = Some(consigneeDetails),
      accountDetails = accountDetails,
      bankDetails = Some(bankDetails),
      maskedBankDetails = Some(maskedBankDetails),
      ndrcDetails = Some(ndrcDetails),
      securityDetails = securityDetails
    )
  )
  def genDisplayDeclarationWithSecurityReason(reasonForSecurity: Option[String]): Gen[DisplayDeclaration] =
    genDisplayDeclarationWithSecurityReason(reasonForSecurity, None)
  def genDisplayDeclarationWithSecurityReason(
    reasonForSecurity: Option[String],
    maybeMrn: Option[MRN]
  ): Gen[DisplayDeclaration]                                                                              = for {
    mrn                      <- genMRN
    acceptanceDate           <- genAcceptanceDate
    declarantReferenceNumber <- Gen.option(genRandomString)
    btaDueDate               <- Gen.option(genLocalDate.map(_.toIsoLocalDate))
    procedureCode            <- genStringWithMaxSizeOfN(5)
    btaSource                <- Gen.option(genRandomString)
    declarantDetails         <- genDeclarantDetails
    consigneeDetails         <- genConsigneeDetails
    numAccDetails            <- Gen.choose(1, 5)
    accountDetails           <- Gen.option(Gen.listOfN(numAccDetails, genAccountDetails))
    bankDetails              <- genBankDetails
    maskedBankDetails        <- Gen.const(mask(bankDetails))
    numNdrcDetails           <- Gen.choose(1, 5)
    ndrcDetails              <- Gen.listOfN(numNdrcDetails, genNdrcDetails)
  } yield DisplayDeclaration(
    DisplayResponseDetail(
      declarationId = maybeMrn.getOrElse(mrn).value,
      acceptanceDate = acceptanceDate.toDisplayString.toEither.value,
      declarantReferenceNumber = declarantReferenceNumber,
      securityReason = reasonForSecurity,
      btaDueDate = btaDueDate,
      procedureCode = procedureCode,
      btaSource = btaSource,
      declarantDetails = declarantDetails,
      consigneeDetails = Some(consigneeDetails),
      accountDetails = accountDetails,
      bankDetails = Some(bankDetails),
      maskedBankDetails = Some(maskedBankDetails),
      ndrcDetails = Some(ndrcDetails)
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
      ndrcDetails = ndrcDetails,
      securityDetails = None
    )

  lazy val genResponseDetailWithSecurities: Gen[ResponseDetail] =
    for {
      mrn                      <- genMRN
      acceptanceDate           <- genLocalDate.map(_.toIsoLocalDate)
      declarantReferenceNumber <- Gen.option(genRandomString)
      securityReason           <- Gen.some(genRandomString)
      procedureCode            <- genStringWithMaxSizeOfN(5)
      btaDueDate               <- Gen.option(genLocalDate.map(_.toIsoLocalDate))
      btaSource                <- Gen.option(genRandomString)
      declarantDetails         <- genDeclarantDetails
      consigneeDetails         <- Gen.option(genConsigneeDetails)
      accountDetails           <- Gen.option(Gen.nonEmptyListOf(genAccountDetails))
      bankDetails              <- Gen.some(genBankDetails)
      securityDetails          <- Gen.some(Gen.nonEmptyListOf(genSecurityDetails))
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
      ndrcDetails = None,
      securityDetails = securityDetails
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
    Arbitrary(genRequestCommon)

  implicit lazy val arbitraryRequestDetail: Typeclass[RequestDetail] =
    Arbitrary(genRequestDetail)

  implicit lazy val arbitraryOverpaymentDeclarationDisplayRequest: Typeclass[OverpaymentDeclarationDisplayRequest] =
    gen[OverpaymentDeclarationDisplayRequest]

  implicit lazy val arbitraryDeclarationRequest: Typeclass[DeclarationRequest] =
    gen[DeclarationRequest]

  implicit lazy val arbitraryResponseCommon: Typeclass[ResponseCommon] =
    Arbitrary(genResponseCommon)

  implicit lazy val arbitraryOverpaymentDeclarationDisplayResponse: Typeclass[OverpaymentDeclarationDisplayResponse] =
    gen[OverpaymentDeclarationDisplayResponse]

  implicit lazy val arbitraryDeclarationInfoResponse: Typeclass[DeclarationResponse] =
    gen[DeclarationResponse]

  implicit lazy val arbitraryAcceptanceDate: Typeclass[AcceptanceDate] =
    Arbitrary(genAcceptanceDate)
}
