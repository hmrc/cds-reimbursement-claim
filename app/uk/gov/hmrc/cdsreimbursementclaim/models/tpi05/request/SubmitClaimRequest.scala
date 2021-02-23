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

package uk.gov.hmrc.cdsreimbursementclaim.models.tpi05.request

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

import play.api.libs.json.{Json, OFormat}

// These classes represent TPI05 API requests

final case class SubmitClaimRequest(
  postNewClaimsRequest: PostNewClaimsRequest
)

object SubmitClaimRequest {

  implicit val submitClaimRequestFormat: OFormat[SubmitClaimRequest] = Json.format

  def generateId: String               = UUID.randomUUID().toString.replaceAll("-", "").take(31)
  val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.systemDefault())

}
