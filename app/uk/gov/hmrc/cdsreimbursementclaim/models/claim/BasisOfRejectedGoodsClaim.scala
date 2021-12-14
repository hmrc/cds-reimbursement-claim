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
import play.api.libs.json.Format
import uk.gov.hmrc.cdsreimbursementclaim.utils.EnumerationFormat

sealed abstract class BasisOfRejectedGoodsClaim(val value: String) extends Product with Serializable

object BasisOfRejectedGoodsClaim {

  case object DamagedBeforeClearance extends BasisOfRejectedGoodsClaim("Damaged Before Clearance")

  case object Defective extends BasisOfRejectedGoodsClaim("Defective")

  case object NotInAccordanceWithContract extends BasisOfRejectedGoodsClaim("Not In Accordance with Contract")

  case object SpecialCircumstances extends BasisOfRejectedGoodsClaim("Special Circumstances")

  private val mappings =
    Seq(
      DamagedBeforeClearance,
      Defective,
      NotInAccordanceWithContract,
      SpecialCircumstances
    ).map(item => (item.toString, item)).toMap

  implicit val format: Format[BasisOfRejectedGoodsClaim] =
    EnumerationFormat(mappings)

  implicit val equality: Eq[BasisOfRejectedGoodsClaim] =
    Eq.fromUniversalEquals[BasisOfRejectedGoodsClaim]
}
