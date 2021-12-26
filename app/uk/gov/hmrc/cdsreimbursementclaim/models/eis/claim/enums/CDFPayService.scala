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
import uk.gov.hmrc.cdsreimbursementclaim.utils.WriteEnumerationToString

sealed trait CDFPayService extends Product with Serializable

object CDFPayService {

  final case object NDRC extends CDFPayService
  final case object SCTY extends CDFPayService

  lazy val values: Set[CDFPayService] = Set(NDRC, CDFPayService.SCTY)

  implicit val writes: Writes[CDFPayService] = WriteEnumerationToString[CDFPayService]
}
