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

package uk.gov.hmrc.cdsreimbursementclaim.models.generators

import org.scalacheck.Gen
import uk.gov.hmrc.cdsreimbursementclaim.models.sub09.SubscriptionResponse
import IdGen.genEori
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori
import play.api.libs.json.Json

object Sub09ReponseGen {

  val genSubscriptionWithXiEori: Gen[(SubscriptionResponse, Eori, Eori)] =
    for {
      eoriGB       <- genEori
      eoriXI       <- genEori
      subscription <- Gen.const(subscription(eoriGB, Some(eoriXI)))
    } yield (subscription, eoriGB, eoriXI)

  val genSubscriptionWithoutXiEori: Gen[(SubscriptionResponse, Eori)] =
    for {
      eoriGB       <- genEori
      subscription <- Gen.const(subscription(eoriGB, None))
    } yield (subscription, eoriGB)

  def subscription(eoriGB: Eori, eoriXI: Option[Eori]): SubscriptionResponse =
    Json
      .parse(s"""|{
                |  "subscriptionDisplayResponse": {
                |    "responseCommon": {
                |      "status": "OK",
                |      "statusText": "Optional status text from ETMP",
                |      "processingDate": "2016-08-17T19:33:47Z",
                |      "returnParameters": [
                |        {
                |          "paramName": "POSITION",
                |          "paramValue": "LINK"
                |        }
                |      ]
                |    },
                |    "responseDetail": {
                |      "EORINo": "${eoriGB.value}",
                |      "EORIStartDate": "1999-01-01",
                |      "EORIEndDate": "2020-01-01",
                |      "CDSFullName": "Tony Stark",
                |      "CDSEstablishmentAddress": {
                |        "streetAndNumber": "86 Mysore Road",
                |        "city": "London",
                |        "postalCode": "SW11 5RZ",
                |        "countryCode": "GB"
                |      },
                |      "typeOfLegalEntity": "0001",
                |      "contactInformation": {
                |        "personOfContact": "Pepper Pott",
                |        "streetAndNumber": "2nd floor, Alexander House",
                |        "city": "Southend-on-sea",
                |        "postalCode": "SS99 1AA",
                |        "countryCode": "GB",
                |        "telephoneNumber": "01702215001",
                |        "faxNumber": "01702215002",
                |        "emailAddress": "someemail@mail.com"
                |      },
                |      "thirdCountryUniqueIdentificationNumber": [
                |        "321",
                |        "222"
                |      ],
                |      "consentToDisclosureOfPersonalData": "1",
                |      "shortName": "Robinson",
                |      "dateOfEstablishment": "1963-04-01",
                |      "typeOfPerson": "1",
                |      "principalEconomicActivity": "2000",
                |      "ETMP_Master_Indicator": true${eoriXI.fold("")(XI_Subscription)}
                |    }
                |  }
                |}""".stripMargin)
      .as[SubscriptionResponse]

  private def XI_Subscription(eori: Eori): String =
    s"""|,
           |      "XI_Subscription": {
           |        "XI_EORINo": "${eori.value}",
           |        "PBEAddress": {
           |          "streetNumber1": "address line 1",
           |          "streetNumber2": "address line 2",
           |          "city": "city 1"
           |        },
           |        "XI_VATNumber": "GB123456789",
           |        "EU_VATNumber": {
           |          "countryCode": "GB",
           |          "VATId": "123456891012"
           |        }
           |      }""".stripMargin

}
