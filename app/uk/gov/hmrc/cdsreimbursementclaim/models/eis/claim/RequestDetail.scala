/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant.PayeeIndicator
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums._
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori
import uk.gov.hmrc.cdsreimbursementclaim.models.CDFPayService

final case class RequestDetail(
  CDFPayService: CDFPayService,
  dateReceived: Option[String] = None,
  claimType: Option[ClaimType] = None,
  caseType: Option[CaseType] = None,
  customDeclarationType: Option[CustomDeclarationType] = None,
  declarationMode: Option[DeclarationMode] = None,
  claimDate: Option[String] = None,
  claimAmountTotal: Option[String] = None,
  disposalMethod: Option[String] = None,
  useExistingPaymentMethod: Option[Boolean] = None,
  reimbursementMethod: Option[ReimbursementMethod] = None,
  reimbursementParty: Option[ReimbursementParty] = None,
  basisOfClaim: Option[String] = None,
  claimant: Option[Claimant] = None,
  payeeIndicator: Option[PayeeIndicator] = None,
  newEORI: Option[Eori] = None,
  newDAN: Option[String] = None,
  authorityTypeProvided: Option[String] = None,
  claimantEORI: Eori,
  claimantEmailAddress: Email,
  claimantName: Option[String] = None,
  goodsDetails: Option[GoodsDetails] = None,
  EORIDetails: Option[EoriDetails] = None,
  MRNDetails: Option[List[MrnDetail]] = None,
  duplicateMRNDetails: Option[MrnDetail] = None,
  methodOfDisposals: Option[List[TemporaryAdmissionMethodOfDisposalDetail]] = None,
  security: Option[SecurityInfo] = None,
  claimantAddress: Option[Address] = None
)

object RequestDetail {
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit lazy val jsonFormat: OFormat[RequestDetail] = Json.format[RequestDetail]
}
