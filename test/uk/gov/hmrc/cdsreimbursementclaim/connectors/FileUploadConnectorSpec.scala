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

import org.scalamock.matchers.ArgCapture.CaptureOne
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.cdsreimbursementclaim.controllers.BaseSpec
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global

class FileUploadConnectorSpec extends BaseSpec with HttpSupport {

  val connector                  = new DefaultFileUploadConnector(mockHttp, appConfig)
  val backEndUrl                 = "http://localhost:7502/filetransfer/init/1.0.0"
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "FileUploadConnector" when {

    "handling request for declaration" must {

      "do a post http call to the DEC64 api and get a response" in {
        val httpResponse = HttpResponse(200, "The Response")
        val capturedHc   = CaptureOne[HeaderCarrier]()
        mockPostString(backEndUrl, Some(capturedHc))(Right(httpResponse))
        val response     = await(connector.upload("<file>upload</file>").value)
        response                                                          shouldBe Right(httpResponse)
        capturedHc.value.authorization                                    shouldBe Some(Authorization("Bearer test-token"))
        capturedHc.value.extraHeaders                                       should contain("X-Forwarded-Host" -> "MDTP")
        capturedHc.value.extraHeaders                                       should contain("Content-Type" -> "application/xml; charset=UTF-8")
        capturedHc.value.extraHeaders                                       should contain("Accept" -> "application/xml")
        capturedHc.value.extraHeaders.exists(_._1 === "Date")             shouldBe true
        capturedHc.value.extraHeaders.exists(_._1 === "X-Correlation-ID") shouldBe true
      }
    }

    "return an error" when {
      "the call fails" in {
        val error    = new Exception("Socket connection error")
        mockPostString(backEndUrl)(Left(error))
        val response = await(connector.upload("<file>upload</file>").value)
        response shouldBe Left(Error(error))
      }
    }

  }
}
