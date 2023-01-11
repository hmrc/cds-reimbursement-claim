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

import org.scalacheck.magnolia.Typeclass
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen.{genEori, genMRN}

import java.net.URL

@SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
object Dec64UploadRequestGen {

  lazy val genDec64UploadRequest: Gen[Dec64UploadRequest] = for {
    uuid          <- genUUID
    mrn           <- genMRN
    eori          <- genEori
    caseNumber    <- genRandomString
    numberOfFiles <- Gen.chooseNum(1, 4)
    files         <- Gen.listOfN(numberOfFiles, genEvidences)
  } yield Dec64UploadRequest(
    id = uuid.toString,
    eori = eori.value,
    caseNumber = caseNumber,
    declarationId = mrn.value,
    entryNumber = false,
    applicationName = "NDRC",
    uploadedFiles = files.toList
  )

  implicit lazy val arbitraryDec64UploadRequest: Typeclass[Dec64UploadRequest] =
    Arbitrary(genDec64UploadRequest)

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

  lazy val genEvidences: Gen[Dec64UploadedFile] =
    for {
      uuid         <- genUUID
      url          <- genUrl
      fileName     <- genFileName
      size         <- Gen.chooseNum(10, 1000)
      uploadTime   <- genLocalDateTime
      documentType <- Gen.oneOf(UploadDocumentType.values)
    } yield Dec64UploadedFile(
      upscanReference = uuid.toString.replace("-", ""),
      checksum = uuid.toString.replace("-", ""),
      downloadUrl = url.toString,
      fileName = fileName,
      fileMimeType = fileName
        .split(".")
        .lastOption
        .map(s => if (s == "pdf") s"application/$s" else s"text/$s")
        .getOrElse("application/octet-stream"),
      fileSize = size,
      uploadTimestamp = uploadTime,
      description = documentType.toDec64DisplayString
    )
}
