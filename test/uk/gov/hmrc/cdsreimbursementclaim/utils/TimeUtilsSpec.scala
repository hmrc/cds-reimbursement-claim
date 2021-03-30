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

import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpec

class TimeUtilsSpec extends AnyWordSpec with Matchers {

  "Time Utils" when {
    "formatting the date" must {

      "format the acceptance date" in {
        val acceptanceDate        = "2010-03-12"
        val expectedFormattedDate = "12 March 2010"
        TimeUtils.toDisplayAcceptanceDateFormat(acceptanceDate) shouldBe Some(expectedFormattedDate)
      }

      "format the mrn acceptance date" in {
        val acceptanceDate        = "12 March 2010"
        val expectedFormattedDate = "20100312"
        TimeUtils.fromDisplayAcceptanceDateFormat(acceptanceDate) shouldBe Some(expectedFormattedDate)
      }

      "format the date of import" in {
        val acceptanceDate        = "2010-03-12"
        val expectedFormattedDate = "20100312"
        TimeUtils.fromDateOfImportAcceptanceDateFormat(acceptanceDate) shouldBe Some(expectedFormattedDate)
      }

    }
  }

}
