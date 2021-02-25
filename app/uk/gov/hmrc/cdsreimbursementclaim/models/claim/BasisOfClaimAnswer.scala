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

import julienrf.json.derived
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.BasisOfClaim

sealed trait BasisOfClaimAnswer extends Product with Serializable

object BasisOfClaimAnswer {

  final case class IncompleteBasisOfClaimAnswer(
    maybeBasisOfClaim: Option[BasisOfClaim]
  ) extends BasisOfClaimAnswer

  object IncompleteBasisOfClaimAnswer {
    val empty: IncompleteBasisOfClaimAnswer                    = IncompleteBasisOfClaimAnswer(None)
    implicit val format: OFormat[IncompleteBasisOfClaimAnswer] = Json.format[IncompleteBasisOfClaimAnswer]
  }

  final case class CompleteBasisOfClaimAnswer(
    basisOfClaim: BasisOfClaim
  ) extends BasisOfClaimAnswer

  object CompleteBasisOfClaimAnswer {
    implicit val format: OFormat[CompleteBasisOfClaimAnswer] = Json.format[CompleteBasisOfClaimAnswer]
  }

  implicit val format: OFormat[BasisOfClaimAnswer] = derived.oformat()
}
