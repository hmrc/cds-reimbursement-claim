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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{Format, JsPath, Reads, Writes}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ClaimantInformation
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.{Address, MRNInformation}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error => CdsError}

final case class ConsigneeDetails(
  EORI: Eori,
  legalName: String,
  establishmentAddress: EstablishmentAddress,
  contactDetails: Option[ContactDetails]
) extends ClaimantDetails

object ConsigneeDetails {

  def fromClaimantInformation(claimantInformation: ClaimantInformation): Either[CdsError, MRNInformation] =
    Address
      .fromContactInformation(claimantInformation.establishmentAddress)
      .map { address =>
        MRNInformation(
          claimantInformation.eori,
          claimantInformation.fullName,
          address,
          claimantInformation.contactInformation
        )
      }

  private val reads: Reads[ConsigneeDetails] = (
    (JsPath \ "consigneeEORI").read[Eori] and
      (JsPath \ "legalName").read[String] and
      (JsPath \ "establishmentAddress").read[EstablishmentAddress] and
      (JsPath \ "contactDetails").readNullable[ContactDetails]
  )(ConsigneeDetails(_, _, _, _))

  private val writes: Writes[ConsigneeDetails] = (
    (JsPath \ "consigneeEORI").write[Eori] and
      (JsPath \ "legalName").write[String] and
      (JsPath \ "establishmentAddress").write[EstablishmentAddress] and
      (JsPath \ "contactDetails").writeNullable[ContactDetails]
  )(unlift(ConsigneeDetails.unapply))

  implicit val format: Format[ConsigneeDetails] = Format(reads, writes)
}
