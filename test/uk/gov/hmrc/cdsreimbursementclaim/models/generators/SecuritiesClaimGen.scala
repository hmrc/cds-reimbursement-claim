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

import org.scalacheck.magnolia.Typeclass
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.cdsreimbursementclaim.models.ContactName
import uk.gov.hmrc.cdsreimbursementclaim.models.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.BankAccountDetailsGen.genBankAccountDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ClaimedReimbursementGen.genClaimedReimbursement
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ContactDetailsGen.genEmail
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen.{genEori, genMRN}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.RejectedGoodsClaimGen.genClaimantInformation
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TaxCodesGen.genTaxCode
import java.net.URL
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.TemporaryAdmissionMethodOfDisposal

@SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
object SecuritiesClaimGen {

  lazy val genClaims: Gen[List[ClaimedReimbursement]] = for {
    numberOfDuties <- Gen.chooseNum(1, 4)
    claims         <- Gen.listOfN(numberOfDuties, genClaimedReimbursement)
  } yield claims

  implicit lazy val arbitraryClaims: Typeclass[List[ClaimedReimbursement]] = Arbitrary(genClaims)

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

  lazy val genReimbursementClaims: Gen[Map[TaxCode, BigDecimal]] = for {
    num   <- Gen.choose(2, 5)
    codes <- Gen
               .listOfN(num, genTaxCode.flatMap(taxCode => Gen.posNum[Long].map(num => (taxCode, BigDecimal(num)))))
               .map(_.toMap)
  } yield codes

  lazy val genSecuritiesReclaims: Gen[Map[String, Map[TaxCode, BigDecimal]]] = for {
    num   <- Gen.choose(2, 5)
    codes <- Gen
               .listOfN(
                 num,
                 genRandomString.flatMap((str: String) => (str, genReimbursementClaims.sample.getOrElse(Map.empty)))
               )
               .map(_.toMap)
  } yield codes

  lazy val genSecuritiesClaim: Gen[SecuritiesClaimRequest] = for {
    mrn                 <- genMRN
    claimType           <- Gen.oneOf(ClaimantType.values)
    claimantInformation <- genClaimantInformation
    reasonForSecurity   <- Gen.oneOf(ReasonForSecurity.values)
    securitiesReclaims  <- genSecuritiesReclaims
    bankAccountDetails  <- Gen.option(genBankAccountDetails)
    documents           <- Gen.listOf(genEvidences)

    temporaryAdmissionMethodOfDisposal <-
      if (ReasonForSecurity.temporaryAdmissions.contains(reasonForSecurity))
        Gen.oneOf(TemporaryAdmissionMethodOfDisposal.values).map(Some.apply)
      else
        Gen.const(None)

    exportMovementReferenceNumber <-
      if (
        ReasonForSecurity.temporaryAdmissions(reasonForSecurity) &&
        temporaryAdmissionMethodOfDisposal.contains(
          TemporaryAdmissionMethodOfDisposal.ExportedInSingleShipment
        )
      ) genMRN.map(Some.apply)
      else
        Gen.const(None)

  } yield SecuritiesClaimRequest(
    SecuritiesClaim(
      movementReferenceNumber = mrn,
      claimantType = claimType,
      claimantInformation = claimantInformation,
      reasonForSecurity = reasonForSecurity,
      securitiesReclaims = securitiesReclaims,
      bankAccountDetails = bankAccountDetails,
      supportingEvidences = documents,
      exportMovementReferenceNumber = exportMovementReferenceNumber,
      temporaryAdmissionMethodOfDisposal = temporaryAdmissionMethodOfDisposal
    )
  )

  implicit lazy val arbitrarySecuritiesClaim: Typeclass[SecuritiesClaimRequest] =
    Arbitrary(genSecuritiesClaim)

  implicit lazy val arbitrarySignedInUserDetails: Typeclass[SignedInUserDetails] = Arbitrary(for {
    email <- genEmail
    eori  <- genEori
    name  <- genRandomString
  } yield SignedInUserDetails(Some(email), eori, email, ContactName(name)))
}
