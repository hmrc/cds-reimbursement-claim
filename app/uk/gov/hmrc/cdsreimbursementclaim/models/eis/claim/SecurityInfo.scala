/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.SecurityDetail
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.securities.{DeclarantReferenceNumber, DeclarationId, ProcedureCode}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.EisBasicDate
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.BtaSource

final case class SecurityInfo(
  dateClaimReceived: Option[EisBasicDate] = None,
  reasonForSecurity: Option[String] = None,
  declarationID: Option[DeclarationId] = None,
  procedureCode: Option[ProcedureCode] = None,
  acceptanceDate: Option[EisBasicDate] = None,
  declarantReferenceNumber: Option[DeclarantReferenceNumber] = None,
  BTASource: Option[BtaSource] = None,
  BTADueDate: Option[EisBasicDate] = None,
  declarantDetails: Option[MRNInformation] = None,
  consigneeDetails: Option[MRNInformation] = None,
  accountDetails: Option[List[AccountDetail]] = None,
  bankDetails: BankDetails = BankDetails(None, None),
  securityDetails: Option[List[SecurityDetail]] = None
)

object SecurityInfo {
  implicit val format: OFormat[SecurityInfo] = Json.format[SecurityInfo]
}
