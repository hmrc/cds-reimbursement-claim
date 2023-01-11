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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Success

class AcceptanceDateSpec extends AnyWordSpec with Matchers {

  "AcceptanceDate" when {
    "formatting the date" must {

      "convert date to display format" in {
        val acceptanceDate = AcceptanceDate("2010-03-12")
        acceptanceDate.flatMap(_.toDisplayString) should be(Success("12 March 2010"))
      }

      "convert date from display format" in {
        val acceptanceDate = AcceptanceDate.fromDisplayFormat("12 March 2010")
        acceptanceDate.flatMap(_.toTpi05DateString) shouldBe Success("20100312")
      }

      "convert from local date format" in {
        val acceptanceDate = AcceptanceDate("2010-03-12")
        acceptanceDate.flatMap(_.toTpi05DateString) shouldBe Success("20100312")
      }
    }
  }
}
