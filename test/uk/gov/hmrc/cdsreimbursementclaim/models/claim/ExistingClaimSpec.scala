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

package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class ExistingClaimSpec extends AnyWordSpec with Matchers {

  val sampleTrueResponse = Json.parse("""{
                         |    "getExistingClaimResponse": {
                         |        "responseCommon": {
                         |            "status": "OK",
                         |            "processingDate": "2022-06-21T10:35:18Z",
                         |            "CDFPayService": "SCTY",
                         |            "CDFPayCaseFound": true,
                         |            "goods": {
                         |                "itemNumber": "0001",
                         |                "goodsDescription": "Something expensive"
                         |            },
                         |            "totalClaimAmount": "1000.00",
                         |            "CDFPayCaseNumber": "SEC-1234",
                         |            "caseStatus": "Open"
                         |        }
                         |    }
                         |}""".stripMargin)

  val sampleFalseResponse = Json.parse("""{
                                      |    "getExistingClaimResponse": {
                                      |        "responseCommon": {
                                      |            "status": "OK",
                                      |            "processingDate": "2022-06-21T10:38:24Z",
                                      |            "CDFPayService": "SCTY",
                                      |            "CDFPayCaseFound": false,
                                      |            "goods": {
                                      |                "itemNumber": "0001",
                                      |                "goodsDescription": "Something expensive"
                                      |            },
                                      |            "totalClaimAmount": "1000.00"
                                      |        }
                                      |    }
                                      |}""".stripMargin)

  "Existing Claim" should {
    "Extract an existing claim from a true response" in {
      sampleTrueResponse.as[ExistingClaim] shouldBe ExistingClaim(true)
    }

    "Extract a non-existing claim from a false response" in {
      sampleFalseResponse.as[ExistingClaim] shouldBe ExistingClaim(false)
    }
  }
}
