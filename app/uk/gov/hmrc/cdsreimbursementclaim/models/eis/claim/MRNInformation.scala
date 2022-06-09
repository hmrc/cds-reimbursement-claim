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
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.{ConsigneeDetails, DeclarantDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori

final case class MRNInformation(
  EORI: Eori,
  legalName: String,
  establishmentAddress: Address,
  contactDetails: ContactInformation
)

object MRNInformation {
  def fromDeclarantDetails(declarantDetails: DeclarantDetails): MRNInformation =
    MRNInformation(
      declarantDetails.EORI,
      legalName = declarantDetails.legalName,
      establishmentAddress = Address
        .fromEstablishmentAddress(declarantDetails.establishmentAddress)
        .copy(contactPerson = declarantDetails.contactDetails.flatMap(_.contactName)),
      contactDetails = ContactInformation.fromDeclarantDetails(declarantDetails)
    )

  def fromConsigneeDetails(consigneeDetails: ConsigneeDetails): MRNInformation =
    MRNInformation(
      consigneeDetails.EORI,
      consigneeDetails.legalName,
      Address.fromEstablishmentAddress(consigneeDetails.establishmentAddress),
      ContactInformation.fromConsigneeDetails(consigneeDetails)
    )

  implicit val format: OFormat[MRNInformation] = Json.format[MRNInformation]
}
