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

package uk.gov.hmrc.cdsreimbursementclaim.services.ccs

import uk.gov.hmrc.cdsreimbursementclaim.models.ccs._
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimSubmitResponse, SingleOverpaymentsClaimRequest}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.TemporalAccessorOps

import java.util.UUID

class SingleOverpaymentsClaimToDec64FilesMapper extends ClaimToDec64Mapper[SingleOverpaymentsClaimRequest] {

  def map(request: SingleOverpaymentsClaimRequest, response: ClaimSubmitResponse): List[Envelope] =
    request.claim.supportingEvidences.zipWithIndex.map { case (document, index) =>
      Envelope(
        Body(
          BatchFileInterfaceMetadata(
            correlationID = UUID.randomUUID().toString,
            batchID = UUID.randomUUID().toString,
            batchCount = index.toLong + 1,
            batchSize = request.claim.supportingEvidences.size.toLong,
            checksum = document.checksum,
            sourceLocation = document.downloadUrl,
            sourceFileName = document.fileName,
            sourceFileMimeType = document.fileMimeType,
            fileSize = document.size,
            properties = PropertiesType(
              List(
                PropertyType("CaseReference", response.caseNumber),
                PropertyType("Eori", request.claim.claimantInformation.eori.value),
                PropertyType("DeclarationId", request.claim.movementReferenceNumber.value),
                PropertyType("DeclarationType", "MRN"),
                PropertyType("ApplicationName", "NDRC"),
                PropertyType("DocumentType", document.documentType.toDec64DisplayString),
                PropertyType("DocumentReceivedDate", document.uploadedOn.toCdsDateTime)
              )
            )
          )
        )
      )
    }.toList
}
