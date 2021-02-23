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

final case class EntryDetails(
  entryNumber: Option[String],
  entryDate: Option[String],
  declarantReferenceNumber: Option[String],
  mainDeclarationReference: Option[Boolean],
  declarantDetails: Option[DeclarantDetails],
  accountDetails: Option[List[AccountDetails]],
  consigneeDetails: Option[DeclarantDetails],
  bankInfo: Option[BankInfo],
  NDRCDetails: Option[List[NdrcDetails]]
)

object EntryDetails {

  implicit val entryDetailsFormat: OFormat[EntryDetails] = Json.format

  def apply(declarationId: String): EntryDetails =
    EntryDetails(
      entryNumber = Some(declarationId),
      entryDate = None,
      declarantReferenceNumber = None,
      mainDeclarationReference = Some(true),
      declarantDetails = None,
      accountDetails = None,
      consigneeDetails = None,
      bankInfo = None,
      NDRCDetails = None
    )
}
