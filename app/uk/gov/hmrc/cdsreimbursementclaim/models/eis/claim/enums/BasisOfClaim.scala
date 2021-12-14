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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums

import cats.Eq
import julienrf.json.derived
import play.api.libs.json.OFormat

sealed abstract class BasisOfClaim(val value: String) extends Product with Serializable

object BasisOfClaim {

  case object DuplicateEntry extends BasisOfClaim("Duplicate Entry")

  case object DutySuspension extends BasisOfClaim("Duty Suspension")

  case object EndUseRelief extends BasisOfClaim("End Use")

  case object IncorrectCommodityCode extends BasisOfClaim("Incorrect Commodity Code")

  case object IncorrectCpc extends BasisOfClaim("Incorrect CPC")

  case object IncorrectValue extends BasisOfClaim("Incorrect Value")

  case object IncorrectEoriAndDefermentAccountNumber extends BasisOfClaim("Incorrect EORI & Deferment Acc. Num.")

  case object InwardProcessingReliefFromCustomsDuty extends BasisOfClaim("IP")

  case object Miscellaneous extends BasisOfClaim("Miscellaneous")

  case object OutwardProcessingRelief extends BasisOfClaim("OPR")

  case object PersonalEffects extends BasisOfClaim("Personal Effects")

  case object Preference extends BasisOfClaim("Preference")

  case object RGR extends BasisOfClaim("RGR")

  case object ProofOfReturnRefundGiven extends BasisOfClaim("Proof of Return/Refund Given")

  case object EvidenceThatGoodsHaveNotEnteredTheEU extends BasisOfClaim("Evidence That Goods Have Not Entered The EU")

  case object IncorrectExciseValue extends BasisOfClaim("Incorrect Excise Value")

  case object IncorrectAdditionalInformationCode extends BasisOfClaim("Incorrect Additional Information Code")

  implicit val eq: Eq[BasisOfClaim] = Eq.fromUniversalEquals

  implicit val format: OFormat[BasisOfClaim] = derived.oformat[BasisOfClaim]()
}
