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

import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ReimbursementMethodAnswer.CurrentMonthAdjustment
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ReimbursementMethodAnswer, TypeOfClaimAnswer}
import uk.gov.hmrc.cdsreimbursementclaim.utils.EnumerationFormat

sealed trait CaseType extends Product with Serializable

object CaseType extends EnumerationFormat[CaseType] {

  case object Individual extends CaseType
  case object Bulk extends CaseType
  case object CMA extends CaseType

  lazy val values: Set[CaseType] = Set(Individual, Bulk, CMA)

  def basedOn(typeOfClaim: TypeOfClaimAnswer, reimbursementMethod: ReimbursementMethodAnswer): CaseType =
    (typeOfClaim, reimbursementMethod) match {
      case (TypeOfClaimAnswer.Multiple, _)  => CaseType.Bulk
      case (TypeOfClaimAnswer.Scheduled, _) => CaseType.Bulk
      case (_, CurrentMonthAdjustment)      => CaseType.CMA
      case _                                => CaseType.Individual
    }
}
