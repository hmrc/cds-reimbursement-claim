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

import cats.kernel.Eq
import play.api.libs.json.Format
import play.api.libs.json.Json
import cats.kernel.Semigroup

final case class AmountPaidWithCorrect(paidAmount: BigDecimal, correctAmount: BigDecimal)

object AmountPaidWithCorrect {

  val empty: AmountPaidWithCorrect = AmountPaidWithCorrect(0, 0)

  implicit val semigroup: Semigroup[AmountPaidWithCorrect] =
    (x: AmountPaidWithCorrect, y: AmountPaidWithCorrect) =>
      AmountPaidWithCorrect(
        paidAmount = x.paidAmount + y.paidAmount,
        correctAmount = x.correctAmount + y.correctAmount
      )

  implicit val equality: Eq[AmountPaidWithCorrect]   = Eq.fromUniversalEquals[AmountPaidWithCorrect]
  implicit val format: Format[AmountPaidWithCorrect] = Json.format[AmountPaidWithCorrect]

}
