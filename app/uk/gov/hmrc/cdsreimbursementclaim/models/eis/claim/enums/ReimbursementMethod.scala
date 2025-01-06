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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums

import uk.gov.hmrc.cdsreimbursementclaim.utils.EnumerationFormat
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ReimbursementMethodAnswer

sealed trait ReimbursementMethod extends Product with Serializable

object ReimbursementMethod extends EnumerationFormat[ReimbursementMethod] {

  case object Deferment extends ReimbursementMethod {
    override def toString: String = "Deferment"
  }

  case object BankTransfer extends ReimbursementMethod {
    override def toString: String = "Bank Transfer"
  }

  case object PayableOrder extends ReimbursementMethod {
    override def toString: String = "Payable Order"
  }

  case object GeneralGuarantee extends ReimbursementMethod {
    override def toString: String = "General Guarantee"
  }

  case object IndividualGuarantee extends ReimbursementMethod {
    override def toString: String = "Individual Guarantee"
  }

  case object Subsidy extends ReimbursementMethod {
    override def toString: String = "Subsidy"
  }

  lazy val values: Set[ReimbursementMethod] =
    Set(Deferment, BankTransfer, PayableOrder, GeneralGuarantee, IndividualGuarantee, Subsidy)

  def from(answer: ReimbursementMethodAnswer): ReimbursementMethod =
    answer match {
      case ReimbursementMethodAnswer.Subsidy                => Subsidy
      case ReimbursementMethodAnswer.CurrentMonthAdjustment => Deferment
      case ReimbursementMethodAnswer.BankAccountTransfer    => BankTransfer
    }

}
