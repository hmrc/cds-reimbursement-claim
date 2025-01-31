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

package uk.gov.hmrc.cdsreimbursementclaim.connectors

import com.typesafe.config.ConfigFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json
import play.api.test.Helpers.*
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.http.CustomHeaderNames
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.EisSubmitClaimRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TPI05RequestGen.*
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global

@SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext"))
class ClaimConnectorSpec extends AnyWordSpec with Matchers with MockFactory with HttpV2Support {

  val eisBearerToken = "token"

  val config: Configuration = Configuration(
    ConfigFactory.parseString(
      """
        | self {
        |   url = host1.com
        |  },
        |  microservice {
        |    services {
        |      claim {
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

  val connector = new DefaultClaimConnector(mockHttp, new ServicesConfig(config)) {
    override def getExtraHeaders(implicit hc: HeaderCarrier): Seq[(String, String)] =
      Seq(
        HeaderNames.DATE                   -> "some-date",
        CustomHeaderNames.X_CORRELATION_ID -> "some-correlation-id",
        HeaderNames.X_FORWARDED_HOST       -> Platform.MDTP,
        HeaderNames.CONTENT_TYPE           -> MimeTypes.JSON,
        HeaderNames.ACCEPT                 -> MimeTypes.JSON
      )
  }

  val explicitHeaders = Seq(
    "Date"             -> "some-date",
    "X-Correlation-ID" -> "some-correlation-id",
    "X-Forwarded-Host" -> "MDTP",
    "Content-Type"     -> "application/json",
    "Accept"           -> "application/json",
    "Authorization"    -> "Bearer test-token"
  )

  "Claim Connector" when {

    val backEndUrl                 = "http://localhost:7502/tpi/postoverpaymentclaim/v1"
    implicit val hc: HeaderCarrier = HeaderCarrier()

    "handling request to submit claim" must {

      "do a post http call and get the TPI-05 API response" in {
        val request      = sample[EisSubmitClaimRequest]
        val httpResponse = HttpResponse(200, "The Response")

        mockHttpPostSuccess[HttpResponse](backEndUrl, Json.toJson(request), httpResponse)

        val response = await(connector.submitClaim(request).value)
        response shouldBe Right(httpResponse)
      }
    }

    "return an error" when {
      "the call fails" in {
        val request = sample[EisSubmitClaimRequest]
        mockHttpPostWithException(backEndUrl, Json.toJson(request), new NotFoundException("not found"))
        await(connector.submitClaim(request).value).isLeft shouldBe true
      }
    }

    "Have the correct Headers" in {
      val connector = new DefaultClaimConnector(mockHttp, new ServicesConfig(config))
      connector.getExtraHeaders.find(_._1 === HeaderNames.CONTENT_TYPE).getOrElse(fail())._2 shouldBe MimeTypes.JSON
    }

  }
}
