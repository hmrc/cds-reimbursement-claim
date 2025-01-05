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

package uk.gov.hmrc.cdsreimbursementclaim.models.generators

import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.cdsreimbursementclaim.models.CDFPayService
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ClaimSubmitResponse
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.{ISO8601DateTime, ISOLocalDate, TemporalAccessorOps}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.{C285, CE1179}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.AddressGen.{genCountry, genPostcode}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.BankAccountDetailsGen.{genAccountName, genAccountNumber, genSortCode}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.C285ClaimGen.genBasisOfClaim
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.CMAEligibleGen.genWhetherCMAEligible
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ContactDetailsGen.{genEmail, genUkPhoneNumber}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen.{genEori, genMRN}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.PaymentMethodGen.genPaymentMethod
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.RejectedGoodsClaimGen.{genBasisOfRejectedGoodsClaim, genInspectionAddressType, genInspectionDate, genMethodOfDisposal}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TaxCodesGen.genTaxCode
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps

object TPI05RequestGen {

  lazy val genNdrcDetails: Gen[NdrcDetails] = for {
    paymentMethod      <- genPaymentMethod
    paymentReference   <- genStringWithMaxSizeOfN(18)
    whetherCMAEligible <- genWhetherCMAEligible
    taxType            <- genTaxCode
    paidAmount         <- genBigDecimal.map(_.toString)
    claimedAmount      <- genBigDecimal.map(_.toString)
  } yield NdrcDetails(
    paymentMethod = paymentMethod,
    paymentReference = paymentReference,
    CMAEligible = whetherCMAEligible,
    taxType = taxType,
    amount = paidAmount,
    claimAmount = Some(claimedAmount),
    None
  )

  lazy val genBankDetails: Gen[BankDetails] = for {
    accountName   <- genAccountName
    sortCode      <- genSortCode
    accountNumber <- genAccountNumber
  } yield BankDetails(
    consigneeBankDetails = Some(
      BankDetail(
        accountHolderName = accountName,
        sortCode = sortCode,
        accountNumber = accountNumber
      )
    ),
    declarantBankDetails = Some(
      BankDetail(
        accountHolderName = accountName,
        sortCode = sortCode,
        accountNumber = accountNumber
      )
    )
  )

  lazy val genAddress: Gen[Address] =
    for {
      firstName    <- genStringWithMaxSizeOfN(10)
      lastName     <- genStringWithMaxSizeOfN(10)
      telephone    <- genUkPhoneNumber
      email        <- genEmail
      num          <- Gen.choose(1, 100)
      street       <- genStringWithMaxSizeOfN(7)
      addressLine2 <- Gen.option(genStringWithMaxSizeOfN(10))
      addressLine3 <- Gen.option(genStringWithMaxSizeOfN(20))
      city         <- Gen.option(genRandomString)
      country      <- genCountry
      postalCode   <- Gen.option(genPostcode)
    } yield Address(
      contactPerson = Some(s"$firstName $lastName"),
      addressLine1 = Some(s"$num $street"),
      addressLine2 = addressLine2,
      addressLine3 = addressLine3,
      street = Some(street),
      city = city,
      countryCode = country.code,
      postalCode = postalCode.map(_.value),
      telephoneNumber = Some(telephone.value),
      emailAddress = Some(email.value)
    )

  lazy val genInspectionAddress: Gen[InspectionAddress] =
    for {
      num          <- Gen.choose(1, 100)
      street       <- genStringWithMaxSizeOfN(7)
      addressLine2 <- genStringWithMaxSizeOfN(10)
      city         <- genRandomString
      country      <- genCountry
      postalCode   <- genPostcode
    } yield InspectionAddress(
      addressLine1 = Some(s"$num $street"),
      addressLine2 = Some(addressLine2),
      addressLine3 = None,
      city = Some(city),
      countryCode = Some(country.code),
      postalCode = Some(postalCode)
    )

  lazy val genAccountDetail: Gen[AccountDetail] =
    for {
      accountType    <- genStringWithMaxSizeOfN(10)
      accountNumber  <- genStringWithMaxSizeOfN(10)
      eori           <- genEori.map(_.value)
      legalName      <- genStringWithMaxSizeOfN(15)
      contactDetails <- Gen.option(genContactInformation)
    } yield AccountDetail(
      accountType = accountType,
      accountNumber = accountNumber,
      EORI = eori,
      legalName = legalName,
      contactDetails = contactDetails
    )

