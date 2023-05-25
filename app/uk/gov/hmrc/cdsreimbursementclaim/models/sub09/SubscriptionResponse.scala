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

package uk.gov.hmrc.cdsreimbursementclaim.models.sub09

import play.api.libs.json.Json
import play.api.libs.json.Format
import uk.gov.hmrc.cdsreimbursementclaim.utils.SimpleStringFormat
import play.api.libs.json.OFormat

final case class SubscriptionResponse(
  subscriptionDisplayResponse: SubscriptionDisplayResponse
)

final case class SubscriptionDisplayResponse(
  responseCommon: ResponseCommon,
  responseDetail: ResponseDetail
)

final case class CdsEstablishmentAddress(
  streetAndNumber: String,
  city: String,
  postalCode: Option[String],
  countryCode: String
)

final case class ContactInformation(
  personOfContact: Option[String],
  sepCorrAddrIndicator: Option[Boolean],
  streetAndNumber: Option[String],
  city: Option[String],
  postalCode: Option[String],
  countryCode: Option[String],
  telephoneNumber: Option[String],
  faxNumber: Option[String],
  emailAddress: Option[EmailAddress],
  emailVerificationTimestamp: Option[String]
)

final case class ResponseCommon(
  status: String,
  statusText: Option[String],
  processingDate: String,
  returnParameters: Option[Array[ReturnParameters]]
)

final case class ResponseDetail(
  EORINo: Option[EORI],
  EORIStartDate: Option[String],
  EORIEndDate: Option[String],
  CDSFullName: String,
  CDSEstablishmentAddress: CdsEstablishmentAddress,
  typeOfLegalEntity: Option[String],
  contactInformation: Option[ContactInformation],
  thirdCountryUniqueIdentificationNumber: Option[Array[String]],
  consentToDisclosureOfPersonalData: Option[String],
  shortName: Option[String],
  dateOfEstablishment: Option[String],
  typeOfPerson: Option[String],
  principalEconomicActivity: Option[String],
  ETMP_Master_Indicator: Boolean,
  XI_Subscription: Option[XiSubscription]
)

final case class ReturnParameters(paramName: String, paramValue: String)

final case class XiSubscription(
  XI_EORINo: String,
  PBEAddress: Option[PbeAddress],
  XI_VATNumber: Option[String],
  EU_VATNumber: Option[EUVATNumber],
  XI_ConsentToDisclose: Option[String],
  XI_SICCode: Option[String]
)

final case class PbeAddress(
  streetNumber1: String,
  streetNumber2: Option[String],
  city: Option[String],
  postalCode: Option[String],
  countryCode: Option[String]
)

final case class EUVATNumber(countryCode: Option[String], VATId: Option[String])

final case class EmailAddress(value: String)

final case class EORI(value: String)

@SuppressWarnings(Array("org.wartremover.warts.Any"))
object SubscriptionResponse {
  implicit val emailAddressFormat: Format[EmailAddress]                                = SimpleStringFormat(EmailAddress.apply, _.value)
  implicit val eoriFormat: Format[EORI]                                                = SimpleStringFormat(EORI.apply, _.value)
  implicit val pbeAddressFormat: OFormat[PbeAddress]                                   = Json.format[PbeAddress]
  implicit val euVatFormat: OFormat[EUVATNumber]                                       = Json.format[EUVATNumber]
  implicit val xiSubscriptionFormat: OFormat[XiSubscription]                           = Json.format[XiSubscription]
  implicit val returnParametersFormat: OFormat[ReturnParameters]                       = Json.format[ReturnParameters]
  implicit val contactInformationFormat: OFormat[ContactInformation]                   = Json.format[ContactInformation]
  implicit val cdsEstablishmentAddressFormat: OFormat[CdsEstablishmentAddress]         = Json.format[CdsEstablishmentAddress]
  implicit val responseDetailFormat: OFormat[ResponseDetail]                           = Json.format[ResponseDetail]
  implicit val responseCommonFormat: OFormat[ResponseCommon]                           = Json.format[ResponseCommon]
  implicit val subscriptionDisplayResponseFormat: OFormat[SubscriptionDisplayResponse] =
    Json.format[SubscriptionDisplayResponse]
  implicit val responseSubscriptionFormat: OFormat[SubscriptionResponse]               = Json.format[SubscriptionResponse]
}
