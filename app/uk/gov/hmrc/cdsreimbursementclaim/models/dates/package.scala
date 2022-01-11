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

package uk.gov.hmrc.cdsreimbursementclaim.models

import org.joda.time.{DateTime, DateTimeZone}

import java.time.Clock
import java.time.temporal.TemporalAccessor
import java.util.TimeZone

package object dates {

  implicit class TemporalAccessorOps(val temporalAccessor: TemporalAccessor) extends AnyVal {

    def toIsoLocalDate: String = ISOLocalDate.of(temporalAccessor)

    def toCdsDateTime: String = CdsDateTime.of(temporalAccessor)
  }

  implicit class JavaToJoda(val clock: Clock) extends AnyVal {
    def nowAsJoda: DateTime =
      new DateTime(clock.instant().toEpochMilli, DateTimeZone.forTimeZone(TimeZone.getTimeZone(clock.getZone)))
  }
}
