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

import java.time.Instant
import java.util.UUID

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.cdsreimbursementclaim.utils.TimeUtils._

final case class GetDeclarationRequest(overpaymentDeclarationDisplayRequest: OverpaymentDeclarationDisplayRequest)
object GetDeclarationRequest {

  def apply(declarationId: MRN): GetDeclarationRequest =
    GetDeclarationRequest(
      OverpaymentDeclarationDisplayRequest(
        RequestCommon("MDTP", RequestDate(), generateAcknoledgementId),
        RequestDetail(declarationId, None)
      )
    )

  def generateAcknoledgementId: String = UUID.randomUUID().toString.replaceAll("-", "").take(31)

  implicit val requestDateWrites: Writes[RequestDate]                                                  = Json.valueWrites
  implicit val requestCommonWrites: Writes[RequestCommon]                                              = Json.writes
  implicit val requestDetailWrites: Writes[RequestDetail]                                              = Json.writes
  implicit val overpaymentDeclarationDisplayRequestReads: Writes[OverpaymentDeclarationDisplayRequest] = Json.writes
  implicit val getDeclarationRequestWrites: Writes[GetDeclarationRequest]                              = Json.writes
}

final case class OverpaymentDeclarationDisplayRequest(
  requestCommon: RequestCommon,
  requestDetail: RequestDetail
)

final case class RequestCommon(
  originatingSystem: String,
  receiptDate: RequestDate,
  acknowledgementReference: String
)

final case class RequestDetail(
  declarationId: MRN,
  securityReason: Option[String]
)

final case class RequestDate(value: String) extends AnyVal

object RequestDate {
  def apply(): RequestDate = RequestDate(eisDateFormat.format(Instant.now()))
}
