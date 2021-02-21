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
import play.api.libs.json.OFormat
import uk.gov.hmrc.cdsreimbursementclaim.models.{EntryNumber, MRN}
import uk.gov.hmrc.cdsreimbursementclaim.models.EitherUtils._

import java.util.UUID

sealed trait CompleteClaim

object CompleteClaim {

  //TODO: complete definition
  final case class CompleteC285Claim(
    id: UUID,
    movementReferenceNumber: Either[EntryNumber, MRN],
    supportingEvidences: CompleteSupportingEvidenceAnswer
  ) extends CompleteClaim

  implicit class CompleteClaimOps(private val completeClaim: CompleteClaim) {

    def evidences: List[SupportingEvidence] = completeClaim match {
      case CompleteC285Claim(_, _, completeSupportingEvidenceAnswer) => completeSupportingEvidenceAnswer.evidences
    }

    def movementReferenceNumber: Either[EntryNumber, MRN] = completeClaim match {
      case CompleteC285Claim(_, movementReferenceNumber, _) => movementReferenceNumber
    }

    def correlationId: UUID = completeClaim match {
      case CompleteC285Claim(id, _, _) => id
    }

  }

  implicit val format: OFormat[CompleteClaim] = derived.oformat[CompleteClaim]()
}
