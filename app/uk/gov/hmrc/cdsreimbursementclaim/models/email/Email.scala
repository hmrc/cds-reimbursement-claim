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

package uk.gov.hmrc.cdsreimbursementclaim.models.email

import cats.data.NonEmptyList
import cats.data.Validated.{Invalid, Valid}
import play.api.libs.functional.syntax._
import play.api.libs.json.Format
import uk.gov.hmrc.cdsreimbursementclaim.models.Validation

final case class Email(value: String) extends AnyVal

object Email {

  implicit val format: Format[Email] =
    implicitly[Format[String]].inmap(Email(_), _.value)

  def emailValidation(
    email: Option[String]
  ): Validation[Email] =
    email match {
      case Some(emailAddress) => Valid(Email(emailAddress))
      case None               => Invalid(NonEmptyList.one("Email address is missing"))
    }

}
