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

import play.api.libs.json.{Json, OWrites}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.MethodOfDisposal
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums._

final case class RequestDetail(
  CDFPayService: CDFPayService, // ok
  dateReceived: Option[String] = None, // ok
  claimType: Option[ClaimType] = None, // ok
  caseType: Option[CaseType] = None, // ok
  customDeclarationType: Option[CustomDeclarationType] = None, // ok
  declarationMode: Option[DeclarationMode] = None, // ok
  claimDate: Option[String] = None, // ok
  claimAmountTotal: Option[BigDecimal] = None, // ok
  disposalMethod: Option[MethodOfDisposal] = None, // ok
  reimbursementMethod: Option[ReimbursementMethod] = None, // ok
  basisOfClaim: Option[String] = None, // ok
  claimant: Option[String] = None, // ok
  payeeIndicator: Option[String] = None, // ok
  newEORI: Option[String] = None, // ok
  newDAN: Option[String] = None, // ok
  authorityTypeProvided: Option[String] = None, // ok
  claimantEORI: Option[String] = None,
  claimantEmailAddress: Option[String] = None,
  goodsDetails: Option[GoodsDetails] = None,
  EORIDetails: Option[EoriDetails] = None,
  MRNDetails: Option[List[MrnDetail]] = None,
  duplicateMRNDetails: Option[MrnDetail] = None
)

object RequestDetail {

  implicit val writes: OWrites[RequestDetail] = Json.writes[RequestDetail]
}