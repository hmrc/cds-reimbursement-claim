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

import cats.Eq
import play.api.libs.json.Format
import uk.gov.hmrc.cdsreimbursementclaim.utils.SimpleStringFormat

sealed abstract class TaxCode(val value: String) extends Product with Serializable {
  override def toString: String = value
}

object TaxCode {

  final case class UnsupportedTaxCode(taxCode: String) extends TaxCode(taxCode)

  case object A00 extends TaxCode("A00")
  case object A20 extends TaxCode("A20")
  case object A30 extends TaxCode("A30")
  case object A35 extends TaxCode("A35")
  case object A40 extends TaxCode("A40")
  case object A45 extends TaxCode("A45")
  case object B00 extends TaxCode("B00")
  case object A50 extends TaxCode("A50")
  case object A70 extends TaxCode("A70")
  case object A80 extends TaxCode("A80")
  case object A85 extends TaxCode("A85")
  case object A90 extends TaxCode("A90")
  case object A95 extends TaxCode("A95")
  case object B05 extends TaxCode("B05")
  case object NI301 extends TaxCode("301")
  case object NI311 extends TaxCode("311")
  case object NI312 extends TaxCode("312")
  case object NI313 extends TaxCode("313")
  case object NI314 extends TaxCode("314")
  case object NI315 extends TaxCode("315")
  case object NI321 extends TaxCode("321")
  case object NI322 extends TaxCode("322")
  case object NI323 extends TaxCode("323")
  case object NI324 extends TaxCode("324")
  case object NI325 extends TaxCode("325")
  case object NI331 extends TaxCode("331")
  case object NI333 extends TaxCode("333")
  case object NI334 extends TaxCode("334")
  case object NI335 extends TaxCode("335")
  case object NI341 extends TaxCode("341")
  case object NI343 extends TaxCode("343")
  case object NI344 extends TaxCode("344")
  case object NI345 extends TaxCode("345")
  case object NI351 extends TaxCode("351")
  case object NI352 extends TaxCode("352")
  case object NI353 extends TaxCode("353")
  case object NI354 extends TaxCode("354")
  case object NI355 extends TaxCode("355")
  case object NI356 extends TaxCode("356")
  case object NI357 extends TaxCode("357")
  case object NI358 extends TaxCode("358")
  case object NI359 extends TaxCode("359")
  case object NI360 extends TaxCode("360")
  case object NI361 extends TaxCode("361")
  case object NI362 extends TaxCode("362")
  case object NI363 extends TaxCode("363")
  case object NI364 extends TaxCode("364")
  case object NI365 extends TaxCode("365")
  case object NI366 extends TaxCode("366")
  case object NI367 extends TaxCode("367")
  case object NI368 extends TaxCode("368")
  case object NI369 extends TaxCode("369")
  case object NI370 extends TaxCode("370")
  case object NI371 extends TaxCode("371")
  case object NI372 extends TaxCode("372")
  case object NI373 extends TaxCode("373")
  case object NI374 extends TaxCode("374")
  case object NI375 extends TaxCode("375")
  case object NI376 extends TaxCode("376")
  case object NI377 extends TaxCode("377")
  case object NI378 extends TaxCode("378")
  case object NI379 extends TaxCode("379")
  case object NI380 extends TaxCode("380")
  case object NI407 extends TaxCode("407")
  case object NI411 extends TaxCode("411")
  case object NI412 extends TaxCode("412")
  case object NI413 extends TaxCode("413")
  case object NI415 extends TaxCode("415")
  case object NI419 extends TaxCode("419")
  case object NI421 extends TaxCode("421")
  case object NI422 extends TaxCode("422")
  case object NI423 extends TaxCode("423")
  case object NI425 extends TaxCode("425")
  case object NI429 extends TaxCode("429")
  case object NI431 extends TaxCode("431")
  case object NI433 extends TaxCode("433")
  case object NI435 extends TaxCode("435")
  case object NI438 extends TaxCode("438")
  case object NI440 extends TaxCode("440")
  case object NI441 extends TaxCode("441")
  case object NI442 extends TaxCode("442")
  case object NI443 extends TaxCode("443")
  case object NI444 extends TaxCode("444")
  case object NI445 extends TaxCode("445")
  case object NI446 extends TaxCode("446")
  case object NI447 extends TaxCode("447")
  case object NI451 extends TaxCode("451")
  case object NI461 extends TaxCode("461")
  case object NI462 extends TaxCode("462")
  case object NI463 extends TaxCode("463")
  case object NI473 extends TaxCode("473")
  case object NI481 extends TaxCode("481")
  case object NI483 extends TaxCode("483")
  case object NI485 extends TaxCode("485")
  case object NI487 extends TaxCode("487")
  case object NI511 extends TaxCode("511")
  case object NI520 extends TaxCode("520")
  case object NI521 extends TaxCode("521")
  case object NI522 extends TaxCode("522")
  case object NI540 extends TaxCode("540")
  case object NI541 extends TaxCode("541")
  case object NI542 extends TaxCode("542")
  case object NI546 extends TaxCode("546")
  case object NI551 extends TaxCode("551")
  case object NI556 extends TaxCode("556")
  case object NI561 extends TaxCode("561")
  case object NI570 extends TaxCode("570")
  case object NI571 extends TaxCode("571")
  case object NI572 extends TaxCode("572")
  case object NI589 extends TaxCode("589")
  case object NI591 extends TaxCode("591")
  case object NI592 extends TaxCode("592")
  case object NI595 extends TaxCode("595")
  case object NI597 extends TaxCode("597")
  case object NI611 extends TaxCode("611")
  case object NI615 extends TaxCode("615")
  case object NI619 extends TaxCode("619")
  case object NI623 extends TaxCode("623")
  case object NI627 extends TaxCode("627")
  case object NI633 extends TaxCode("633")
  case object NI99A extends TaxCode("99A")
  case object NI99B extends TaxCode("99B")
  case object NI99C extends TaxCode("99C")
  case object NI99D extends TaxCode("99D")

