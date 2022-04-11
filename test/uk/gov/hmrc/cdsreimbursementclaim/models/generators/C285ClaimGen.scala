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

import cats.data.NonEmptyList
import org.scalacheck.magnolia.{Typeclass, gen}
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.cdsreimbursementclaim.models.ContactName
import uk.gov.hmrc.cdsreimbursementclaim.models.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Acc14DeclarationGen.genDisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.AddressGen.{genCountry, genPostcode}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.BankAccountDetailsGen.genBankAccountDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ClaimedReimbursementGen.genClaimedReimbursement
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ContactDetailsGen.{genEmail, genUkPhoneNumber}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen.{genEori, genMRN}
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

  lazy val genC285Claim: Gen[C285Claim] = for {
    id                          <- genUUID
    typeOfClaim                 <- Gen.oneOf(TypeOfClaimAnswer.values)
    mrn                         <- genMRN
    duplicateMrn                <- Gen.option(genMRN)
    declarantType               <- Gen.oneOf(DeclarantTypeAnswer.values)
    detailsRegisteredWithCds    <- genDetailsRegisteredWithCdsAnswer
    contactDetails              <- genContactDetails
    contactAddress              <- genContactAddress
    eori                        <- genEori
    displayDeclaration          <- genDisplayDeclaration
    duplicateDisplayDeclaration <- Gen.option(genDisplayDeclaration)
    basisOfClaim                <- genBasisOfClaim
    reimbursementMethod         <- Gen.oneOf(ReimbursementMethodAnswer.values)
    bankAccountDetails          <- Gen.option(genBankAccountDetails)
    documents                   <- Gen.nonEmptyListOf(genEvidences).map(NonEmptyList.fromListUnsafe)
    commodityDetails            <- genRandomString
    claimedReimbursement        <- genClaimedReimbursement
    associated                  <- Gen
                                     .option(genMRN)
                                     .flatMap(mrn =>
                                       if (mrn.isDefined)
                                         genClaimedReimbursement.map(reimbursement => (mrn, Some(reimbursement)))
                                       else Gen.const((mrn, None))
                                     )
  } yield C285Claim(
    id = id,
    typeOfClaim = typeOfClaim,
    movementReferenceNumber = mrn,
    duplicateMovementReferenceNumberAnswer = duplicateMrn,
    declarantTypeAnswer = declarantType,
    detailsRegisteredWithCdsAnswer = detailsRegisteredWithCds,
    mrnContactDetailsAnswer = Some(contactDetails),
    mrnContactAddressAnswer = Some(contactAddress),
    basisOfClaimAnswer = basisOfClaim,
    bankAccountDetailsAnswer = bankAccountDetails,
    documents = documents,
    commodityDetailsAnswer = CommodityDetailsAnswer(commodityDetails),
    displayDeclaration = Some(displayDeclaration),
    duplicateDisplayDeclaration = duplicateDisplayDeclaration,
    importerEoriNumber = Some(ImporterEoriNumberAnswer(eori)),
    declarantEoriNumber = Some(DeclarantEoriNumberAnswer(eori)),
    claimedReimbursementsAnswer = NonEmptyList.of(claimedReimbursement),
    reimbursementMethodAnswer = reimbursementMethod,
    associatedMRNsAnswer = associated._1.map(mrn => NonEmptyList.of(mrn)),
    associatedMRNsClaimsAnswer = associated._2.map(reimbursement => NonEmptyList.one(NonEmptyList.one(reimbursement)))
  )

  implicit lazy val arbitraryC285Claim: Typeclass[C285Claim] =
    Arbitrary(genC285Claim)

  implicit lazy val arbitrarySignedInUserDetails: Typeclass[SignedInUserDetails] = Arbitrary(for {
    email <- genEmail
    eori  <- genEori
    name  <- genRandomString
  } yield SignedInUserDetails(Some(email), eori, email, ContactName(name)))

  implicit lazy val arbitraryC285ClaimRequest: Typeclass[C285ClaimRequest] =
    gen[C285ClaimRequest]
}
