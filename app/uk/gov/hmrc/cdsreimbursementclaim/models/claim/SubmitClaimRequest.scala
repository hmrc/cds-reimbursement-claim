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

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{Format, JsPath, Reads, Writes}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.SignedInUserDetails

import java.util.UUID

final case class SubmitClaimRequest[A](
  id: UUID,
  claim: A,
  signedInUserDetails: SignedInUserDetails
)

object SubmitClaimRequest {

  private def submitClaimRequestReads[A](implicit reads: Reads[A]): Reads[SubmitClaimRequest[A]] = (
    (JsPath \ "id").read[UUID] and
      (JsPath \ "claim").read[A] and
      (JsPath \ "signedInUserDetails").read[SignedInUserDetails]
  )(SubmitClaimRequest(_, _, _))

  private def submitClaimRequestWrites[A](implicit writes: Writes[A]): Writes[SubmitClaimRequest[A]] = (
    (JsPath \ "id").write[UUID] and
      (JsPath \ "claim").write[A] and
      (JsPath \ "signedInUserDetails").write[SignedInUserDetails]
  )(unlift(SubmitClaimRequest.unapply[A]))

  implicit def submitClaimRequestFormat[A](implicit format: Format[A]): Format[SubmitClaimRequest[A]] =
    Format(submitClaimRequestReads, submitClaimRequestWrites)
}
