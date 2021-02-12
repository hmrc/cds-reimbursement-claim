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

package uk.gov.hmrc.cdsreimbursementclaim.models

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging._

final case class WorkItemHeaders(requestId: Option[String], sessionId: Option[String]) {
  def toHeaderCarrier: HeaderCarrier =
    HeaderCarrier(requestId = requestId.map(RequestId), sessionId = sessionId.map(SessionId))
}

object WorkItemHeaders {
  implicit val formats: Format[WorkItemHeaders] = Json.format[WorkItemHeaders]
  def apply(hc: HeaderCarrier): WorkItemHeaders =
    new WorkItemHeaders(hc.requestId.map(_.value), hc.sessionId.map(_.value))
}

final case class WorkItemPayload(soapRequest: String, headers: WorkItemHeaders)

object WorkItemPayload {
  implicit val workItemPayloadFormat: Format[WorkItemPayload] = Json.format[WorkItemPayload]
}

final case class WorkItemResult(payload: WorkItemPayload, response: Either[Error, Unit])
