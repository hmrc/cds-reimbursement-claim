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
import play.api.libs.json.OFormat

sealed abstract class TaxCategory(val value: String) extends Product with Serializable

object TaxCategory {

  case object UkDuty extends TaxCategory("uk-duty")
  case object EuDuty extends TaxCategory("eu-duty")
  case object Beer extends TaxCategory("beer")
  case object Wine extends TaxCategory("wine")
  case object MadeWine extends TaxCategory("made-wine")
  case object LowAlcoholBeverages extends TaxCategory("low-alcohol-beverages")
  case object Spirits extends TaxCategory("spirits")
  case object CiderPerry extends TaxCategory("cider-perry")
  case object HydrocarbonOils extends TaxCategory("hydrocarbon-oils")
  case object Biofuels extends TaxCategory("biofuels")
  case object MiscellaneousRoadFuels extends TaxCategory("miscellaneous-road-fuels")
  case object Tobacco extends TaxCategory("tobacco")
  case object ClimateChangeLevy extends TaxCategory("climate-change-levy")

  implicit val taxCategoryFormat: OFormat[TaxCategory] = derived.oformat[TaxCategory]()
  implicit val eq: Eq[TaxCategory]                     = Eq.fromUniversalEquals[TaxCategory]

}
