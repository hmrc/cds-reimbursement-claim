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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response

import play.api.libs.json.{Json, OFormat}

import scala.collection.immutable

final case class DeclarationErrorResponse(errorDetail: ErrorDetail)

object DeclarationErrorResponse {
  implicit val format: OFormat[DeclarationErrorResponse] = Json.format[DeclarationErrorResponse]

}

final case class ErrorDetail(
  timestamp: Option[String],
  correlationId: Option[String],
  errorCode: Option[String],
  errorMessage: Option[String],
  source: Option[String],
  sourceFaultDetail: SourceFaultDetail
)

object ErrorDetail {
  implicit val format: OFormat[ErrorDetail] = Json.format[ErrorDetail]
}

final case class SourceFaultDetail(detail: immutable.Seq[String])

object SourceFaultDetail {
  implicit val format: OFormat[SourceFaultDetail] = Json.format[SourceFaultDetail]
}
