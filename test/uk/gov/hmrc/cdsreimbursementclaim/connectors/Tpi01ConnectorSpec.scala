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

package uk.gov.hmrc.cdsreimbursementclaim.connectors

import com.typesafe.config.ConfigFactory
import org.scalatest.compatible.Assertion
import play.api.Configuration
import play.api.http.{HeaderNames, MimeTypes, Port}
import play.api.mvc.Results
import play.api.routing.sird._
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.http.CustomHeaderNames
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi01.{ClaimsSelector, _}
import uk.gov.hmrc.cdsreimbursementclaim.utils.{TestDataFromFile, ValidateEisHeaders}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc.Result

@SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext"))
class Tpi01ConnectorSpec extends ConnectorSpec with WithTpi01Connector with ValidateEisHeaders {

  "TP01Connector" when {
    "handling request for claims" must {
      "get the 200 NDRC claims response" in {
        givenEndpointStub { case r @ POST(p"/tpi/getreimbursementclaims/v1") =>
          validateEisHeaders(r.headers)
          Tpi01TestData.tpi01Response200NdrcResult
        } {
          givenTpi01Connector { connector =>
            val response = await(connector.getClaims(Eori("ABC123"), ClaimsSelector.All))
            inside(response) {
              case Right(
                    Response(GetReimbursementClaimsResponse(c, Some(ResponseDetail(true, false, Some(cdfPayCase)))))
                  ) =>
                cdfPayCase.NDRCCaseTotal shouldBe Some("45.123")
                cdfPayCase.SCTYCaseTotal shouldBe None
            }
          }
        }
      }
      "get the 200 Securities claims response" in {
        givenEndpointStub { case POST(p"/tpi/getreimbursementclaims/v1") =>
          Tpi01TestData.tpi01Response200SctyResult
        } {
          givenTpi01Connector { connector =>
            val response = await(connector.getClaims(Eori("ABC123"), ClaimsSelector.All))
            inside(response) {
              case Right(
                    Response(GetReimbursementClaimsResponse(c, Some(ResponseDetail(false, true, Some(cdfPayCase)))))
                  ) =>
                cdfPayCase.NDRCCaseTotal shouldBe None
                cdfPayCase.SCTYCaseTotal shouldBe Some("123.45")
            }
          }
        }
      }
      "get the 200 Ndrc and Securities claims response" in {
        givenEndpointStub { case POST(p"/tpi/getreimbursementclaims/v1") =>
          Tpi01TestData.tpi01Response200NdcrAndSctyResult
        } {
          givenTpi01Connector { connector =>
            val response = await(connector.getClaims(Eori("ABC123"), ClaimsSelector.All))
            inside(response) {
              case Right(
                    Response(GetReimbursementClaimsResponse(c, Some(ResponseDetail(true, true, Some(cdfPayCase)))))
                  ) =>
                cdfPayCase.NDRCCaseTotal shouldBe Some("45.123")
                cdfPayCase.SCTYCaseTotal shouldBe Some("123.45")
            }
          }
        }
      }
      "get the 200 empty claims response" in {
        givenEndpointStub { case POST(p"/tpi/getreimbursementclaims/v1") =>
          Tpi01TestData.tpi01Response200NoClaimsResult
        } {
          givenTpi01Connector { connector =>
            val response = await(connector.getClaims(Eori("ABC123"), ClaimsSelector.All))
            inside(response) {
              case Right(Response(GetReimbursementClaimsResponse(c, Some(ResponseDetail(false, false, details))))) =>
                details shouldBe empty
            }
          }
        }
      }
      "get the 400 missing field error" in {
        givenEndpointStub { case POST(p"/tpi/getreimbursementclaims/v1") =>
          Tpi01TestData.tpi01Response400MissingFieldResult
        } {
          givenTpi01Connector { connector =>
            val response = await(connector.getClaims(Eori("ABC123"), ClaimsSelector.All))
            inside(response) { case Left(ErrorResponse(400, Some(errorDetails))) =>
              errorDetails.errorCode    shouldBe "400"
              errorDetails.errorMessage shouldBe "Invalid message"
              errorDetails.source       shouldBe "ct-api"
              inside(errorDetails.sourceFaultDetail) { case SourceFaultDetail(details) =>
                details should not be empty
              }
            }
          }
        }
      }
      "get the 400 pattern error" in {
        givenEndpointStub { case POST(p"/tpi/getreimbursementclaims/v1") =>
          Tpi01TestData.tpi01Response400PatternErrorResult
        } {
          givenTpi01Connector { connector =>
            val response = await(connector.getClaims(Eori("ABC123"), ClaimsSelector.All))
            inside(response) { case Left(ErrorResponse(400, Some(errorDetails))) =>
              errorDetails.errorCode    shouldBe "400"
              errorDetails.errorMessage shouldBe "Invalid message"
              errorDetails.source       shouldBe "ct-api"
              inside(errorDetails.sourceFaultDetail) { case SourceFaultDetail(details) =>
                details should not be empty
              }
            }
          }
        }
      }
      "get the 500 system timeout error" in {
        givenEndpointStub { case POST(p"/tpi/getreimbursementclaims/v1") =>
          Tpi01TestData.tpi01Response500SystemTimeoutErrorResult
        } {
          givenTpi01Connector { connector =>
            val response = await(connector.getClaims(Eori("ABC123"), ClaimsSelector.All))
            inside(response) { case Left(ErrorResponse(500, Some(errorDetails))) =>
              errorDetails.errorCode    shouldBe "500"
              errorDetails.errorMessage shouldBe "Error connecting to the server"
              errorDetails.source       shouldBe "Backend"
              inside(errorDetails.sourceFaultDetail) { case SourceFaultDetail(details) =>
                details should not be empty
              }
            }
          }
        }
      }
    }

  }

}

