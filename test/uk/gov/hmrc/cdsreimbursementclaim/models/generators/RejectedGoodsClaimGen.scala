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

import org.scalacheck.magnolia.{Typeclass, gen}
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.TemporalAccessorOps
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Acc14DeclarationGen.genDisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.AddressGen.{genCountry, genPostcode}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.BankAccountDetailsGen.genBankAccountDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen.{genEori, genMRN}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TPI05RequestGen.genContactInformation
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TaxCodesGen.genTaxCode
import java.net.URL
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.NdrcDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import scala.jdk.CollectionConverters.asScalaBufferConverter

@SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
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

  lazy val genReimbursementClaims: Gen[Map[TaxCode, BigDecimal]] = for {
    num   <- Gen.choose(2, 5)
    codes <- Gen
               .listOfN(num, genTaxCode.flatMap(taxCode => Gen.posNum[Long].map(num => (taxCode, BigDecimal(num)))))
               .map(_.toMap)
  } yield codes

  lazy val genScheduledReimbursementClaims: Gen[Map[String, Map[TaxCode, AmountPaidWithRefund]]] =
    Gen
      .nonEmptyListOf(
        Gen
          .nonEmptyListOf(Gen.oneOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i'))
          .map(String.valueOf)
          .flatMap { id =>
            Gen
              .nonEmptyListOf(
                genTaxCode.flatMap(taxCode =>
                  Gen.posNum[Long].map(num => (taxCode, AmountPaidWithRefund(BigDecimal(num + 1), BigDecimal(num))))
                )
              )
              .map(list => id -> list.toMap)
          }
      )
      .map(_.toMap)

  lazy val genInspectionAddress: Gen[InspectionAddress] =
    for {
      num                   <- Gen.choose(1, 100)
      street                <- genStringWithMaxSizeOfN(7)
      addressLine2          <- genStringWithMaxSizeOfN(10)
      city                  <- genRandomString
      country               <- genCountry
      postalCode            <- genPostcode
      inspectionAddressType <- genInspectionAddressType
    } yield InspectionAddress(
      addressLine1 = Some(s"$num $street"),
      addressLine2 = Some(addressLine2),
      addressLine3 = None,
      city = Some(city),
      countryCode = Some(country.code),
      postalCode = Some(postalCode),
      addressType = inspectionAddressType
    )

  lazy val genClaimantInformation: Gen[ClaimantInformation] = for {
    eori               <- genEori
    fullName           <- genRandomString
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

  lazy val genSingleRejectedGoodsClaim: Gen[(SingleRejectedGoodsClaim, DisplayDeclaration)] =
    for {
      mrn                    <- genMRN
      claimantType           <- Gen.oneOf(ClaimantType.values)
      claimantInformation    <- genClaimantInformation
      basisOfClaim           <- genBasisOfRejectedGoodsClaim
      specialCircumstances   <- Gen.option(genRandomString)
      methodOfDisposal       <- genMethodOfDisposal
      bankAccountDetails     <- Gen.option(genBankAccountDetails)
      inspectionDate         <- genLocalDate
      inspectionAddress      <- genInspectionAddress
      detailsOfRejectedGoods <- genRandomString
      reimbursementMethod    <- Gen.oneOf(ReimbursementMethodAnswer.values)
      declaration            <- genDisplayDeclaration.map { generatedDeclaration =>
                                  val drd = generatedDeclaration.displayResponseDetail.copy(declarationId = mrn.value)
                                  DisplayDeclaration(drd)
                                }
      claims                 <- genClaimsFromDisplayDeclaration(declaration)
      evidences              <- Gen.nonEmptyListOf(genEvidences)
    } yield (
      SingleRejectedGoodsClaim(
        movementReferenceNumber = mrn,
        claimantType = claimantType,
        claimantInformation = claimantInformation,
        basisOfClaim = basisOfClaim,
        basisOfClaimSpecialCircumstances = specialCircumstances,
        methodOfDisposal = methodOfDisposal,
        detailsOfRejectedGoods = detailsOfRejectedGoods,
        inspectionDate = inspectionDate,
        inspectionAddress = inspectionAddress,
        reimbursementClaims = claims._2,
        reimbursementMethod = reimbursementMethod,
        bankAccountDetails = bankAccountDetails,
        supportingEvidences = evidences
      ),
      declaration
    )

  def genClaimAmount(ndrcDetails: List[NdrcDetails]): Gen[Map[TaxCode, BigDecimal]] = {
    val possibleValues = ndrcDetails.map(detail => TaxCode.getOrFail(detail.taxType) -> detail.amount.toDouble)
    Gen
      .sequence(possibleValues.map { case (taxCode, maxAmount) =>
        for {
          amount <- Gen.choose[Double](0, maxAmount).map(BigDecimal(_))
        } yield taxCode -> amount
      })
      .map(_.asScala.toMap)
  }

  def genClaimsFromDisplayDeclaration(displayDeclaration: DisplayDeclaration): Gen[(MRN, Map[TaxCode, BigDecimal])] =
    for {
      claimAmount <- genClaimAmount(displayDeclaration.displayResponseDetail.ndrcDetails.toList.flatten)
    } yield (MRN(displayDeclaration.displayResponseDetail.declarationId), claimAmount)

  lazy val genMultipleRejectedGoodsClaim: Gen[(MultipleRejectedGoodsClaim, List[DisplayDeclaration])] =
    for {
      numMrns                <- Gen.choose(2, 10)
      mrns                   <- Gen.listOfN(numMrns, genMRN)
      claimantType           <- Gen.oneOf(ClaimantType.values)
      claimantInformation    <- genClaimantInformation
      basisOfClaim           <- genBasisOfRejectedGoodsClaim
      specialCircumstances   <- Gen.option(genRandomString)
      methodOfDisposal       <- genMethodOfDisposal
      bankAccountDetails     <- Gen.option(genBankAccountDetails)
      inspectionDate         <- genLocalDate
      inspectionAddress      <- genInspectionAddress
      detailsOfRejectedGoods <- genRandomString
      reimbursementMethod    <- Gen.oneOf(ReimbursementMethodAnswer.values)
      numEvidences           <- Gen.choose(2, 5)
      evidences              <- Gen.listOfN(numEvidences, genEvidences)
      declarations           <- Gen
                                  .sequence(
                                    mrns.map(mrn =>
                                      genDisplayDeclaration.map { generatedDeclaration =>
                                        val drd = generatedDeclaration.displayResponseDetail.copy(declarationId = mrn.value)
                                        DisplayDeclaration(drd)
                                      }
                                    )
                                  )
                                  .map(_.asScala.toList)
      claims                 <- Gen.sequence(declarations.map(declaration => genClaimsFromDisplayDeclaration(declaration)))
    } yield (
      MultipleRejectedGoodsClaim(
        movementReferenceNumbers = mrns,
        claimantType = claimantType,
        claimantInformation = claimantInformation,
        basisOfClaim = basisOfClaim,
        basisOfClaimSpecialCircumstances = specialCircumstances,
        methodOfDisposal = methodOfDisposal,
        detailsOfRejectedGoods = detailsOfRejectedGoods,
        inspectionDate = inspectionDate,
        inspectionAddress = inspectionAddress,
        reimbursementClaims = claims.asScala.toMap,
        reimbursementMethod = reimbursementMethod,
        bankAccountDetails = bankAccountDetails,
        supportingEvidences = evidences
      ),
      declarations
    )

  lazy val genScheduledRejectedGoodsClaim: Gen[ScheduledRejectedGoodsClaim] =
    for {
      mrn                    <- genMRN
      claimantType           <- Gen.oneOf(ClaimantType.values)
      claimantInformation    <- genClaimantInformation
      basisOfClaim           <- genBasisOfRejectedGoodsClaim
      specialCircumstances   <- Gen.option(genRandomString)
      methodOfDisposal       <- genMethodOfDisposal
      bankAccountDetails     <- Gen.option(genBankAccountDetails)
      inspectionDate         <- genLocalDate
      inspectionAddress      <- genInspectionAddress
      detailsOfRejectedGoods <- genRandomString
      reimbursementMethod    <- Gen.oneOf(ReimbursementMethodAnswer.values)
      claims                 <- genScheduledReimbursementClaims
      numEvidences           <- Gen.choose(2, 5)
      evidences              <- Gen.listOfN(numEvidences, genEvidences)
      scheduledDocument      <- genEvidences
    } yield ScheduledRejectedGoodsClaim(
      movementReferenceNumber = mrn,
      claimantType = claimantType,
      claimantInformation = claimantInformation,
      basisOfClaim = basisOfClaim,
      basisOfClaimSpecialCircumstances = specialCircumstances,
      methodOfDisposal = methodOfDisposal,
      detailsOfRejectedGoods = detailsOfRejectedGoods,
      inspectionDate = inspectionDate,
      inspectionAddress = inspectionAddress,
      reimbursementClaims = claims,
      reimbursementMethod = reimbursementMethod,
      bankAccountDetails = bankAccountDetails,
      supportingEvidences = evidences,
      scheduledDocument = scheduledDocument
    )

  implicit def arbitraryRequest[Claim <: RejectedGoodsClaim](implicit
    arb: Arbitrary[Claim]
  ): Typeclass[RejectedGoodsClaimRequest[Claim]] =
    gen[RejectedGoodsClaimRequest[Claim]]

  implicit lazy val arbitrarySingleClaimDetails: Typeclass[(SingleRejectedGoodsClaim, DisplayDeclaration)] =
    Arbitrary(genSingleRejectedGoodsClaim)

  implicit lazy val arbitrarySingleRejectedGoodsClaim: Typeclass[SingleRejectedGoodsClaim] =
    Arbitrary(
      for {
        (claim, _) <- genSingleRejectedGoodsClaim
      } yield claim
    )

  implicit lazy val arbitraryMultipleClaimDetails: Typeclass[(MultipleRejectedGoodsClaim, List[DisplayDeclaration])] =
    Arbitrary(genMultipleRejectedGoodsClaim)

  implicit lazy val arbitraryMultipleRejectedGoodsClaim: Typeclass[MultipleRejectedGoodsClaim] =
    Arbitrary(
      for {
        (claim, _) <- genMultipleRejectedGoodsClaim
      } yield claim
    )

  implicit lazy val arbitraryScheduledClaimDetails: Typeclass[(ScheduledRejectedGoodsClaim, DisplayDeclaration)] =
    Arbitrary(
      for {
        declaration <- genDisplayDeclaration
        claim       <- genScheduledRejectedGoodsClaim
      } yield (claim, declaration)
    )

  implicit lazy val arbitraryScheduledRejectedGoodsClaim: Typeclass[ScheduledRejectedGoodsClaim] =
    Arbitrary(genScheduledRejectedGoodsClaim)
}
