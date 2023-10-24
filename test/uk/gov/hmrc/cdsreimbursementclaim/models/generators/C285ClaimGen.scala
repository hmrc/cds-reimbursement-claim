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

import org.scalacheck.magnolia.{Typeclass}
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.AddressGen.{genCountry, genPostcode}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ClaimedReimbursementGen.genClaimedReimbursement
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ContactDetailsGen.{genEmail, genUkPhoneNumber}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen.genMRN
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import java.net.URL

object C285ClaimGen {

  lazy val genBasisOfClaim: Gen[BasisOfClaim] =
    Gen.oneOf(BasisOfClaim.values)

  lazy val genClaims: Gen[List[ClaimedReimbursement]] = for {
    numberOfDuties <- Gen.chooseNum(1, 4)
    claims         <- Gen.listOfN(numberOfDuties, genClaimedReimbursement)
  } yield claims

  implicit lazy val arbitraryClaims: Typeclass[List[ClaimedReimbursement]] = Arbitrary(genClaims)

  lazy val genAssociatedMRNs: Gen[List[MRN]] = for {
    numberOfMRNs <- Gen.chooseNum(1, 4)
    mrns         <- Gen.listOfN(numberOfMRNs, genMRN)
  } yield mrns

  implicit lazy val arbitraryAssociatedMRNs: Typeclass[List[MRN]] = Arbitrary(genAssociatedMRNs)

  lazy val genContactDetails: Gen[ContactDetails] =
    for {
      fullName     <- genRandomString
      emailAddress <- genEmail
      phoneNumber  <- Gen.option(genUkPhoneNumber)
    } yield ContactDetails(fullName, emailAddress, phoneNumber)

  lazy val genContactAddress: Gen[ContactAddress] =
    for {
      num      <- Gen.choose(1, 100)
      street   <- genStringWithMaxSizeOfN(7)
      line2    <- Gen.option(genStringWithMaxSizeOfN(10))
      line3    <- Gen.option(genStringWithMaxSizeOfN(20))
      line4    <- genStringWithMaxSizeOfN(15)
      postcode <- genPostcode
      country  <- genCountry
    } yield ContactAddress(
      line1 = s"$num $street",
      line2 = line2,
      line3 = line3,
      line4 = line4,
      postcode = postcode,
      country = country
    )

  lazy val genDetailsRegisteredWithCdsAnswer: Gen[DetailsRegisteredWithCdsAnswer] =
    for {
      fullName                 <- genRandomString
      emailAddress             <- genEmail
      contactAddress           <- genContactAddress
      whetherAddCompanyDetails <- genBoolean
    } yield DetailsRegisteredWithCdsAnswer(
      fullName,
      emailAddress,
      contactAddress,
      whetherAddCompanyDetails
    )

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

}
