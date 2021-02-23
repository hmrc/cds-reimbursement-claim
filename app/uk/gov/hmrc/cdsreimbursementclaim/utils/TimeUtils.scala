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

package uk.gov.hmrc.cdsreimbursementclaim.utils

import org.joda.time.{DateTime, DateTimeZone}

import java.time._
import java.time.format.DateTimeFormatter
import java.util.TimeZone
import scala.util.Try

object TimeUtils {

  def acceptanceDateDisplayFormat(acceptanceDate: String): Option[String] = {
    val result = for {
      t <- Try(LocalDate.parse(acceptanceDate, DateTimeFormatter.ofPattern("u-M-d")))
      f <- Try(DateTimeFormatter.ofPattern("d MMMM u").format(t))
    } yield f
    result.toOption
  }

  val cdsDateTimeFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.systemDefault())

  def cdsDateTimeNow: String = cdsDateTimeFormat.format(LocalDateTime.now)

  val rfc7231DateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O")
  def rfc7231DateTimeNow: String               = rfc7231DateTimeFormat.format(ZonedDateTime.now(ZoneOffset.UTC))

  val iso8601DateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
  def iso8601DateTimeNow: String               = iso8601DateTimeFormat.format(ZonedDateTime.now(ZoneOffset.UTC))

  def isoLocalDateNow: String = DateTimeFormatter.ISO_LOCAL_DATE.format(ZonedDateTime.now(ZoneOffset.UTC))

  implicit class JavaToJoda(clock: Clock) {
    def nowAsJoda: DateTime =
      new DateTime(clock.instant().toEpochMilli, DateTimeZone.forTimeZone(TimeZone.getTimeZone(clock.getZone)))
  }
}
