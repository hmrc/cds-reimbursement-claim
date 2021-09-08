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

import cats.data.NonEmptyList
import play.api.libs.json.{Json, OFormat}

final case class Claim(
  paymentMethod: String,
  paymentReference: String,
  taxCategory: TaxCategory,
  taxCode: TaxCode,
  paidAmount: BigDecimal,
  claimAmount: BigDecimal,
  isFilled: Boolean
)

object Claim {
  implicit class ListClaimOps(private val claims: NonEmptyList[Claim]) {
    def total: BigDecimal = claims.map(c => c.claimAmount).toList.sum
  }
  implicit val format: OFormat[Claim] = Json.format[Claim]
}
