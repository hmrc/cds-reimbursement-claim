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

import cats.Eq
import play.api.libs.json.Writes
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DeclarantTypeAnswer
import uk.gov.hmrc.cdsreimbursementclaim.utils.WriteEnumerationToString

sealed trait YesNo extends Product with Serializable

object YesNo {

  final case object No extends YesNo
  final case object Yes extends YesNo

  def whetherFrom(declarantTypeAnswer: DeclarantTypeAnswer): YesNo =
    declarantTypeAnswer match {
      case DeclarantTypeAnswer.Importer => Yes
      case _                            => No
    }

  implicit val equality: Eq[YesNo] = Eq.fromUniversalEquals[YesNo]

  implicit val writes: Writes[YesNo] = WriteEnumerationToString[YesNo]
}
