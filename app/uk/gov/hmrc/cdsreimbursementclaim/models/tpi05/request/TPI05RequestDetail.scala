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

import play.api.libs.json.{Json, OFormat}

final case class TPI05RequestDetail(
  CDFPayService: Option[String],
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
  EORIDetails: Option[EoriDetails],
  MRNDetails: Option[List[MrnDetails]],
  //DuplicateMRNDetails: Option[MrnDetails],
  entryDetails: Option[List[EntryDetails]]
  //duplicateEntryDetails: Option[EntryDetails]
)

object TPI05RequestDetail {
  implicit val requestDetailFormat: OFormat[TPI05RequestDetail] = Json.format

  def apply(claimantEORI: String): TPI05RequestDetail =
    TPI05RequestDetail(
      CDFPayService = Some("NDRC"),
      dateReceived = None,
      claimType = None,
      caseType = None,
      customDeclarationType = None,
      declarationMode = None,
      claimDate = None,
      claimAmountTotal = None,
      disposalMethod = None,
      reimbursementMethod = None,
      basisOfClaim = None,
      claimant = None,
      payeeIndicator = None,
      newEORI = None,
      newDAN = None,
      authorityTypeProvided = None,
      claimantEORI = Some(claimantEORI),
      claimantEmailAddress = None,
      goodsDetails = None,
      EORIDetails = None,
      MRNDetails = None,
      entryDetails = None
    )
}
