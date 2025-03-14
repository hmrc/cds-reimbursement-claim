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
      WAFRules.asSafeText("?;[H]+,e*/(l)-\"l=o\"\\!<>")           shouldBe "?;[H]+,e*/(l)-’l=o’ "
      WAFRules.asSafeText(
        "Applying 5% duty \nwas wrong;\ton our side."
      )                                                           shouldBe "Applying 5 percent duty \nwas wrong;\ton our side."
      WAFRules.asSafeText(
        "My guess about the duty rate was wrong (Sorry for that!)."
      )                                                           shouldBe "My guess about the duty rate was wrong (Sorry for that )."
      WAFRules.asSafeText("The original [price] was £500 (687$)") shouldBe "The original [price] was £500 (687 dollar )"
      WAFRules.asSafeText(
        "VAT £103.60 has been charged in error as goods imported were personal items, left in the hotel (item ''Iphone ear pod'' left in the hotel and returned back to the UK.) Customer (Private individual) has provided original sales purchase receipt and commercial invoice also states ''lost Ipod'' was shipped to UK"
      )                                                           shouldBe "VAT £103.60 has been charged in error as goods imported were personal items, left in the hotel (item ’Iphone ear pod’ left in the hotel and returned back to the UK.) Customer (Private individual) has provided original sales purchase receipt and commercial invoice also states ’lost Ipod’ was shipped to UK"
      WAFRules.asSafeText(
        "WRONG COMERCIAL INVOICE WAS USED ON ENTRY, WHICH LEAD TO VALUE BEING OVER DECALRED. INVOICE \"K2407\" $27405 USED INCORRECTLY CORRECT INVOICE \"CM-000-UK\" IS $20453 Value over declared by $6952 / £5625.96"
      )                                                           shouldBe "WRONG COMERCIAL INVOICE WAS USED ON ENTRY, WHICH LEAD TO VALUE BEING OVER DECALRED. INVOICE ’K2407’ dollar 27405 USED INCORRECTLY CORRECT INVOICE ’CM-000-UK’ IS dollar 20453 Value over declared by dollar 6952 / £5625.96"
    }
  }

}