  lazy val genContactInformation: Gen[ContactInformation] = for {
    firstName    <- genStringWithMaxSizeOfN(10)
    lastName     <- genStringWithMaxSizeOfN(10)
    email        <- genEmail
    telephone    <- genUkPhoneNumber
    num          <- Gen.choose(1, 100)
    street       <- genStringWithMaxSizeOfN(7)
    addressLine2 <- Gen.option(genStringWithMaxSizeOfN(10))
    addressLine3 <- Gen.option(genStringWithMaxSizeOfN(20))
    city         <- Gen.option(genRandomString)
    country      <- Gen.option(genCountry)
    postalCode   <- Gen.option(genPostcode)
  } yield ContactInformation(
    contactPerson = Some(s"$firstName $lastName"),
    addressLine1 = Some(s"$num $street"),
    addressLine2 = addressLine2,
    addressLine3 = addressLine3,
    street = Some(street),
    city = city,
    countryCode = country.map(_.code),
    postalCode = postalCode.map(_.value),
    telephoneNumber = Some(telephone.value),
    faxNumber = None,
    emailAddress = Some(email.value)
  )

  def genEoriInformation(eori: Eori): Gen[EORIInformation] =
    for {
      fullName             <- genRandomString
      establishmentAddress <- genAddress
      contactInformation   <- genContactInformation
    } yield EORIInformation(
      EORINumber = eori,
      CDSFullName = fullName,
      CDSEstablishmentAddress = establishmentAddress,
      contactInformation = Some(contactInformation)
    )

  lazy val genMrnInformation: Gen[MRNInformation] =
    for {
      eori                 <- genEori
      legalName            <- genRandomString
      establishmentAddress <- genAddress
      contactInformation   <- genContactInformation
    } yield MRNInformation(
      EORI = eori,
      legalName = legalName,
      establishmentAddress = establishmentAddress,
      contactDetails = Some(contactInformation)
    )

  lazy val genMrnDetails: Gen[MrnDetail] =
    for {
      mrn                      <- genMRN
      acceptanceDate           <- genLocalDate.map(_.toIsoLocalDate)
      declarantReferenceNumber <- Gen.option(genRandomString)
      procedureCode            <- genStringWithMaxSizeOfN(5)
      accountDetails           <- Gen.option(Gen.nonEmptyListOf(genAccountDetail))
      declarantDetails         <- genMrnInformation
      consigneeDetails         <- Gen.option(genMrnInformation)
      bankDetails              <- Gen.option(genBankDetails)
      ndrcDetails              <- Gen.option(Gen.nonEmptyListOf(genNdrcDetails))
    } yield new MrnDetail(
      MRNNumber = Some(mrn),
      acceptanceDate = Some(acceptanceDate),
      declarantReferenceNumber = declarantReferenceNumber,
      mainDeclarationReference = Some(true),
      procedureCode = Some(procedureCode),
      declarantDetails = Some(declarantDetails),
      accountDetails = accountDetails,
      consigneeDetails = consigneeDetails,
      bankDetails = bankDetails,
      NDRCDetails = ndrcDetails
    )

  lazy val genC285ClaimRequestDetail: Gen[RequestDetail] =
    for {
      cdfPayService        <- Gen.oneOf(CDFPayService.values)
      caseType             <- Gen.oneOf(CaseType.values)
      declarationMode      <- Gen.oneOf(DeclarationMode.values)
      claimAmountTotal     <- genBigDecimal.map(_.roundToTwoDecimalPlaces)
      reimbursementMethod  <- Gen.oneOf(ReimbursementMethod.values)
      basisOfClaim         <- genBasisOfClaim
      claimant             <- Gen.oneOf(Claimant.values)
      claimantEORI         <- genEori
      claimantEmailAddress <- genEmail
      claimantName         <- genAccountName.map(_.value)
      descOfGoods          <- genRandomString
      isPrivateImporter    <- Gen.oneOf(YesNo.values)
      eoriInformation      <- genEoriInformation(claimantEORI)
      mrnDetails           <- genMrnDetails
      duplicateMrnDetails  <- Gen.option(genMrnDetails)
    } yield RequestDetail(
      CDFPayService = cdfPayService,
      dateReceived = Some(ISO8601DateTime.now),
      claimType = Some(C285),
      caseType = Some(caseType),
      customDeclarationType = Some(CustomDeclarationType.MRN),
      declarationMode = Some(declarationMode),
      claimDate = Some(ISOLocalDate.now),
      claimAmountTotal = Some(claimAmountTotal.toString()),
      reimbursementMethod = Some(reimbursementMethod),
      basisOfClaim = Some(basisOfClaim.toTPI05DisplayString),
      claimant = Some(claimant),
      payeeIndicator = Some(claimant),
      claimantEORI = claimantEORI,
      claimantEmailAddress = claimantEmailAddress,
      claimantName = Some(claimantName),
      goodsDetails = Some(
        GoodsDetails(
          descOfGoods = Some(descOfGoods),
          isPrivateImporter = Some(isPrivateImporter)
        )
      ),
      EORIDetails = Some(
        EoriDetails(
          agentEORIDetails = eoriInformation,
          importerEORIDetails = eoriInformation
        )
      ),
      MRNDetails = Some(mrnDetails :: Nil),
      duplicateMRNDetails = duplicateMrnDetails
    )

