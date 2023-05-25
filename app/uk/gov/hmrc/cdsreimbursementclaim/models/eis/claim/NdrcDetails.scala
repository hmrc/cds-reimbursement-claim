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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim

import cats.data.{Validated, ValidatedNel}
import cats.implicits.{catsSyntaxEq, catsSyntaxTuple5Semigroupal}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.TaxCode

final case class NdrcDetails(
  paymentMethod: String,
  paymentReference: String,
  CMAEligible: Option[String],
  taxType: TaxCode,
  amount: String,
  claimAmount: Option[String]
)

object NdrcDetails {

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def buildChecking(
    taxCode: TaxCode,
    paymentMethod: String,
    paymentReference: String,
    paidAmount: BigDecimal,
    claimedAmount: BigDecimal
  ): ValidatedNel[Error, NdrcDetails] = (
    Validator.validatePaymentMethod(paymentMethod),
    Validator.validatePaymentReference(paymentReference),
    Validator.validateTaxCode(taxCode),
    Validator.validateAmount(paidAmount.toString()),
    Validator.validateAmount(claimedAmount.toString())
  ).mapN {
    (
      validatedPaymentMethod,
      validatedPaymentReference,
      validatedTaxCode,
      validatedPaidAmount,
      validatedClaimedAmount
    ) =>
      NdrcDetails(
        validatedPaymentMethod,
        validatedPaymentReference,
        None,
        validatedTaxCode,
        validatedPaidAmount,
        Some(validatedClaimedAmount)
      )
  }

  final object Validator {

    def validateAmount(amount: String): ValidatedNel[Error, String] =
      Validated.condNel(
        amount.matches("^-?[0-9]{1,11}[.][0-9]{1,2}|$^-?[0-9]{1,11}$"),
        amount,
        Error(s"Bad amount format: $amount")
      )

    def validatePaymentMethod(paymentMethod: String): ValidatedNel[Error, String] =
      Validated.condNel(
        paymentMethod.length === 3,
        paymentMethod,
        Error(s"The payment method is expected to be 3 characters long: $paymentMethod")
      )

    def validatePaymentReference(paymentReference: String): ValidatedNel[Error, String] =
      Validated.condNel(
        paymentReference.nonEmpty && paymentReference.length <= 18,
        paymentReference,
        Error(s"The payment reference is blank or exceeds 18 characters: $paymentReference")
      )

    def validateTaxCode(taxCode: TaxCode): ValidatedNel[Error, TaxCode] =
      Validated.condNel(taxCode.value.length === 3, taxCode, Error(s"Tax type size is less than 3: ${taxCode.value}"))
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit val format: OFormat[NdrcDetails] = Json.format[NdrcDetails]
}
