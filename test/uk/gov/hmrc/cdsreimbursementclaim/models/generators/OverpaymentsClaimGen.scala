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

import cats.implicits.catsSyntaxEq
import org.scalacheck.magnolia.Typeclass
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.AuthenticatedUser
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.BasisOfClaim.DuplicateEntry
import uk.gov.hmrc.cdsreimbursementclaim.models.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.NdrcDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Acc14DeclarationGen.genDisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.BankAccountDetailsGen.genBankAccountDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ContactDetailsGen.genEmail
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen.{genEori, genMRN}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TPI05RequestGen.genContactInformation
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TaxCodesGen.genTaxCode
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.services.tpi05.OverpaymentsSingleClaimData

import java.net.URL
import scala.jdk.CollectionConverters.asScalaBufferConverter

@SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
object OverpaymentsClaimGen {

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

  lazy val genBasisOfClaim: Gen[BasisOfClaim] =
    Gen.oneOf(BasisOfClaim.values)

  lazy val genReimbursementClaims: Gen[Map[TaxCode, BigDecimal]] = for {
    num   <- Gen.choose(2, 5)
    codes <- Gen
               .listOfN(num, genTaxCode.flatMap(taxCode => Gen.posNum[Long].map(num => (taxCode, BigDecimal(num)))))
               .map(_.toMap)
  } yield codes