  lazy val genRejectedGoodsClaimRequestDetail: Gen[RequestDetail] =
    for {
      cdfPayService         <- Gen.oneOf(CDFPayService.values)
      claimAmountTotal      <- genBigDecimal.map(_.roundToTwoDecimalPlaces)
      disposalMethod        <- genMethodOfDisposal
      reimbursementMethod   <- Gen.oneOf(ReimbursementMethod.values)
      basisOfClaim          <- genBasisOfRejectedGoodsClaim
      claimant              <- Gen.oneOf(Claimant.values)
      claimantEORI          <- genEori
      claimantEmailAddress  <- genEmail
      claimantName          <- genAccountName.map(_.value)
      descOfGoods           <- genRandomString
      specialCircumstances  <- genRandomString
      inspectionDate        <- genInspectionDate
      inspectionAddressType <- genInspectionAddressType
      inspectionAddress     <- genInspectionAddress
      eoriInformation       <- genEoriInformation(claimantEORI)
      mrnDetails            <- genMrnDetails
    } yield RequestDetail(
      CDFPayService = cdfPayService,
      dateReceived = Some(ISO8601DateTime.now),
      claimType = Some(CE1179),
      customDeclarationType = Some(CustomDeclarationType.MRN),
      claimDate = Some(ISOLocalDate.now),
      claimAmountTotal = Some(claimAmountTotal.toString()),
      disposalMethod = Some(disposalMethod.toTPI05DisplayString),
      reimbursementMethod = Some(reimbursementMethod),
      basisOfClaim = Some(basisOfClaim.toTPI05DisplayString),
      claimant = Some(claimant),
      payeeIndicator = Some(claimant),
      claimantEORI = claimantEORI,
      claimantEmailAddress = claimantEmailAddress,
      claimantName = Some(claimantName),
      goodsDetails = Some(
        GoodsDetails(
          descOfGoods = Some(descOfGoods),
          anySpecialCircumstances = Some(specialCircumstances),
          dateOfInspection = Some(inspectionDate),
          atTheImporterOrDeclarantAddress = Some(inspectionAddressType.toTPI05DisplayString),
          inspectionAddress = Some(inspectionAddress)
        )
      ),
      EORIDetails = Some(
        EoriDetails(
          agentEORIDetails = eoriInformation,
          importerEORIDetails = eoriInformation
        )
      ),
      MRNDetails = Some(mrnDetails :: Nil)
    )

  lazy val genRequestCommon: Gen[RequestCommon] =
    genRandomString.map { acknowledgementReference =>
      RequestCommon(
        originatingSystem = "CDS",
        receiptDate = ISO8601DateTime.now,
        acknowledgementReference = acknowledgementReference
      )
    }

  lazy val genC285EisRequest = for {
    common <- genRequestCommon
    detail <- genC285ClaimRequestDetail
  } yield EisSubmitClaimRequest(PostNewClaimsRequest(common, detail))

  implicit lazy val arbitraryRequestCommon: Arbitrary[RequestCommon] =
    Arbitrary(genRequestCommon)

  implicit lazy val arbitraryRequestDetail: Arbitrary[RequestDetail] =
    Arbitrary(Gen.oneOf(genC285ClaimRequestDetail, genRejectedGoodsClaimRequestDetail))

  implicit lazy val arbitraryEisSubmitClaimRequest: Arbitrary[EisSubmitClaimRequest] =
    GeneratorUtils.gen[EisSubmitClaimRequest]

  // implicit lazy val arbitraryEisSubmitClaimResponse: Arbitrary[EisSubmitClaimResponse] =
  //   GeneratorUtils.gen[EisSubmitClaimResponse]

  implicit lazy val arbitraryClaimSubmitResponse: Arbitrary[ClaimSubmitResponse] =
    GeneratorUtils.gen[ClaimSubmitResponse]
}
