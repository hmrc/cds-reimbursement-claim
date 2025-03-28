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

import uk.gov.hmrc.cdsreimbursementclaim.utils.EnumerationFormat

sealed trait BasisOfRejectedGoodsClaim {
  def toTPI05DisplayString: String
}

object BasisOfRejectedGoodsClaim extends EnumerationFormat[BasisOfRejectedGoodsClaim] {

  case object DamagedBeforeClearance extends BasisOfRejectedGoodsClaim {
    def toTPI05DisplayString: String = "Damaged Before Clearance"
  }

  case object Defective extends BasisOfRejectedGoodsClaim {
    def toTPI05DisplayString: String = "Defective"
  }

  case object NotInAccordanceWithContract extends BasisOfRejectedGoodsClaim {
    def toTPI05DisplayString: String = "Not In Accordance With Contract"
  }

  case object SpecialCircumstances extends BasisOfRejectedGoodsClaim {
    def toTPI05DisplayString: String = "Special Circumstances"
  }

  override lazy val values: Set[BasisOfRejectedGoodsClaim] =
    Set(DamagedBeforeClearance, Defective, NotInAccordanceWithContract, SpecialCircumstances)
}
