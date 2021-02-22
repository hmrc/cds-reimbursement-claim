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

import com.typesafe.config.ConfigFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.cdsreimbursementclaim.models.ccs.CcsSubmissionPayload
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.CcsSubmissionGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global

class CcsConnectorSpec extends AnyWordSpec with Matchers with MockFactory with HttpSupport {

  "Claim Connector" when {

    val config: Configuration = Configuration(
      ConfigFactory.parseString(
        """
          | self {
          |   url = host1.com
          |  },
          |  microservice {
          |    services {
          |      ccs {
          |        protocol = http
          |        host     = localhost
          |        port     = 7502
          |      }
          |   }
          |}
          |eis {
          |    bearer-token = "test-token"
          |}
          |
          |""".stripMargin
      )
    )

    val connector = new DefaultCcsConnector(mockHttp, new ServicesConfig(config))

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val backEndUrl = "http://localhost:7502/filetransfer/init/v1"

    "handling request to submit a file" must {

      "do a post http call and get the DEC-64 API response" in {
        val request      = sample[CcsSubmissionPayload]
        val httpResponse = HttpResponse(204, "success response")
        mockPostString(backEndUrl, request.headers, request.dec64Body)(Some(httpResponse))
        val response     = await(connector.submitToCcs(request).value)
        response shouldBe Right(httpResponse)
      }
    }

    "return an error" when {
      "the call fails" in {
        val request = sample[CcsSubmissionPayload]
        mockPostString(backEndUrl, request.headers, request.dec64Body)(None)
        await(connector.submitToCcs(request).value).isLeft shouldBe true
      }
    }

  }
}
