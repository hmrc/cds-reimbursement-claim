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

import play.api.libs.json.{Json, OWrites}
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error => CdsError}
import uk.gov.hmrc.cdsreimbursementclaim.services.tpi05.ClaimToTPI05Mapper

import scala.annotation.implicitNotFound

final case class EisSubmitClaimRequest(
  postNewClaimsRequest: PostNewClaimsRequest
)

object EisSubmitClaimRequest {

  @implicitNotFound("No implicit TPI05 mapper found for request object")
  def apply[A](source: A)(implicit mapper: ClaimToTPI05Mapper[A]): Either[CdsError, EisSubmitClaimRequest] =
    mapper.map(source)

  implicit val format: OWrites[EisSubmitClaimRequest] = Json.writes[EisSubmitClaimRequest]
}
