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

import play.api.libs.json.Writes
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimantType, DeclarantTypeAnswer}
import uk.gov.hmrc.cdsreimbursementclaim.utils.EnumerationToStringWrites

sealed trait Claimant extends Product with Serializable

object Claimant {

  type PayeeIndicator = Claimant

  final case object Importer extends Claimant
  final case object Representative extends Claimant

  def of(declarantType: DeclarantTypeAnswer): Claimant =
    declarantType match {
      case DeclarantTypeAnswer.Importer                            => Importer
      case DeclarantTypeAnswer.AssociatedWithImporterCompany       => Representative
      case DeclarantTypeAnswer.AssociatedWithRepresentativeCompany => Representative
    }

  def of(claimantType: ClaimantType): Claimant =
    claimantType match {
      case ClaimantType.Consignee => Importer
      case ClaimantType.Declarant => Representative
      case ClaimantType.User      => Representative
    }

  implicit val writes: Writes[Claimant] = EnumerationToStringWrites[Claimant]
}
