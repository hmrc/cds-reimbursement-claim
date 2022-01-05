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
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{Country, Street}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.ConsigneeDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori

final case class EORIInformation(
  EORINumber: Option[Eori],
  CDSFullName: Option[String],
  CDSEstablishmentAddress: Address,
  contactInformation: Option[ContactInformation],
  legalEntityType: Option[String] = None,
  EORIStartDate: Option[String] = None,
  VATDetails: Option[List[VATDetail]] = None
)

object EORIInformation {

  def forConsignee(maybeConsigneeDetails: Option[ConsigneeDetails]): EORIInformation =
    EORIInformation(
      EORINumber = maybeConsigneeDetails.map(_.EORI),
      CDSFullName = maybeConsigneeDetails.map(_.legalName),
      CDSEstablishmentAddress = Address(
        contactPerson = None,
        addressLine1 = maybeConsigneeDetails.map(_.establishmentAddress.addressLine1),
        addressLine2 = maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine2),
        AddressLine3 = maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine3),
        street = Street.fromLines(
          maybeConsigneeDetails.map(_.establishmentAddress.addressLine1),
          maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine2)
        ),
        city = maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine3),
        countryCode = maybeConsigneeDetails
          .map(_.establishmentAddress.countryCode)
          .getOrElse(Country.uk.code),
        postalCode = maybeConsigneeDetails.flatMap(_.establishmentAddress.postalCode),
        telephone = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.telephone)),
        emailAddress = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.emailAddress))
      ),
      contactInformation = Some(
        ContactInformation(
          contactPerson = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.contactName)),
          addressLine1 = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine1)),
          addressLine2 = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine2)),
          addressLine3 = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine3)),
          street = Street.fromLines(
            maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine1)),
            maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine2))
          ),
          city = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine3)),
          countryCode = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.countryCode)),
          postalCode = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.postalCode)),
          telephoneNumber = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.telephone)),
          faxNumber = None,
          emailAddress = maybeConsigneeDetails.flatMap(_.contactDetails.flatMap(_.emailAddress))
        )
      )
    )

  implicit val format: OFormat[EORIInformation] = Json.format[EORIInformation]
}
