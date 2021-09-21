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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim

import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.{JsPath, Json, OWrites, Writes}

// The root structure for the JSON payload exceed 22 fields.
// Therefore the type needs to be split.
final case class RequestDetailA(
  CDFPayService: String,
  dateReceived: Option[String],
  claimType: Option[String],
  caseType: Option[String],
  customDeclarationType: Option[String],
  declarationMode: Option[String],
  claimDate: Option[String],
  claimAmountTotal: Option[String],
  disposalMethod: Option[String],
  reimbursementMethod: Option[String],
  basisOfClaim: Option[String],
  claimant: Option[String],
  payeeIndicator: Option[String],
  newEORI: Option[String],
  newDAN: Option[String],
  authorityTypeProvided: Option[String],
  claimantEORI: Option[String],
  claimantEmailAddress: Option[String],
  goodsDetails: Option[GoodsDetails],
  EORIDetails: Option[EoriDetails]
)

object RequestDetailA {
  implicit val format: OWrites[RequestDetailA] = Json.writes[RequestDetailA]
}

final case class RequestDetailB(
  MRNDetails: Option[List[MrnDetail]],
  duplicateMRNDetails: Option[MrnDetail]
)

object RequestDetailB {
  implicit val format: OWrites[RequestDetailB] = Json.writes[RequestDetailB]
}

final case class RequestDetail(
  requestDetailA: RequestDetailA,
  requestDetailB: RequestDetailB
)

object RequestDetail {
  implicit val format: Writes[RequestDetail] = (
    JsPath.write[RequestDetailA] and
      JsPath.write[RequestDetailB]
  )(unlift(RequestDetail.unapply))
}
