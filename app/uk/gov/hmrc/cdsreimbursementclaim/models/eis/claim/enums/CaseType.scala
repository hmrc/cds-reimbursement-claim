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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums

sealed trait CaseType extends Product with Serializable

object CaseType {
  case object Individual extends CaseType
  case object Bulk extends CaseType
  case object CMA extends CaseType

  implicit def caseTypeToString(caseType: CaseType): String = caseType match {
    case Individual => Individual.toString
    case Bulk       => Bulk.toString
    case CMA        => CMA.toString
  }

}
