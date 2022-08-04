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

package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.cdsreimbursementclaim.utils.EnumerationFormat

final case class GetDeclarationError(
  message: String,
  code: GetDeclarationErrorCode
)

object GetDeclarationError {
  implicit val format: OFormat[GetDeclarationError] = Json.format[GetDeclarationError]

  def invalidReasonForSecurity: GetDeclarationError = GetDeclarationError(
    "Invalid reason for security",
    GetDeclarationErrorCode.InvalidReasonForSecurityError
  )

  def declarationNotFound: GetDeclarationError = GetDeclarationError(
    "No declaration found for the given MRN",
    GetDeclarationErrorCode.DeclarationNotFoundError
  )

  def unexpetedError: GetDeclarationError = GetDeclarationError(
    "Unexpected rrror",
    GetDeclarationErrorCode.UnexpectedError
  )
}

sealed class GetDeclarationErrorCode(val code: String)

object GetDeclarationErrorCode extends EnumerationFormat[GetDeclarationErrorCode] {
  case object InvalidReasonForSecurityError extends GetDeclarationErrorCode("InvalidReasonForSecurity")
  case object DeclarationNotFoundError extends GetDeclarationErrorCode("InvalidReasonForSecurity")
  case object UnexpectedError extends GetDeclarationErrorCode("UnexpectedError")

  override val values: Set[GetDeclarationErrorCode] = Set(
    InvalidReasonForSecurityError,
    DeclarationNotFoundError,
    UnexpectedError
  )
}
