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

package uk.gov.hmrc.cdsreimbursementclaim.utils

import cats.data.NonEmptyList
import cats.data.Validated.{Invalid, Valid}
import cats.implicits.catsSyntaxEq
import uk.gov.hmrc.cdsreimbursementclaim.models.Validation

object DataUtils {

  private val financialAmountRegex: String = "^-?[0-9]{1,11}[.][0-9]{1,2}|$^-?[0-9]{1,11}$"

  def isValidAmount(amount: BigDecimal): Validation[String] =
    if ((amount.toString().matches(financialAmountRegex))) {
      Valid(amount.toString())
    } else
      Invalid(NonEmptyList.one(s"amount is in incorrect format: ${amount.toString()}"))

  def isValidTaxType(taxType: String): Validation[String] =
    if (taxType.length === 3) Valid(taxType) else Invalid(NonEmptyList.one(s"tax type is of incorrect size : $taxType"))

  def isValidPaymentReference(paymentReference: String): Validation[String] =
    if (paymentReference.nonEmpty && paymentReference.length <= 18) Valid(paymentReference)
    else Invalid(NonEmptyList.one(s"payment reference is of incorrect size : $paymentReference"))

  def isValidPaymentMethod(paymentMethod: String): Validation[String] =
    if (paymentMethod.length === 3) Valid(paymentMethod)
    else Invalid(NonEmptyList.one(s"payment method is of incorrect size : $paymentMethod"))

}
