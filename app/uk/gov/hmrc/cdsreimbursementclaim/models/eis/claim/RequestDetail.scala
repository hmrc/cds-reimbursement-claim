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
import play.api.libs.json.{Format, JsPath, Json, OFormat}

// The root structure for the JSON payload exceed 22 fields. Therefore the type needs to be split.
final case class RequestDetailA(
  CDFPayservice: String,
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
  ClaimantEORI: Option[String],
  claimantEmailAddress: Option[String],
  goodsDetails: Option[GoodsDetails],
  EORIDetails: Option[EORIDetails]
)

object RequestDetailA {
  implicit val format: OFormat[RequestDetailA] = Json.format[RequestDetailA]
}

final case class RequestDetailB(
  MRNDetails: Option[List[MRNDetail]],
  duplicateMRNDetails: Option[MRNDetail],
  entryDetails: Option[List[EntryDetail]],
  duplicateEntryDetails: Option[EntryDetail]
)

object RequestDetailB {
  implicit val format: OFormat[RequestDetailB] = Json.format[RequestDetailB]
}

final case class RequestDetail(
  requestDetailA: RequestDetailA,
  requestDetailB: RequestDetailB
)

object RequestDetail {
  implicit val format: Format[RequestDetail] = (
    JsPath.format[RequestDetailA] and
      JsPath.format[RequestDetailB]
  )(RequestDetail.apply, unlift(RequestDetail.unapply))
}
