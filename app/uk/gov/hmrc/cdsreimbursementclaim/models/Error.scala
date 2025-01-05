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

import uk.gov.hmrc.http.HttpResponse
import play.api.mvc.Results
import play.api.mvc.Result

sealed trait Error {
  type T
  val value: T

  def asResult(): Result
}

object Error {

  def apply(message: String): Error = new Error {
    override type T = String
    override val value: T           = message
    override def toString(): String = message
    override def asResult(): Result =
      Results.InternalServerError(message)
  }

  def apply(throwable: Throwable): Error = new Error {
    override type T = Throwable
    override val value: T           = throwable
    override def toString(): String = throwable.getMessage()
    override def asResult(): Result =
      Results.InternalServerError(throwable.toString())
  }

  def apply(response: HttpResponse): Error = new Error {
    override type T = HttpResponse
    override val value: T           = response
    override def toString(): String = response.body
    override def asResult(): Result =
      Results
        .Status(response.status)(response.body)
        .withHeaders(response.headers.toSeq.flatMap { case (key, values) =>
          values.map(value => (key, value))
        }*)
  }
}
