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

package uk.gov.hmrc.cdsreimbursementclaim.services.ccs

import uk.gov.hmrc.cdsreimbursementclaim.models.ccs._
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{C285ClaimRequest, ClaimSubmitResponse}
import uk.gov.hmrc.cdsreimbursementclaim.utils.TimeUtils.cdsDateTimeFormat

import java.util.UUID

class C285ClaimToDec64FilesMapper extends ClaimToDec64FilesMapper[C285ClaimRequest] {

  def map(request: C285ClaimRequest, response: ClaimSubmitResponse): List[Envelope] =
    request.claim.documents.zipWithIndex.map { case (document, index) =>
      Envelope(
        Body(
          BatchFileInterfaceMetadata(
            correlationID = UUID.randomUUID().toString,
            batchID = request.claim.id.toString,
            batchCount = index.toLong + 1,
            batchSize = request.claim.documents.size.toLong,
            checksum = document.upscanSuccess.uploadDetails.checksum,
            sourceLocation = document.upscanSuccess.downloadUrl,
            sourceFileName = document.upscanSuccess.uploadDetails.fileName,
            sourceFileMimeType = document.upscanSuccess.uploadDetails.fileMimeType,
            fileSize = document.upscanSuccess.uploadDetails.size,
            properties = PropertiesType(
              List(
                PropertyType("CaseReference", response.caseNumber),
                PropertyType("Eori", request.signedInUserDetails.eori.value),
                PropertyType("DeclarationId", request.claim.movementReferenceNumber.value),
                PropertyType("DeclarationType", "MRN"),
                PropertyType("ApplicationName", "NDRC"),
                PropertyType(
                  "DocumentType",
                  document.documentType.map(_.toTPI05Key).getOrElse("")
                ),
                PropertyType("DocumentReceivedDate", cdsDateTimeFormat.format(document.uploadedOn))
              )
            )
          )
        )
      )
    }.toList
}