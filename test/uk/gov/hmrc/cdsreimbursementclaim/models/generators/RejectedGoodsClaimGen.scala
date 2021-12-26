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

import org.scalacheck.magnolia.{Typeclass, gen}
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.TemporalAccessorOps
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.AddressGen.{genCountry, genPostcode}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.BankAccountDetailsGen.genBankAccountDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen.{genEori, genMRN}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TPI05RequestGen.genContactInformation
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TaxCodesGen.genTaxCode

import java.net.URL

object RejectedGoodsClaimGen {

  lazy val genUrl: Gen[URL] =
    for {
      protocol <- Gen.oneOf("http", "https")
      hostname <- genStringWithMaxSizeOfN(7)
      domain   <- Gen.oneOf("com", "co.uk", "lv")
    } yield new URL(s"$protocol://$hostname.$domain")

  lazy val genFileName: Gen[String] = for {
    name      <- genStringWithMaxSizeOfN(6)
    extension <- Gen.oneOf("pdf", "doc", "csv")
  } yield s"$name.$extension"

  lazy val genMethodOfDisposal: Gen[MethodOfDisposal] =
    Gen.oneOf(MethodOfDisposal.values)

  lazy val genInspectionDate: Gen[String] =
    genLocalDate.map(_.toIsoLocalDate)

  lazy val genInspectionAddressType: Gen[InspectionAddressType] =
    Gen.oneOf(InspectionAddressType.values)

  lazy val genBasisOfRejectedGoodsClaim: Gen[BasisOfRejectedGoodsClaim] =
    Gen.oneOf(BasisOfRejectedGoodsClaim.values)

  lazy val genReimbursementClaims: Gen[Map[TaxCode, BigDecimal]] =
    Gen
      .nonEmptyListOf(
        genTaxCode.flatMap(taxCode => genBigDecimal.map((taxCode, _)))
      )
      .map(_.toMap)

  lazy val genInspectionAddress: Gen[InspectionAddress] =
    for {
      num          <- Gen.choose(1, 100)
      street       <- genStringWithMaxSizeOfN(7)
      addressLine2 <- genStringWithMaxSizeOfN(10)
      city         <- genRandomString
      country      <- genCountry
      postalCode   <- genPostcode
    } yield InspectionAddress(
      addressLine1 = s"$num $street",
      addressLine2 = addressLine2,
      city = city,
      countryCode = country.code,
      postalCode = postalCode
    )

  lazy val genClaimantInformation: Gen[ClaimantInformation] = for {
    eori               <- genEori
    fullName           <- Gen.option(genRandomString)
    contactInformation <- genContactInformation
  } yield ClaimantInformation(eori, fullName, contactInformation, contactInformation)

  lazy val genEvidences: Gen[EvidenceDocument] =
    for {
      uuid         <- genUUID
      url          <- genUrl
      fileName     <- genFileName
      size         <- Gen.chooseNum(10L, 1000L)
      uploadTime   <- genLocalDateTime
      documentType <- Gen.oneOf(UploadDocumentType.values)
    } yield EvidenceDocument(
      checksum = uuid.toString.replace("-", ""),
      downloadUrl = url.toString,
      fileName = fileName,
      fileMimeType = fileName
        .split(".")
        .lastOption
        .map(s => if (s == "pdf") s"application/$s" else s"text/$s")
        .getOrElse("application/octet-stream"),
      size = size,
      uploadedOn = uploadTime,
      documentType = documentType
    )

  lazy val genRejectedGoodsClaim: Gen[RejectedGoodsClaim] =
    for {
      mrn                    <- genMRN
      claimantType           <- Gen.oneOf(ClaimantType.values)
      claimantInformation    <- genClaimantInformation
      basisOfClaim           <- genBasisOfRejectedGoodsClaim
      specialCircumstances   <- Gen.option(genRandomString)
      methodOfDisposal       <- genMethodOfDisposal
      bankAccountDetails     <- Gen.option(genBankAccountDetails)
      inspectionDate         <- genLocalDate
      inspectionAddressType  <- genInspectionAddressType
      inspectionAddress      <- genInspectionAddress
      detailsOfRejectedGoods <- genRandomString
      reimbursementMethod    <- Gen.oneOf(ReimbursementMethodAnswer.values)
      claims                 <- genReimbursementClaims
      evidences              <- Gen.nonEmptyListOf(genEvidences)
    } yield RejectedGoodsClaim(
      movementReferenceNumber = mrn,
      claimantType = claimantType,
      claimantInformation = claimantInformation,
      basisOfClaim = basisOfClaim,
      basisOfClaimSpecialCircumstances = specialCircumstances,
      methodOfDisposal = methodOfDisposal,
      detailsOfRejectedGoods = detailsOfRejectedGoods,
      inspectionDate = inspectionDate,
      inspectionAddressType = inspectionAddressType,
      inspectionAddress = inspectionAddress,
      reimbursementClaims = claims,
      reimbursementMethod = reimbursementMethod,
      bankAccountDetails = bankAccountDetails,
      supportingEvidences = evidences
    )

  implicit lazy val arbitraryRejectedGoodsClaim: Typeclass[RejectedGoodsClaim] =
    Arbitrary(genRejectedGoodsClaim)

  implicit lazy val arbitraryRejectedGoodsClaimRequest: Typeclass[RejectedGoodsClaimRequest] =
    gen[RejectedGoodsClaimRequest]
}
