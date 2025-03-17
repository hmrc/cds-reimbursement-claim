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

object WAFRules {

  def asSafeText(text: String): String =
    val sanitized = text
      .filter {
        case '"' | '`' | '\'' => false
        case _                => true
      }
      .replace("%", " percent ")
      .replace("$", " dollar ")
      .map(c =>
        c match {
          case d if Character.isLetterOrDigit(d)                                                                    => d
          case '\n' | '\t'                                                                                          => c
          case ' ' | '.' | ',' | ';' | ':' | '?' | '+' | '-' | '=' | '*' | '/' | '(' | ')' | '[' | ']' | '_' | '\\' => c
          case '£' | '€' | '¥'                                                                                      => c
          case _                                                                                                    => ' '
        }
      )
      .replaceAll("[ ]+", " ")
    println(s"Rejected text: $text")
    println(s"Sanitized text: $sanitized")
    sanitized

}
