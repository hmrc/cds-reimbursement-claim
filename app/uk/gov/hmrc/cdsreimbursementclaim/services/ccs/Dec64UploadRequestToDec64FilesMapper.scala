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

package uk.gov.hmrc.cdsreimbursementclaim.services.ccs

import uk.gov.hmrc.cdsreimbursementclaim.models.ccs._
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimSubmitResponse, Dec64UploadRequest}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.TemporalAccessorOps

import java.util.UUID
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity

class Dec64UploadRequestToDec64FilesMapper extends ClaimToDec64Mapper[Dec64UploadRequest] {

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def map(request: Dec64UploadRequest, response: ClaimSubmitResponse): List[String] =
    request.uploadedFiles.zipWithIndex.map { case (document, index) =>
      DEC64RequestBuilder(
        correlationID = UUID.randomUUID().toString,
        batchID = UUID.randomUUID().toString,
        batchCount = index + 1,
        batchSize = request.uploadedFiles.size,
        checksum = document.checksum,
        sourceLocation = document.downloadUrl,
        sourceFileName = document.fileName,
        sourceFileMimeType = document.fileMimeType,
        fileSize = document.fileSize,
        properties = List(
          ("CaseReference", response.caseNumber),
          ("Eori", request.eori),
          ("DeclarationId", request.declarationId),
          ("DeclarationType", "MRN"),
          ("ApplicationName", request.applicationName),
          ("DocumentType", document.description),
          ("DocumentReceivedDate", document.uploadTimestamp.toCdsDateTime)
        ) ++ (if (request.applicationName == "Securities")
                (
                  "RFS",
                  request.reasonForSecurity
                    .flatMap(ReasonForSecurity.parseACC14Code)
                    .getOrElse(throw new Exception("Missing RFS property"))
                    .dec64DisplayString
                ) :: Nil
              else Nil)
      )
    }
}
