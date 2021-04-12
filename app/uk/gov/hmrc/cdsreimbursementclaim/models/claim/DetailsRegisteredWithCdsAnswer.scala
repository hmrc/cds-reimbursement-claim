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

sealed trait DetailsRegisteredWithCdsAnswer extends Product with Serializable

object DetailsRegisteredWithCdsAnswer {

  final case class IncompleteDetailsRegisteredWithCdsAnswer(
    detailsRegisteredWithCds: Option[DetailsRegisteredWithCdsFormData]
  ) extends DetailsRegisteredWithCdsAnswer

  object IncompleteDetailsRegisteredWithCdsAnswer {
    val empty: IncompleteDetailsRegisteredWithCdsAnswer = IncompleteDetailsRegisteredWithCdsAnswer(None)

    implicit val format: OFormat[IncompleteDetailsRegisteredWithCdsAnswer] =
      derived.oformat[IncompleteDetailsRegisteredWithCdsAnswer]()
  }

  final case class CompleteDetailsRegisteredWithCdsAnswer(
    detailsRegisteredWithCds: DetailsRegisteredWithCdsFormData
  ) extends DetailsRegisteredWithCdsAnswer

  object CompleteDetailsRegisteredWithCdsAnswer {
    implicit val format: OFormat[CompleteDetailsRegisteredWithCdsAnswer] =
      derived.oformat[CompleteDetailsRegisteredWithCdsAnswer]()
  }

  implicit val format: OFormat[DetailsRegisteredWithCdsAnswer] = derived.oformat[DetailsRegisteredWithCdsAnswer]()
}
