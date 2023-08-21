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

package uk.gov.hmrc.cdsreimbursementclaim.utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class WAFRulesSpec extends AnyWordSpec with Matchers {

  "WAFRules" should {
    "convert text to safe WAF-accepted version" in {
      WAFRules.asSafeText("Hel-lo!")                            shouldBe "Hel-lo "
      WAFRules.asSafeText(
        "Applying 5% duty was wrong on our side."
      )                                                         shouldBe "Applying 5 per cent duty was wrong on our side."
      WAFRules.asSafeText(
        "My guess about the duty rate was wrong (Sorry for that!)."
      )                                                         shouldBe "My guess about the duty rate was wrong  Sorry for that  ."
      WAFRules.asSafeText("The original price was £500 (687$)") shouldBe "The original price was GBP 500  687 USD "
    }
  }

}
