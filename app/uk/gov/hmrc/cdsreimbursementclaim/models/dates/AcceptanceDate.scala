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

package uk.gov.hmrc.cdsreimbursementclaim.models.dates

import uk.gov.hmrc.cdsreimbursementclaim.models.dates.AcceptanceDate.{dateStringFormat, displayFormat}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try

final case class AcceptanceDate(value: LocalDate) extends AnyVal {
  def toDisplayString: Try[String]   = Try(displayFormat.format(value))
  def toTpi05DateString: Try[String] = Try(dateStringFormat.format(value))
}

object AcceptanceDate {

  private val displayFormat: DateTimeFormatter    = DateTimeFormatter.ofPattern("d MMMM u")
  private val inputDateFormat: DateTimeFormatter  = DateTimeFormatter.ofPattern("u-M-d")
  private val dateStringFormat: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE

  def fromDisplayFormat(value: String): Try[AcceptanceDate] =
    parse(value, displayFormat)

  def apply(value: String): Try[AcceptanceDate] =
    parse(value, inputDateFormat)

  private def parse(value: String, format: DateTimeFormatter) =
    Try(LocalDate.parse(value, format))
      .map(AcceptanceDate(_))
}
