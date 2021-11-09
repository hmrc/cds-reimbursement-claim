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

package uk.gov.hmrc.cdsreimbursementclaim.models.ids

import play.api.libs.json.Format
import play.api.mvc.PathBindable
import uk.gov.hmrc.cdsreimbursementclaim.utils.SimpleStringFormat

final case class MRN(value: String)

object MRN {

  def parse(value: String): Option[MRN] =
    if (isValid(value)) Some(new MRN(value)) else None

  def isValid(in: String): Boolean = {
    val regex = """\d{2}[a-zA-Z]{2}\w{13}\d""".r
    in match {
      case regex(_*) => true
      case _         => false
    }
  }

  implicit val binder: PathBindable[MRN] = new PathBindable[MRN] {
    def bind(key: String, value: String): Either[String, MRN] =
      parse(value).toRight(s"Value: $value cannot be parsed as MRN")

    def unbind(key: String, value: MRN): String = value.value

  }

  implicit val format: Format[MRN] = SimpleStringFormat(MRN(_), _.value)
}
