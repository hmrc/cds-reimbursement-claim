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

final case class RequestDetailA(
  CDFPayService: String,
  dateReceived: Option[String] = None,
  claimType: Option[String] = None,
  caseType: Option[String] = None,
  customDeclarationType: Option[String] = None,
  declarationMode: Option[String] = None,
  claimDate: Option[String] = None,
  claimAmountTotal: Option[String] = None,
  disposalMethod: Option[String] = None,
  reimbursementMethod: Option[String] = None,
  basisOfClaim: Option[String] = None,
  claimant: Option[String] = None,
  payeeIndicator: Option[String] = None,
  newEORI: Option[String] = None,
  newDAN: Option[String] = None,
  authorityTypeProvided: Option[String] = None,
  claimantEORI: Option[String] = None,
  claimantEmailAddress: Option[String] = None,
  goodsDetails: Option[GoodsDetails] = None,
  EORIDetails: Option[EoriDetails] = None
)

object RequestDetailA {
  implicit val format: OWrites[RequestDetailA] = Json.writes[RequestDetailA]
}

final case class RequestDetailB(
  MRNDetails: Option[List[MrnDetail]] = None,
  duplicateMRNDetails: Option[MrnDetail] = None
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
