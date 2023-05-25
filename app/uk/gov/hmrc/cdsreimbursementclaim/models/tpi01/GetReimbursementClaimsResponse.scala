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

package uk.gov.hmrc.cdsreimbursementclaim.models.tpi01

import cats.syntax.eq._
import play.api.libs.json._
import collection.immutable.Seq

final case class GetReimbursementClaimsResponse(
  responseCommon: ResponseCommon,
  responseDetail: Option[ResponseDetail]
) {
  val mdtpError: Boolean = responseCommon.returnParameters.exists(_.exists(_.paramName === "POSITION"))
}

@SuppressWarnings(Array("org.wartremover.warts.Any"))
object GetReimbursementClaimsResponse {
  implicit val format: OFormat[GetReimbursementClaimsResponse] = Json.format[GetReimbursementClaimsResponse]
}

final case class ReturnParameter(paramName: String, paramValue: String)

@SuppressWarnings(Array("org.wartremover.warts.Any"))
object ReturnParameter {
  implicit val format: OFormat[ReturnParameter] = Json.format[ReturnParameter]
}

final case class ResponseCommon(
  status: String,
  processingDate: String,
  correlationId: Option[String],
  errorMessage: Option[String],
  returnParameters: Option[List[ReturnParameter]]
)

@SuppressWarnings(Array("org.wartremover.warts.Any"))
object ResponseCommon {
  implicit val format: OFormat[ResponseCommon] = Json.format[ResponseCommon]
}

final case class ResponseDetail(NDRCCasesFound: Boolean, SCTYCasesFound: Boolean, CDFPayCase: Option[CDFPayCase]) {}

@SuppressWarnings(Array("org.wartremover.warts.Any"))
object ResponseDetail {
  implicit val format: OFormat[ResponseDetail] = Json.format[ResponseDetail]
}

final case class CDFPayCase(
  NDRCCaseTotal: Option[String],
  NDRCCases: Option[Seq[NDRCCaseDetails]],
  SCTYCaseTotal: Option[String],
  SCTYCases: Option[Seq[SCTYCaseDetails]]
)

@SuppressWarnings(Array("org.wartremover.warts.Any"))
object CDFPayCase {
  implicit val format: OFormat[CDFPayCase] = Json.format[CDFPayCase]
}