  val values: List[TaxCode] = List(
    A00,
    A20,
    A30,
    A35,
    A40,
    A45,
    B00,
    A50,
    A70,
    A80,
    A85,
    A90,
    A95,
    B05,
    NI407,
    NI411,
    NI412,
    NI413,
    NI415,
    NI419,
    NI421,
    NI422,
    NI423,
    NI425,
    NI429,
    NI431,
    NI433,
    NI435,
    NI438,
    NI440,
    NI441,
    NI442,
    NI443,
    NI444,
    NI445,
    NI446,
    NI447,
    NI451,
    NI461,
    NI462,
    NI463,
    NI473,
    NI481,
    NI483,
    NI485,
    NI487,
    NI511,
    NI520,
    NI521,
    NI522,
    NI540,
    NI541,
    NI542,
    NI546,
    NI551,
    NI556,
    NI561,
    NI570,
    NI571,
    NI572,
    NI589,
    NI591,
    NI592,
    NI595,
    NI597,
    NI611,
    NI615,
    NI619,
    NI623,
    NI627,
    NI633,
    NI99A,
    NI99B,
    NI99C,
    NI99D,
    NI301,
    NI311,
    NI312,
    NI313,
    NI314,
    NI315,
    NI321,
    NI322,
    NI323,
    NI324,
    NI325,
    NI331,
    NI333,
    NI334,
    NI335,
    NI341,
    NI343,
    NI344,
    NI345,
    NI351,
    NI352,
    NI353,
    NI353,
    NI354,
    NI355,
    NI356,
    NI357,
    NI358,
    NI359,
    NI360,
    NI361,
    NI362,
    NI363,
    NI364,
    NI365,
    NI366,
    NI367,
    NI368,
    NI369,
    NI370,
    NI371,
    NI372,
    NI373,
    NI374,
    NI375,
    NI376,
    NI377,
    NI378,
    NI379,
    NI380
  )

  private val stringToTaxCodeMap: Map[String, TaxCode] =
    values.map(code => code.value -> code).toMap

  def apply(taxCode: String): Option[TaxCode] =
    stringToTaxCodeMap.get(taxCode)

  def getOrFail(taxCode: String): TaxCode =
    stringToTaxCodeMap.getOrElse(taxCode, UnsupportedTaxCode(taxCode))

  implicit val equality: Eq[TaxCode] = Eq.fromUniversalEquals[TaxCode]

  implicit val format: Format[TaxCode] = SimpleStringFormat(getOrFail, _.value)

  implicit val ordering: Ordering[TaxCode] =
    Ordering.fromLessThan { (t1: TaxCode, t2: TaxCode) =>
      val a = if (t1.value.head.isLetter) "0" + t1.value else t1.value
      val b = if (t2.value.head.isLetter) "0" + t2.value else t2.value
      implicitly[Ordering[String]].lt(a, b)
    }
}
