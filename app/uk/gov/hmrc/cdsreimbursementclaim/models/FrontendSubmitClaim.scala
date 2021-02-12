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

package uk.gov.hmrc.cdsreimbursementclaim.models

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.cdsreimbursementclaim.models.FrontendSubmitClaim.FileInformation
import uk.gov.hmrc.cdsreimbursementclaim.models.upscan.UploadReference

//This protocol is used when the frontend wants to send down to this service for the finel submission.
final case class FrontendSubmitClaim(
  eori: String,
  declarationId: String,
  files: List[FileInformation]
)

object FrontendSubmitClaim {

  implicit val fileInformationFormat: OFormat[FileInformation]         = Json.format
  implicit val frontendSubmitClaimFormat: OFormat[FrontendSubmitClaim] = Json.format

  final case class FileInformation(uploadReference: UploadReference, documentType: String)

}
