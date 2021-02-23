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

package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import play.api.libs.functional.syntax.toInvariantFunctorOps
import play.api.libs.json.Format

import java.util.function.Predicate

final case class Postcode(value: String) extends AnyVal

object Postcode {

  implicit val format: Format[Postcode] =
    implicitly[Format[String]].inmap(Postcode(_), _.value)

  val postcodeRegexPredicate: Predicate[String] =
    "^[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}$|BFPO\\s?[0-9]{1,3}$".r.pattern
      .asPredicate()

}
