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

import play.api.libs.json._
import uk.gov.hmrc.cdsreimbursementclaim.models.FrontendSubmitClaim.FileInformation
//import uk.gov.hmrc.cdsreimbursementclaim.models.SubmitClaimRequest.TPI05RequestDetail
import uk.gov.hmrc.cdsreimbursementclaim.models.upscan.UploadReference

//This protocol is used when the frontend wants to send down to this service for the finel submission.
final case class FrontendSubmitClaim(
  //submitClaimRequest: TPI05RequestDetail,
  eori: String,
  declarationId: String,
  declarationIdType: DeclarationIdType,
  files: List[FileInformation]
)

sealed trait DeclarationIdType
case object MRNType extends DeclarationIdType { override def toString: String = "MRN" }
case object CHIEFType extends DeclarationIdType { override def toString: String = "CHIEF" }

object DeclarationIdType {
  implicit val declarationIdTypeFormat: Format[DeclarationIdType] = new Format[DeclarationIdType] {
    def reads(json: JsValue): JsResult[DeclarationIdType] =
      json.as[String] match {
        case "MRN"   => JsSuccess(MRNType)
        case "CHIEF" => JsSuccess(CHIEFType)
      }
    def writes(dec: DeclarationIdType): JsValue           = JsString(dec.toString)

  }

}

object FrontendSubmitClaim {

  implicit val fileInformationFormat: OFormat[FileInformation]         = Json.format
  implicit val frontendSubmitClaimFormat: OFormat[FrontendSubmitClaim] = Json.format

  final case class FileInformation(uploadReference: UploadReference, documentType: String)

}
