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
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.request.DeclarationRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Acc14DeclarationGen.*
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global

@SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext"))
class DeclarationConnectorSpec extends AnyWordSpec with Matchers with MockFactory with HttpV2Support {

  val config: Configuration = Configuration(
    ConfigFactory.parseString(
      """
        | self {
        |   url = host1.com
        |  },
        |  microservice {
        |    services {
        |      declaration {
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

  val connector: DefaultDeclarationConnector = new DefaultDeclarationConnector(mockHttp, new ServicesConfig(config)) {
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

  "DeclarationInfoConnector" when {
    val backEndUrl = "http://localhost:7502/accounts/overpaymentdeclarationdisplay/v1"

    implicit val hc: HeaderCarrier = HeaderCarrier()
    val request                    = sample[DeclarationRequest]

    "handling request for declaration" must {

      "do a post http call and get the ACC14 API response" in {
        val httpResponse = HttpResponse(200, "acc-14 response payload")
        mockHttpPostSuccess[HttpResponse](backEndUrl, Json.toJson(request), httpResponse)
        val response     = await(connector.getDeclaration(request).value)
        response shouldBe Right(httpResponse)
      }
    }

    "return an error" when {
      "the call fails" in {
        mockHttpPostWithException(backEndUrl, Json.toJson(request), new NotFoundException("not found"))
        val response = await(connector.getDeclaration(request).value)
        response.isLeft shouldBe true
      }
    }
  }
}
