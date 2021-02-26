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

import cats.Eq
import julienrf.json.derived
import play.api.i18n.Messages
import play.api.libs.json.OFormat

sealed trait Address extends Product with Serializable

object Address {

  val addressLineAllowedCharacters: List[Char] =
    ('A' to 'Z').toList ::: ('a' to 'z').toList ::: ('0' to '9').toList :::
      List(' ', '-', ',', '.', '&', '\'')

  val addressLineMaxLength: Int = 35

  final case class UkAddress(
    line1: String,
    line2: Option[String],
    town: Option[String],
    county: Option[String],
    postcode: Postcode
  ) extends Address {
    val countryCode: String = Country.uk.code
  }
  implicit val ukAddressFormat: OFormat[UkAddress] = derived.oformat[UkAddress]()

  final case class NonUkAddress(
    line1: String,
    line2: Option[String],
    line3: Option[String],
    line4: String,
    line5: Option[String],
    postcode: Option[String],
    country: Country
  ) extends Address

  implicit val nonUkAddressFormat: OFormat[NonUkAddress] = derived.oformat[NonUkAddress]()

  implicit val format: OFormat[Address] = derived.oformat()

  implicit val eq: Eq[Address] = Eq.fromUniversalEquals

  implicit class AddressOps(private val a: Address) extends AnyVal {
    def getAddressLines(implicit messages: Messages): List[String] = {
      val lines = a match {
        case u: UkAddress    => List(Some(u.line1), u.line2, u.town, u.county, Some(u.postcode.value))
        case n: NonUkAddress =>
          List(
            Some(n.line1),
            n.line2,
            n.line3,
            Some(n.line4),
            n.line5,
            n.postcode,
            messages.translate(s"country.${n.country.code}", Seq.empty)
          )
      }
      lines.collect { case Some(s) => s }
    }
  }

}
