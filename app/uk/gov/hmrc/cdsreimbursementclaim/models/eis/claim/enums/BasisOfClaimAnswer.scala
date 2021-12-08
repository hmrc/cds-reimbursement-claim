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

  case object DuplicateEntry extends BasisOfClaimAnswer {
    override def toString: String = "Duplicate Entry"
  }

  case object DutySuspension extends BasisOfClaimAnswer {
    override def toString: String = "Duty Suspension"
  }

  case object EndUseRelief extends BasisOfClaimAnswer {
    override def toString: String = "End Use"
  }

  case object IncorrectCommodityCode extends BasisOfClaimAnswer {
    override def toString: String = "Incorrect Commodity Code"
  }

  case object IncorrectCpc extends BasisOfClaimAnswer {
    override def toString: String = "Incorrect CPC"
  }

  case object IncorrectValue extends BasisOfClaimAnswer {
    override def toString: String = "Incorrect Value"
  }

  case object IncorrectEoriAndDefermentAccountNumber extends BasisOfClaimAnswer {
    override def toString: String = "Incorrect EORI & Deferment Acc. Num."
  }

  case object InwardProcessingReliefFromCustomsDuty extends BasisOfClaimAnswer {
    override def toString: String = "IP"
  }

  case object Miscellaneous extends BasisOfClaimAnswer {
    override def toString: String = "Miscellaneous"
  }

  case object OutwardProcessingRelief extends BasisOfClaimAnswer {
    override def toString: String = "OPR"
  }

  case object PersonalEffects extends BasisOfClaimAnswer {
    override def toString: String = "Personal Effects"
  }

  case object Preference extends BasisOfClaimAnswer {
    override def toString: String = "Preference"
  }

  case object RGR extends BasisOfClaimAnswer {
    override def toString: String = "RGR"
  }

  case object ProofOfReturnRefundGiven extends BasisOfClaimAnswer {
    override def toString: String = "Proof of Return/Refund Given"
  }

  case object EvidenceThatGoodsHaveNotEnteredTheEU extends BasisOfClaimAnswer {
    override def toString: String = "Evidence That Goods Have Not Entered The EU"
  }

  case object IncorrectExciseValue extends BasisOfClaimAnswer {
    override def toString: String = "Incorrect Excise Value"
  }

  case object IncorrectAdditionalInformationCode extends BasisOfClaimAnswer {
    override def toString: String = "Incorrect Additional Information Code"
  }

  implicit val eq: Eq[BasisOfClaimAnswer] = Eq.fromUniversalEquals

  implicit val format: OFormat[BasisOfClaimAnswer] = derived.oformat[BasisOfClaimAnswer]()

}
