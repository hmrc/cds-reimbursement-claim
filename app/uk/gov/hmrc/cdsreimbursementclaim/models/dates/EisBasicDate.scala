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

import play.api.libs.json.{JsError, JsResult, JsString, JsSuccess, JsValue, Reads, Writes}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.EisBasicDate.eisStringFormat

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try

final case class EisBasicDate(value: LocalDate) extends AnyVal {
  def toTpi05DateString: String = eisStringFormat.format(value)
}

object EisBasicDate {
  private val eisStringFormat: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE

  def parse(value: String): Try[EisBasicDate] =
    parse(value, eisStringFormat)

  private def parse(value: String, format: DateTimeFormatter) =
    Try(LocalDate.parse(value, format))
      .map(EisBasicDate(_))

  implicit val EisBasicDateWrites: Writes[EisBasicDate] = new Writes[EisBasicDate] {
    def writes(eisBasicDate: EisBasicDate) = JsString(eisBasicDate.toTpi05DateString)
  }

  private def getResult(parseResult: Try[EisBasicDate]): JsResult[EisBasicDate] =
    parseResult.toEither.fold(e => JsError(e.getMessage): JsResult[EisBasicDate], a => JsSuccess(a))

  implicit val EisBasicDateReads: Reads[EisBasicDate] = new Reads[EisBasicDate] {
    override def reads(json: JsValue): JsResult[EisBasicDate] =
      json
        .validate[String]
        .map(parse)
        .flatMap(getResult)
  }
}
