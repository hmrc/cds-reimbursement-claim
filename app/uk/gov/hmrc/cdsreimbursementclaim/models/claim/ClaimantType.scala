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

package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import cats.Eq
import play.api.libs.json.Format
import uk.gov.hmrc.cdsreimbursementclaim.utils.EnumerationFormat

sealed trait ClaimantType extends Product with Serializable

object ClaimantType {

  case object Consignee extends ClaimantType
  case object Declarant extends ClaimantType
  case object User extends ClaimantType

  private val mappings: Map[String, ClaimantType] =
    Seq(Consignee, Declarant, User)
      .map(item => (item.toString, item))
      .toMap

  implicit val format: Format[ClaimantType] = EnumerationFormat(mappings)

  implicit val equality: Eq[ClaimantType] = Eq.fromUniversalEquals[ClaimantType]
}
