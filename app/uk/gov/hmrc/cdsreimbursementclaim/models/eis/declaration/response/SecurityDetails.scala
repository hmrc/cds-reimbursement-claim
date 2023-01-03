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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response

import cats.syntax.eq._
import play.api.libs.json.{Json, OFormat}

final case class SecurityDetails(
  securityDepositId: String,
  totalAmount: String,
  amountPaid: String,
  paymentMethod: String,
  paymentReference: String,
  taxDetails: List[TaxDetails]
) {

  def isGuaranteeEligible: Boolean  = paymentMethod === "004"
  def isBankAccountPayment: Boolean = paymentMethod === "001"
}

object SecurityDetails {
  implicit val format: OFormat[SecurityDetails] = Json.format[SecurityDetails]
}
