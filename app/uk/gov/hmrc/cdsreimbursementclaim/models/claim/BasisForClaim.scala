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

import julienrf.json.derived
import play.api.libs.json.OFormat

sealed trait BasisForClaim extends Product with Serializable

object BasisForClaim {
  case object DuplicateMrnEntry extends BasisForClaim
  case object DutySuspension extends BasisForClaim
  case object EndUseRelief extends BasisForClaim
  case object IncorrectCommodityCode extends BasisForClaim
  case object IncorrectCpc extends BasisForClaim
  case object IncorrectValue extends BasisForClaim
  case object IncorrectEoriAndDefermentAccountNumber extends BasisForClaim
  case object InwardProcessingReliefFromCustomsDuty extends BasisForClaim
  case object OutwardProcessingRelief extends BasisForClaim
  case object Preference extends BasisForClaim
  case object ProofOfReturnRefundGiven extends BasisForClaim

  implicit def basisForClaimToString(basisForClaim: BasisForClaim): String = basisForClaim match {
    case DuplicateMrnEntry                      => "Duplicate Mrn or Entry number"
    case DutySuspension                         => "Duty Suspension"
    case EndUseRelief                           => "End Use Relief"
    case IncorrectCommodityCode                 => "Incorrect Commodity Code"
    case IncorrectCpc                           => "Incorrect Cpc"
    case IncorrectValue                         => "Incorrect Value"
    case IncorrectEoriAndDefermentAccountNumber => "Incorrect Eori and Deferment Account Number"
    case InwardProcessingReliefFromCustomsDuty  => "Inward Processing Relief from Customs Duty"
    case OutwardProcessingRelief                => "Outward Processing Relief"
    case Preference                             => "Preference"
    case ProofOfReturnRefundGiven               => "Proof of Return and Refund Given"
  }

  implicit val format: OFormat[BasisForClaim] = derived.oformat[BasisForClaim]()

}
