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

sealed trait BasisOfClaim extends Product with Serializable {
  def toTPI05Key: String
}

object BasisOfClaim {

  case object DuplicateEntry extends BasisOfClaim {
    def toTPI05Key: String = "Duplicate Entry"
  }

  case object DutySuspension extends BasisOfClaim {
    def toTPI05Key: String = "Duty Suspension"
  }

  case object EndUseRelief extends BasisOfClaim {
    def toTPI05Key: String = "End Use"
  }

  case object IncorrectCommodityCode extends BasisOfClaim {
    def toTPI05Key: String = "Incorrect Commodity Code"
  }

  case object IncorrectCpc extends BasisOfClaim {
    def toTPI05Key: String = "Incorrect CPC"
  }

  case object IncorrectValue extends BasisOfClaim {
    def toTPI05Key: String = "Incorrect Value"
  }

  case object IncorrectEoriAndDefermentAccountNumber extends BasisOfClaim {
    def toTPI05Key: String = "Incorrect EORI & Deferment Acc. Num."
  }

  case object InwardProcessingReliefFromCustomsDuty extends BasisOfClaim {
    def toTPI05Key: String = "IP"
  }

  case object Miscellaneous extends BasisOfClaim {
    def toTPI05Key: String = "Miscellaneous"
  }

  case object OutwardProcessingRelief extends BasisOfClaim {
    def toTPI05Key: String = "OPR"
  }

  case object PersonalEffects extends BasisOfClaim {
    def toTPI05Key: String = "Personal Effects"
  }

  case object Preference extends BasisOfClaim {
    def toTPI05Key: String = "Preference"
  }

  case object RGR extends BasisOfClaim {
    def toTPI05Key: String = "RGR"
  }

  case object ProofOfReturnRefundGiven extends BasisOfClaim {
    def toTPI05Key: String = "Proof of Return/Refund Given"
  }

  case object EvidenceThatGoodsHaveNotEnteredTheEU extends BasisOfClaim {
    def toTPI05Key: String = "Evidence That Goods Have Not Entered The EU"
  }

  case object IncorrectExciseValue extends BasisOfClaim {
    def toTPI05Key: String = "Incorrect Excise Value"
  }

  case object IncorrectAdditionalInformationCode extends BasisOfClaim {
    def toTPI05Key: String = "Incorrect Additional Information Code"
  }

  implicit val equality: Eq[BasisOfClaim] = Eq.fromUniversalEquals

  implicit val format: OFormat[BasisOfClaim] = derived.oformat[BasisOfClaim]()
}
