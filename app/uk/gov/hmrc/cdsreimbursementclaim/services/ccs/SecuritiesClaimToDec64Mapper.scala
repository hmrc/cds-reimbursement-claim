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
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimSubmitResponse, SecuritiesClaimRequest}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.TemporalAccessorOps

import java.util.UUID

class SecuritiesClaimToDec64Mapper extends ClaimToDec64Mapper[SecuritiesClaimRequest] {

  def map(request: SecuritiesClaimRequest, response: ClaimSubmitResponse): List[String] =
    request.claim.supportingEvidences.zipWithIndex.map { case (document, index) =>
      DEC64RequestBuilder(
        correlationID = UUID.randomUUID().toString,
        batchID = UUID.randomUUID().toString,
        batchCount = index + 1,
        batchSize = request.claim.supportingEvidences.size,
        checksum = document.checksum,
        sourceLocation = document.downloadUrl,
        sourceFileName = document.fileName,
        sourceFileMimeType = document.fileMimeType,
        fileSize = document.size,
        properties = List(
          ("CaseReference", response.caseNumber),
          ("Eori", request.claim.claimantInformation.eori.value),
          ("DeclarationId", request.claim.movementReferenceNumber.value),
          ("DeclarationType", "MRN"),
          ("RFS", request.claim.reasonForSecurity.dec64DisplayString),
          ("ApplicationName", "Securities"),
          ("DocumentType", document.documentType.toDec64DisplayString),
          ("DocumentReceivedDate", document.uploadedOn.toCdsDateTime)
        )
      )
    }.toList
}
