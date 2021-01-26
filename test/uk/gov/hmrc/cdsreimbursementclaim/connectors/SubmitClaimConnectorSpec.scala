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

package uk.gov.hmrc.cdsreimbursementclaim.connectors

import play.api.libs.json.JsString
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.cdsreimbursementclaim.controllers.BaseSpec
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global

class SubmitClaimConnectorSpec extends BaseSpec with HttpSupport {

  val (eisBearerToken, eisEnvironment) = "token" -> "environment"

  val connector = new DefaultSubmitClaimConnector(mockHttp, appConfig)

  "SubmitClaimConnectorSpec" when {

    val backEndUrl                 = "http://localhost:7502/tpi/postoverpaymentclaim/v1"
    implicit val hc: HeaderCarrier = HeaderCarrier()

    "handling request to submit claim" must {

      "do a post http call and get the TPI-05 API response" in {
        val httpResponse = HttpResponse(200, "The Response")
        mockPost(backEndUrl, Seq.empty, *)(Right(httpResponse))
        val response     = await(connector.submitClaim(JsString("The Request")).value)
        response shouldBe Right(httpResponse)
      }
    }

    "return an error" when {
      "the call fails" in {
        val error    = new Exception("Socket connection error")
        mockPost(backEndUrl, Seq.empty, *)(Left(error))
        val response = await(connector.submitClaim(JsString("The Request")).value)
        response shouldBe Left(Error(error))
      }
    }

  }
}