trait WithTpi01Connector {

  def givenTpi01Connector(body: Tpi01Connector => Assertion): Port => HttpClient => Assertion = { port => httpClient =>
    val config: Configuration = Configuration(
      ConfigFactory.parseString(
        s"""
        | self {
        |   url = host1.com
        |  },
        |  microservice {
        |    services {
        |      claim {
        |        protocol = http
        |        host     = localhost
        |        port     = $port
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

    val connector: Tpi01Connector = new Tpi01Connector(httpClient, new ServicesConfig(config)) {
      override def getExtraHeaders: Seq[(String, String)] =
        Seq(
          HeaderNames.DATE                   -> "some-date",
          CustomHeaderNames.X_CORRELATION_ID -> "some-correlation-id",
          HeaderNames.X_FORWARDED_HOST       -> Platform.MDTP,
          HeaderNames.CONTENT_TYPE           -> MimeTypes.JSON,
          HeaderNames.ACCEPT                 -> MimeTypes.JSON
        )
    }

    body(connector)
  }
}

object Tpi01TestData extends TestDataFromFile {

  private val jsonContentType = HeaderNames.CONTENT_TYPE -> MimeTypes.JSON

  lazy val tpi01Response200NdrcResult: Result =
    Results.Ok(contentOfFile("conf/resources/tpi01/response-200-NDRC.json")).withHeaders(jsonContentType)

  lazy val tpi01Response200SctyResult: Result =
    Results.Ok(contentOfFile("conf/resources/tpi01/response-200-SCTY.json")).withHeaders(jsonContentType)

  lazy val tpi01Response200NdcrAndSctyResult: Result =
    Results.Ok(contentOfFile("conf/resources/tpi01/response-200-NDRC-SCTY.json")).withHeaders(jsonContentType)

  lazy val tpi01Response200NoClaimsResult: Result =
    Results.Ok(contentOfFile("conf/resources/tpi01/response-200-no-claims-found.json")).withHeaders(jsonContentType)

  lazy val tpi01Response200InvalidEoriResult =
    Results.Ok(contentOfFile("conf/resources/tpi01/response-200-invalid-eori.json")).withHeaders(jsonContentType)

  lazy val tpi01Response400MissingFieldResult =
    Results
      .BadRequest(contentOfFile("conf/resources/tpi01/response-400-mandatory-missing-field.json"))
      .withHeaders(jsonContentType)

  lazy val tpi01Response400PatternErrorResult =
    Results
      .BadRequest(contentOfFile("conf/resources/tpi01/response-400-pattern-error.json"))
      .withHeaders(jsonContentType)

  lazy val tpi01Response500SystemTimeoutErrorResult =
    Results
      .InternalServerError(contentOfFile("conf/resources/tpi01/response-500-system-timeout.json"))
      .withHeaders(jsonContentType)

}
