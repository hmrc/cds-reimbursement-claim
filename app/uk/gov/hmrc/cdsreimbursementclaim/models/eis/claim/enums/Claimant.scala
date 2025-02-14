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

import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimantType, PayeeType}
import uk.gov.hmrc.cdsreimbursementclaim.utils.EnumerationFormat

sealed trait Claimant extends Product with Serializable

object Claimant extends EnumerationFormat[Claimant] {

  type PayeeIndicator = Claimant

  case object Importer extends Claimant
  case object Representative extends Claimant

  def basedOn(claimantType: ClaimantType): Claimant =
    claimantType match {
      case ClaimantType.Consignee => Importer
      case ClaimantType.Declarant => Representative
      case ClaimantType.User      => Representative
    }

  def basedOn(payeeType: PayeeType): Claimant =
    payeeType match {
      case PayeeType.Consignee => Importer
      case PayeeType.Declarant => Representative
    }

  lazy val values: Set[PayeeIndicator] = Set(Importer, Representative)
}
