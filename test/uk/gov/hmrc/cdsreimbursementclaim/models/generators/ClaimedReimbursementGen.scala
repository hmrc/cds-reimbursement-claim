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

package uk.gov.hmrc.cdsreimbursementclaim.models.generators

import org.scalacheck.magnolia._
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ClaimedReimbursement
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.PaymentMethodGen.genPaymentMethod
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TaxCodesGen.genTaxCode

object ClaimedReimbursementGen {

  lazy val genClaimedReimbursement: Gen[ClaimedReimbursement] = for {
    id               <- genUUID
    paymentMethod    <- genPaymentMethod
    paymentReference <- genStringWithMaxSizeOfN(max = 18)
    taxCode          <- genTaxCode
    paidAmount       <- Gen.posNum[Long].map(BigDecimal(_))
    claimAmount      <- Gen.posNum[Long].map(BigDecimal(_))
    isFilled         <- genBoolean
  } yield ClaimedReimbursement(
    id,
    paymentMethod,
    paymentReference,
    taxCode,
    paidAmount,
    claimAmount,
    isFilled
  )

  implicit lazy val arbitraryClaimedReimbursement: Typeclass[ClaimedReimbursement] = Arbitrary(genClaimedReimbursement)
}
