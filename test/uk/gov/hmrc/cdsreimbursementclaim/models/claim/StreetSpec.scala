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

package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class StreetSpec extends AnyWordSpec with Matchers {

  "Street object" should {
    "concatenate address" when {
      "line1 does not end with line2" in {
        List(
          (Some("Grand Super Warehouse"), Some("10 Happy Place")),
          (Some("7 Frog Court"), Some("58 Oak Tree Road")),
          (Some("Meerkat House"), Some("15 Beacon Road"))
        ).foreach { case (line1, line2) =>
          val streetResult = Street.fromLines(line1, line2)
          val line1Result  = Street.line1(line1, line2)
          val line2Result  = Street.line2(line1, line2)

          line1Result  shouldBe line1
          line2Result  shouldBe line2
          streetResult shouldBe Some(s"${line1.value} ${line2.value}")
        }
      }

      "line1 plus line2 is 70 characters" in {
        List(
          (Some("Suite 1A, Floor 1, Statham House La"), Some("ncastrian Office Centre Talbot Road")),
          (
            Some("Suite 1A, Floor 1, Statham House Lancastrian"),
            Some(" Office Centre Talbot Road")
          ),
          (Some("Suite 1A, "), Some("Floor 1, Statham House Lancastrian Office Centre Talbot Road"))
        ).foreach { case (line1, line2) =>
          val streetResult = Street.fromLines(line1, line2)
          val line1Result  = Street.line1(line1, line2)
          val line2Result  = Street.line2(line1, line2)

          line1Result  shouldBe line1
          line2Result  shouldBe line2
          streetResult shouldBe Some(s"${line1.value}${line2.value}")
        }
      }
    }

    "not concatenate address" when {
      "line1 ends with line2" in {
        List(
          (Some("Grand Super Warehouse"), Some("10 Happy Place")),
          (Some("7 Frog Court"), Some("58 Oak Tree Road")),
          (Some("Meerkat House"), Some("15 Beacon Road"))
        ).foreach { case (line1, line2) =>
          val line1Andline2 = Some(s"${line1.value} ${line2.value}")

          val streetResult = Street.fromLines(line1Andline2, line2)
          val line1Result  = Street.line1(line1Andline2, line2)
          val line2Result  = Street.line2(line1Andline2, line2)

          line1Result  shouldBe line1
          line2Result  shouldBe line2
          streetResult shouldBe Some(s"${line1.value} ${line2.value}")
        }
      }

      "line2 starts with line1" in {
        List(
          (Some("Grand Super Warehouse"), Some("10 Happy Place")),
          (Some("7 Frog Court"), Some("58 Oak Tree Road")),
          (Some("Meerkat House"), Some("15 Beacon Road"))
        ).foreach { case (line1, line2) =>
          val line1Andline2 = Some(s"${line1.value} ${line2.value}")

          val streetResult = Street.fromLines(line1, line1Andline2)
          val line1Result  = Street.line1(line1, line1Andline2)
          val line2Result  = Street.line2(line1, line1Andline2)

          line1Result  shouldBe line1
          line2Result  shouldBe line2
          streetResult shouldBe Some(s"${line1.value} ${line2.value}")
        }
      }
    }
  }
}
