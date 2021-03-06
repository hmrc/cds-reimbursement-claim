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

package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import play.api.libs.json.{Json, OFormat}

final case class Declaration(
  declarantId: String,
  acceptanceDate: String,
  declarantDetails: DeclarantDetails,
  consigneeDetails: Option[ConsigneeDetails],
  maskedBankDetails: Option[MaskedBankDetails],
  securityDetails: Option[List[SecurityDetails]],
  ndrcDetails: Option[List[NdrcDetails]]
)

object Declaration {

  implicit class DeclarationOps(declaration: Declaration) {

    def totalPaidCharges: BigDecimal =
      BigDecimal(
        declaration.ndrcDetails.fold(0.0)(ndrcDetails => ndrcDetails.map(s => s.amount.toDouble).sum)
      )

    def consigneeName: Option[String] = declaration.consigneeDetails.map(details => details.legalName)

    def consigneeEmail: Option[String] =
      declaration.consigneeDetails.flatMap(details => details.contactDetails.flatMap(f => f.emailAddress))

    def consigneeTelephone: Option[String] =
      declaration.consigneeDetails.flatMap(details => details.contactDetails.flatMap(f => f.telephone))

    def consigneeAddress: Option[String] =
      declaration.consigneeDetails.map(details => establishmentAddress(details.establishmentAddress).mkString(", "))

    def declarantName: String = declaration.declarantDetails.legalName

    def declarantEmailAddress: Option[String] =
      declaration.declarantDetails.contactDetails.flatMap(details => details.emailAddress)

    def declarantTelephoneNumber: Option[String] =
      declaration.declarantDetails.contactDetails.flatMap(details => details.telephone)

    def declarantContactAddress: Option[String] =
      declaration.declarantDetails.contactDetails.map(details => declarantAddress(details).mkString(", "))

    def declarantAddress(contactDetails: ContactDetails): List[String] = {
      val lines = List(
        contactDetails.addressLine1,
        contactDetails.addressLine2,
        contactDetails.addressLine3,
        contactDetails.addressLine4,
        contactDetails.postalCode,
        contactDetails.countryCode
      )
      lines.collect { case Some(s) => s }
    }

    def establishmentAddress(establishmentAddress: EstablishmentAddress): List[String] = {
      val lines = List(
        Some(establishmentAddress.addressLine1),
        establishmentAddress.addressLine2,
        establishmentAddress.addressLine3,
        establishmentAddress.postalCode,
        Some(establishmentAddress.countryCode)
      )
      lines.collect { case Some(s) => s }
    }
  }

  implicit val format: OFormat[Declaration] = Json.format[Declaration]
}
