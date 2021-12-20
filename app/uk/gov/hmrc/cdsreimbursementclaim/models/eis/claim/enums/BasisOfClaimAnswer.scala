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

sealed trait BasisOfClaimAnswer extends Product with Serializable

object BasisOfClaimAnswer {

  case object DuplicateEntry extends BasisOfClaimAnswer
  case object DutySuspension extends BasisOfClaimAnswer
  case object EndUseRelief extends BasisOfClaimAnswer
  case object IncorrectCommodityCode extends BasisOfClaimAnswer
  case object IncorrectCpc extends BasisOfClaimAnswer
  case object IncorrectValue extends BasisOfClaimAnswer
  case object IncorrectEoriAndDefermentAccountNumber extends BasisOfClaimAnswer
  case object InwardProcessingReliefFromCustomsDuty extends BasisOfClaimAnswer
  case object Miscellaneous extends BasisOfClaimAnswer
  case object OutwardProcessingRelief extends BasisOfClaimAnswer
  case object PersonalEffects extends BasisOfClaimAnswer
  case object Preference extends BasisOfClaimAnswer
  case object RGR extends BasisOfClaimAnswer
  case object ProofOfReturnRefundGiven extends BasisOfClaimAnswer
  case object EvidenceThatGoodsHaveNotEnteredTheEU extends BasisOfClaimAnswer
  case object IncorrectExciseValue extends BasisOfClaimAnswer
  case object IncorrectAdditionalInformationCode extends BasisOfClaimAnswer

  // $COVERAGE-OFF$
  def basisOfClaimToString(basisForClaim: BasisOfClaimAnswer): String = basisForClaim match {
    case DuplicateEntry                         => "Duplicate Entry"
    case DutySuspension                         => "Duty Suspension"
    case EndUseRelief                           => "End Use"
    case IncorrectCommodityCode                 => "Incorrect Commodity Code"
    case IncorrectCpc                           => "Incorrect CPC"
    case IncorrectValue                         => "Incorrect Value"
    case IncorrectEoriAndDefermentAccountNumber => "Incorrect EORI & Deferment Acc. Num."
    case InwardProcessingReliefFromCustomsDuty  => "IP"
    case Miscellaneous                          => "Miscellaneous"
    case OutwardProcessingRelief                => "OPR"
    case PersonalEffects                        => "Personal Effects"
    case Preference                             => "Preference"
    case RGR                                    => "RGR"
    case ProofOfReturnRefundGiven               => "Proof of Return/Refund Given"
    case EvidenceThatGoodsHaveNotEnteredTheEU   => "Evidence That Goods Have Not Entered The EU"
    case IncorrectExciseValue                   => "Incorrect Excise Value"
    case IncorrectAdditionalInformationCode     => "Incorrect Additional Information Code"
  }
  // $COVERAGE-ON$

  implicit val eq: Eq[BasisOfClaimAnswer] = Eq.fromUniversalEquals

  implicit val format: OFormat[BasisOfClaimAnswer] = derived.oformat[BasisOfClaimAnswer]()

}