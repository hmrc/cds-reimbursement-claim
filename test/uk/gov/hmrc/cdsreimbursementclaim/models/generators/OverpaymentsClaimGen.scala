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

import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.BasisOfClaim.DuplicateEntry
import uk.gov.hmrc.cdsreimbursementclaim.models.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.NdrcDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Acc14DeclarationGen.genDisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.BankAccountDetailsGen.genBankAccountDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen.{genEori, genMRN}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TPI05RequestGen.genContactInformation
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TaxCodesGen.genTaxCode
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN

import java.net.URL
import scala.jdk.CollectionConverters.CollectionHasAsScala
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori

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

  val genOverpaymentsSingleClaimAllTypes
    : Gen[(SingleOverpaymentsClaim, DisplayDeclaration, Option[DisplayDeclaration])] =
    for {
      claimantType <- Gen.oneOf(ClaimantType.values)
      claim        <- genOverpaymentsSingleClaim(claimantType)
    } yield claim

  def genOverpaymentsSingleClaim(
    claimantType: ClaimantType
  ): Gen[(SingleOverpaymentsClaim, DisplayDeclaration, Option[DisplayDeclaration])] =
    for {
      mrn                 <- genMRN
      claimantInformation <- genClaimantInformation
      basisOfClaim        <- genBasisOfClaim
      bankAccountDetails  <- Gen.option(genBankAccountDetails)
      reimbursementMethod <- Gen.oneOf(ReimbursementMethodAnswer.values)
      declaration         <- genDisplayDeclaration.map { generatedDeclaration =>
                               val drd = generatedDeclaration.displayResponseDetail.copy(declarationId = mrn.value)
                               DisplayDeclaration(drd)
                             }
      duplicateMrn        <- Gen.option(genMRN).map(_.filter(_ => basisOfClaim === DuplicateEntry))
      duplicateDeclaration =
        duplicateMrn.map(dup => DisplayDeclaration(declaration.displayResponseDetail.copy(declarationId = dup.value)))
      claims              <- genClaimsFromDisplayDeclaration(declaration)
      evidences           <- Gen.nonEmptyListOf(genEvidences)
      additionalDetails   <- genRandomString
      payeeType           <- Gen.oneOf[PayeeType](PayeeType.Declarant, PayeeType.Consignee)
      newEoriAndDan       <- Gen.oneOf(None, Some(NewEoriAndDan(Eori("foo-eori"), "foo-dan")))
    } yield (
      SingleOverpaymentsClaim(
        movementReferenceNumber = mrn,
        duplicateMovementReferenceNumber = duplicateMrn,
        claimantType = claimantType,
        claimantInformation = claimantInformation,
        basisOfClaim = basisOfClaim,
        additionalDetails = additionalDetails,
        reimbursements = claims._2.toSeq.map { case (taxCode, amount) =>
          Reimbursement(taxCode, amount, reimbursementMethod)
        },
        reimbursementMethod = reimbursementMethod,
        bankAccountDetails = bankAccountDetails,
        supportingEvidences = evidences,
        payeeType = payeeType,
        newEoriAndDan = newEoriAndDan
      ),
      declaration,
      duplicateDeclaration
    )

  lazy val genOverpaymentsMultipleClaim: Gen[(MultipleOverpaymentsClaim, List[DisplayDeclaration])] =
    for {
      numMrns             <- Gen.choose(2, 10)
      mrns                <- Gen.listOfN(numMrns, genMRN)
      claimantType        <- Gen.oneOf(ClaimantType.values)
      payeeType           <- Gen.oneOf[PayeeType](PayeeType.Declarant, PayeeType.Consignee)
      claimantInformation <- genClaimantInformation
      basisOfClaim        <- genBasisOfClaim
      bankAccountDetails  <- Gen.option(genBankAccountDetails)
      reimbursementMethod <- Gen.oneOf(ReimbursementMethodAnswer.values)
      numEvidences        <- Gen.choose(2, 5)
      evidences           <- Gen.listOfN(numEvidences, genEvidences)
      declarations        <- Gen
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
      claims              <- Gen.sequence(declarations.map(declaration => genClaimsFromDisplayDeclaration(declaration)))
      additionalDetails   <- genRandomString
      newEoriAndDan       <- Gen.oneOf(None, Some(NewEoriAndDan(Eori("foo-eori"), "foo-dan")))
    } yield (
      MultipleOverpaymentsClaim(
        movementReferenceNumbers = mrns,
        claimantType = claimantType,
        payeeType = payeeType,
        claimantInformation = claimantInformation,
        basisOfClaim = basisOfClaim,
        additionalDetails = additionalDetails,
        reimbursementClaims = claims.asScala.toMap,
        reimbursementMethod = reimbursementMethod,
        bankAccountDetails = bankAccountDetails,
        supportingEvidences = evidences,
        newEoriAndDan = newEoriAndDan
      ),
      declarations
    )

  val genOverpaymentsScheduledClaimAllTypes: Gen[(ScheduledOverpaymentsClaim, DisplayDeclaration)] =
    for {
      claimantType <- Gen.oneOf(ClaimantType.values)
      claim        <- genOverpaymentsScheduledClaim(claimantType)
    } yield claim

  def genOverpaymentsScheduledClaim(claimantType: ClaimantType): Gen[(ScheduledOverpaymentsClaim, DisplayDeclaration)] =
    for {
      mrn                 <- genMRN
      payeeType           <- Gen.oneOf[PayeeType](PayeeType.values)
      claimantInformation <- genClaimantInformation
      basisOfClaim        <- genBasisOfClaim
      bankAccountDetails  <- Gen.option(genBankAccountDetails)
      reimbursementMethod <- Gen.oneOf(ReimbursementMethodAnswer.values)
      declaration         <- genDisplayDeclaration.map { generatedDeclaration =>
                               val drd = generatedDeclaration.displayResponseDetail.copy(declarationId = mrn.value)
                               DisplayDeclaration(drd)
                             }
      claims              <- genScheduledOverpaymentClaims
      evidences           <- Gen.nonEmptyListOf(genEvidences)
      scheduledDocument   <- genEvidences
      additionalDetails   <- genRandomString
      newEoriAndDan       <- Gen.oneOf(None, Some(NewEoriAndDan(Eori("foo-eori"), "foo-dan")))
    } yield (
      ScheduledOverpaymentsClaim(
        movementReferenceNumber = mrn,
        claimantType = claimantType,
        payeeType = payeeType,
        claimantInformation = claimantInformation,
        basisOfClaim = basisOfClaim,
        additionalDetails = additionalDetails,
        reimbursementClaims = claims,
        reimbursementMethod = reimbursementMethod,
        bankAccountDetails = bankAccountDetails,
        supportingEvidences = evidences,
        scheduledDocument = scheduledDocument,
        newEoriAndDan = newEoriAndDan
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

  implicit lazy val arbitrarySingleOverpaymentsRequest: Arbitrary[SingleOverpaymentsClaimRequest] =
    Arbitrary(genOverpaymentsSingleClaimAllTypes.map { case (claim, _, _) => SingleOverpaymentsClaimRequest(claim) })

  implicit lazy val arbitraryMultipleOverpaymentsRequest: Arbitrary[MultipleOverpaymentsClaimRequest] =
    Arbitrary(genOverpaymentsMultipleClaim.map { case (claim, _) => MultipleOverpaymentsClaimRequest(claim) })

  implicit lazy val arbitraryScheduledOverpaymentsRequest: Arbitrary[ScheduledOverpaymentsClaimRequest] =
    Arbitrary(genOverpaymentsScheduledClaimAllTypes.map { case (claim, _) => ScheduledOverpaymentsClaimRequest(claim) })

  implicit lazy val arbitrarySingleOverpaymentsClaimDetails
    : Arbitrary[(SingleOverpaymentsClaim, DisplayDeclaration, Option[DisplayDeclaration])] =
    Arbitrary(genOverpaymentsSingleClaimAllTypes)

  implicit lazy val arbitraryOverpaymentsSingleClaim: Arbitrary[SingleOverpaymentsClaim] =
    Arbitrary(
      for {
        (claim, _, _) <- genOverpaymentsSingleClaimAllTypes
      } yield claim
    )

  implicit lazy val arbitraryMultipleOverpaymentsClaimDetails
    : Arbitrary[(MultipleOverpaymentsClaim, List[DisplayDeclaration])] =
    Arbitrary(genOverpaymentsMultipleClaim)

  implicit lazy val arbitraryOverpaymentsMultipleClaim: Arbitrary[MultipleOverpaymentsClaim] =
    Arbitrary(
      for {
        (claim, _) <- genOverpaymentsMultipleClaim
      } yield claim
    )

  implicit lazy val arbitraryScheduledOverpaymentsClaimDetails
    : Arbitrary[(ScheduledOverpaymentsClaim, DisplayDeclaration)] =
    Arbitrary(genOverpaymentsScheduledClaimAllTypes)

  implicit lazy val arbitraryOverpaymentsScheduledClaim: Arbitrary[ScheduledOverpaymentsClaim] =
    Arbitrary(
      for {
        (claim, _) <- genOverpaymentsScheduledClaimAllTypes
      } yield claim
    )

}