  val genScheduledOverpaymentClaims: Gen[Map[String, Map[TaxCode, AmountPaidWithCorrect]]] =
    Gen
      .nonEmptyListOf(
        Gen
          .nonEmptyListOf(Gen.oneOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i'))
          .map(String.valueOf)
          .flatMap { id =>
            Gen
              .nonEmptyListOf(
                genTaxCode.flatMap(taxCode =>
                  Gen.posNum[Long].map(num => (taxCode, AmountPaidWithCorrect(BigDecimal(num + 1), BigDecimal(num))))
                )
              )
              .map(list => id -> list.toMap)
          }
      )
      .map(_.toMap)

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

  lazy val genOverpaymentsSingleClaim: Gen[OverpaymentsSingleClaimData] =
    for {
      mrn                      <- genMRN
      claimantType             <- Gen.oneOf(ClaimantType.values)
      claimantInformation      <- genClaimantInformation
      basisOfClaim             <- genBasisOfClaim
      bankAccountDetails       <- Gen.option(genBankAccountDetails)
      reimbursementMethod      <- Gen.oneOf(ReimbursementMethodAnswer.values)
      declaration              <- genDisplayDeclaration.map { generatedDeclaration =>
                                    val drd = generatedDeclaration.displayResponseDetail.copy(declarationId = mrn.value)
                                    DisplayDeclaration(drd)
                                  }
      duplicateMrn             <- Gen.option(genMRN).map(_.filter(_ => basisOfClaim === DuplicateEntry))
      duplicateDeclaration      =
        duplicateMrn.map(dup => DisplayDeclaration(declaration.displayResponseDetail.copy(declarationId = dup.value)))
      claims                   <- genClaimsFromDisplayDeclaration(declaration)
      evidences                <- Gen.nonEmptyListOf(genEvidences)
      whetherInNorthernIreland <- Gen.oneOf(true, false)
      additionalDetails        <- genRandomString
      userEmail                <- Gen.some(genEmail)
      firstName                <- genStringWithMaxSizeOfN(15)
      lastName                 <- genStringWithMaxSizeOfN(15)
      ggCredId                 <- genStringWithMaxSizeOfN(15)
    } yield OverpaymentsSingleClaimData(
      SingleOverpaymentsClaim(
        movementReferenceNumber = mrn,
        duplicateMovementReferenceNumber = duplicateMrn,
        claimantType = claimantType,
        claimantInformation = claimantInformation,
        basisOfClaim = basisOfClaim,
        whetherNorthernIreland = whetherInNorthernIreland,
        additionalDetails = additionalDetails,
        reimbursementClaims = claims._2,
        reimbursementMethod = reimbursementMethod,
        bankAccountDetails = bankAccountDetails,
        supportingEvidences = evidences
      ),
      declaration,
      duplicateDeclaration,
      AuthenticatedUser(
        ggCredId,
        Some(s"$firstName $lastName"),
        userEmail
      )
    )

  lazy val genOverpaymentsMultipleClaim: Gen[(MultipleOverpaymentsClaim, List[DisplayDeclaration])] =
    for {
      numMrns                  <- Gen.choose(2, 10)
      mrns                     <- Gen.listOfN(numMrns, genMRN)
      claimantType             <- Gen.oneOf(ClaimantType.values)
      claimantInformation      <- genClaimantInformation
      basisOfClaim             <- genBasisOfClaim
      bankAccountDetails       <- Gen.option(genBankAccountDetails)
      reimbursementMethod      <- Gen.oneOf(ReimbursementMethodAnswer.values)
      numEvidences             <- Gen.choose(2, 5)
      evidences                <- Gen.listOfN(numEvidences, genEvidences)
      declarations             <- Gen
                                    .sequence(
                                      mrns.map(mrn =>
                                        genDisplayDeclaration.map { generatedDeclaration =>
                                          val drd = generatedDeclaration.displayResponseDetail.copy(declarationId = mrn.value)
                                          DisplayDeclaration(drd)
                                        }
                                      )
                                    )
                                    .map(_.asScala.toList)
                                    .suchThat(_.nonEmpty)
      claims                   <- Gen.sequence(declarations.map(declaration => genClaimsFromDisplayDeclaration(declaration)))
      whetherInNorthernIreland <- Gen.oneOf(true, false)
      additionalDetails        <- genRandomString
    } yield (
      MultipleOverpaymentsClaim(
        movementReferenceNumbers = mrns,
        claimantType = claimantType,
        claimantInformation = claimantInformation,
        basisOfClaim = basisOfClaim,
        whetherNorthernIreland = whetherInNorthernIreland,
        additionalDetails = additionalDetails,
        reimbursementClaims = claims.asScala.toMap,
        reimbursementMethod = reimbursementMethod,
        bankAccountDetails = bankAccountDetails,
        supportingEvidences = evidences
      ),
      declarations
    )

  lazy val genOverpaymentsScheduledClaim: Gen[(ScheduledOverpaymentsClaim, DisplayDeclaration)] =
    for {
      mrn                      <- genMRN
      claimantType             <- Gen.oneOf(ClaimantType.values)
      claimantInformation      <- genClaimantInformation
      basisOfClaim             <- genBasisOfClaim
      bankAccountDetails       <- Gen.option(genBankAccountDetails)
      reimbursementMethod      <- Gen.oneOf(ReimbursementMethodAnswer.values)
      declaration              <- genDisplayDeclaration.map { generatedDeclaration =>
                                    val drd = generatedDeclaration.displayResponseDetail.copy(declarationId = mrn.value)
                                    DisplayDeclaration(drd)
                                  }
      claims                   <- genScheduledOverpaymentClaims
      evidences                <- Gen.nonEmptyListOf(genEvidences)
      scheduledDocument        <- genEvidences
      whetherInNorthernIreland <- Gen.oneOf(true, false)
      additionalDetails        <- genRandomString
    } yield (
      ScheduledOverpaymentsClaim(
        movementReferenceNumber = mrn,
        claimantType = claimantType,
        claimantInformation = claimantInformation,
        basisOfClaim = basisOfClaim,
        whetherNorthernIreland = whetherInNorthernIreland,
        additionalDetails = additionalDetails,
        reimbursementClaims = claims,
        reimbursementMethod = reimbursementMethod,
        bankAccountDetails = bankAccountDetails,
        supportingEvidences = evidences,
        scheduledDocument = scheduledDocument
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

  implicit lazy val arbitrarySingleOverpaymentsRequest: Typeclass[SingleOverpaymentsClaimRequest]       =
    Arbitrary(genOverpaymentsSingleClaim.map { case OverpaymentsSingleClaimData(claim, _, _, _) => SingleOverpaymentsClaimRequest(claim) })

  implicit lazy val arbitraryMultipleOverpaymentsRequest: Typeclass[MultipleOverpaymentsClaimRequest]   =
    Arbitrary(genOverpaymentsMultipleClaim.map { case (claim, _) => MultipleOverpaymentsClaimRequest(claim) })

  implicit lazy val arbitraryScheduledOverpaymentsRequest: Typeclass[ScheduledOverpaymentsClaimRequest] =
    Arbitrary(genOverpaymentsScheduledClaim.map { case (claim, _) => ScheduledOverpaymentsClaimRequest(claim) })

  implicit lazy val arbitrarySingleOverpaymentsClaimDetails
    : Typeclass[OverpaymentsSingleClaimData]              =
    Arbitrary(genOverpaymentsSingleClaim)

  implicit lazy val arbitraryOverpaymentsSingleClaim: Typeclass[SingleOverpaymentsClaim] =
    Arbitrary(
      for {
        OverpaymentsSingleClaimData(claim, _, _, _) <- genOverpaymentsSingleClaim
      } yield claim
    )

  implicit lazy val arbitraryMultipleOverpaymentsClaimDetails
    : Typeclass[(MultipleOverpaymentsClaim, List[DisplayDeclaration])] =
    Arbitrary(genOverpaymentsMultipleClaim)

  implicit lazy val arbitraryOverpaymentsMultipleClaim: Typeclass[MultipleOverpaymentsClaim] =
    Arbitrary(
      for {
        (claim, _) <- genOverpaymentsMultipleClaim
      } yield claim
    )

  implicit lazy val arbitraryScheduledOverpaymentsClaimDetails
    : Typeclass[(ScheduledOverpaymentsClaim, DisplayDeclaration)] =
    Arbitrary(genOverpaymentsScheduledClaim)

  implicit lazy val arbitraryOverpaymentsScheduledClaim: Typeclass[ScheduledOverpaymentsClaim] =
    Arbitrary(
      for {
        (claim, _) <- genOverpaymentsScheduledClaim
      } yield claim
    )

}
