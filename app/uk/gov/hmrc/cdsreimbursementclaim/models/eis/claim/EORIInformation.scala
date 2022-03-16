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

  def forConsignee(maybeConsigneeDetails: Option[ConsigneeDetails]): EORIInformation = {
    val maybeContactDetails = maybeConsigneeDetails.flatMap(_.contactDetails)

    val maybeTelephone    = maybeContactDetails.flatMap(_.telephone)
    val maybeEmailAddress = maybeContactDetails.flatMap(_.emailAddress)

    val maybeEstablishmentAddressLine1 = maybeConsigneeDetails.map(_.establishmentAddress.addressLine1)
    val maybeEstablishmentAddressLine2 = maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine2)
    val maybeEstablishmentAddressLine3 = maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine3)

    val maybeAddress1 = maybeContactDetails.flatMap(_.addressLine1)
    val maybeAddress2 = maybeContactDetails.flatMap(_.addressLine2)
    val maybeAddress3 = maybeContactDetails.flatMap(_.addressLine3)

    EORIInformation(
      EORINumber = maybeConsigneeDetails.map(_.EORI),
      CDSFullName = maybeConsigneeDetails.map(_.legalName),
      CDSEstablishmentAddress = Address(
        contactPerson = None,
        addressLine1 = maybeEstablishmentAddressLine1,
        addressLine2 = maybeEstablishmentAddressLine2,
        addressLine3 = maybeEstablishmentAddressLine3,
        street = Street.fromLines(maybeEstablishmentAddressLine1, maybeEstablishmentAddressLine2),
        city = maybeEstablishmentAddressLine3,
        countryCode = maybeConsigneeDetails
          .map(_.establishmentAddress.countryCode)
          .getOrElse(Country.uk.code),
        postalCode = maybeConsigneeDetails.flatMap(_.establishmentAddress.postalCode),
        telephoneNumber = maybeTelephone,
        emailAddress = maybeEmailAddress
      ),
      contactInformation = Some(
        ContactInformation(
          contactPerson = maybeContactDetails.flatMap(_.contactName),
          addressLine1 = maybeAddress1,
          addressLine2 = maybeAddress2,
          addressLine3 = maybeAddress3,
          street = Street.fromLines(maybeAddress1, maybeAddress2),
          city = maybeAddress3,
          countryCode = maybeContactDetails.flatMap(_.countryCode),
          postalCode = maybeContactDetails.flatMap(_.postalCode),
          telephoneNumber = maybeTelephone,
          faxNumber = None,
          emailAddress = maybeEmailAddress
        )
      )
    )
  }

  implicit val format: OFormat[EORIInformation] = Json.format[EORIInformation]
}
