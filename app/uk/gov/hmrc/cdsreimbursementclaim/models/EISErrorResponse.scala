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

package uk.gov.hmrc.cdsreimbursementclaim.models

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.http.HttpResponse
import play.api.libs.json.Reads
import play.api.libs.json.JsObject
import play.api.libs.json.JsNumber
import scala.util.Try
import com.fasterxml.jackson.core.JsonParseException

import scala.collection.immutable

final case class EisErrorResponse(
  status: Int,
  errorDetail: Option[ErrorDetail],
  unparsedResponseBody: Option[String] = None
) {
  def getErrorDescriptionWithPrefix(prefix: String): String =
    unparsedResponseBody match {
      case Some(body) if body.isEmpty => s"$prefix failed with status $status and empty body."
      case Some(body)                 => s"$prefix failed with status $status and body: $body"
      case None                       =>
        errorDetail match {
          case Some(error) =>
            s"$prefix failed with status $status and errorCode=${error.errorCode}, and errorMessage=${error.errorMessage}, and correlationId=${error.correlationId}"
          case None        =>
            s"$prefix failed with status $status and no further details."
        }
    }
}

final case class ErrorDetail(
  timestamp: String,
  correlationId: String,
  errorCode: String,
  errorMessage: String,
  source: String,
  sourceFaultDetail: SourceFaultDetail
)

object ErrorDetail {
  implicit val format: OFormat[ErrorDetail] = Json.format[ErrorDetail]
}

final case class SourceFaultDetail(detail: immutable.Seq[String])

object SourceFaultDetail {
  implicit val format: OFormat[SourceFaultDetail] = Json.format[SourceFaultDetail]
}

object EisErrorResponse {
  implicit val format: OFormat[EisErrorResponse] = Json.format[EisErrorResponse]

  final def readsErrorDetailOr[R : Reads]: HttpReads[Either[EisErrorResponse, R]] =
    new HttpReads[Either[EisErrorResponse, R]] {

      override def read(
        method: String,
        url: String,
        response: HttpResponse
      ): Either[EisErrorResponse, R] =
        if (response.body.isEmpty)
          Left(EisErrorResponse(response.status, None, Some(response.body)))
        else
          Try(
            response.json
              .asOpt[R]
              .toRight(
                response.json
                  .asOpt[JsObject]
                  .flatMap(_.+("status" -> JsNumber(response.status)).asOpt[EisErrorResponse])
                  .getOrElse(EisErrorResponse(response.status, None, Some(response.body)))
              )
          ).toEither.fold(
            {
              case e: JsonParseException =>
                Left(EisErrorResponse(response.status, None, Some(response.body)))

              case e =>
                Left(
                  EisErrorResponse(
                    response.status,
                    None,
                    Some(s"Unexpected error while parsing response from EIS: ${e.getMessage}")
                  )
                )
            },
            identity
          )

    }
}
