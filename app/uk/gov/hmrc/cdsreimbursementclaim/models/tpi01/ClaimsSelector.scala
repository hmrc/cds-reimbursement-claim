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

package uk.gov.hmrc.cdsreimbursementclaim.models.tpi01

import uk.gov.hmrc.cdsreimbursementclaim.utils.EnumerationFormat

/** Claims selector in the TPI01 request:
  *
  * O = All Overpayments(NDRC + Securities) only
  * N = NDRC only
  * S = Securities only
  * U = Underpayments only
  * A = All cases
  */
sealed trait ClaimsSelector {
  val value: String
}

object ClaimsSelector extends EnumerationFormat[ClaimsSelector] {

  final case object All extends ClaimsSelector {
    override val value: String = "A"
  }
  final case object Overpayments extends ClaimsSelector {
    override val value: String = "O"
  }
  final case object Ndrc extends ClaimsSelector {
    override val value: String = "N"
  }
  final case object Securities extends ClaimsSelector {
    override val value: String = "S"
  }
  final case object Underpayments extends ClaimsSelector {
    override val value: String = "U"
  }

  override val values: Set[ClaimsSelector] =
    Set(All, Overpayments, Ndrc, Securities, Underpayments)
}
