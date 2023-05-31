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
import org.scalatest.compatible.Assertion
import play.api.Configuration
import play.api.http.{HeaderNames, MimeTypes, Port}
import play.api.mvc.{Result, Results}
import play.api.routing.sird._
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.http.CustomHeaderNames
import uk.gov.hmrc.cdsreimbursementclaim.models.CDFPayService
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi02._
import uk.gov.hmrc.cdsreimbursementclaim.utils.{TestDataFromFile, ValidateEisHeaders}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import com.eclipsesource.schema.SchemaType
import play.api.mvc.AnyContent
import uk.gov.hmrc.cdsreimbursementclaim.utils.SchemaValidation

import scala.collection.immutable

@SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext"))
class Tpi02ConnectorSpec extends ConnectorSpec with WithTpi02Connector with ValidateEisHeaders with SchemaValidation {

  lazy val tpi02RequestSchema: SchemaType =
    readSchema("conf/resources/tpi02/tpi02-request-schema.json")

  def validateTpi02Request(request: play.api.mvc.Request[AnyContent]): Unit = {
    validateEisHeaders(request.headers)
    validateRequestBody(tpi02RequestSchema, request.body.asJson.getOrElse(fail("Request is missing json body")))
  }

  "Tpi02Connector" when {
    "handling request for claims" must {
      "get the 200 NDRC claim response" in {
        givenEndpointStub { case POST(p"/tpi/getspecificcase/v1") =>
          Tpi02TestData.tpi02Response200NdrcClaimResult
        }(validateTpi02Request) {
          givenTpi02Connector { connector =>
            val response = await(connector.getSpecificClaim(CDFPayService.NDRC, "ABC-123"))
            inside(response) {
              case Right(
                    Response(GetSpecificCaseResponse(c, Some(responseDetail)))
                  ) =>
                c.status                       shouldBe "OK"
                responseDetail.CDFPayCaseFound shouldBe true
                responseDetail.CDFPayService   shouldBe "NDRC"
                responseDetail.NDRCCase        shouldBe defined
                responseDetail.SCTYCase        shouldBe None
            }
          }
        }
      }
      "get the 200 SCTY claim response" in {
        givenEndpointStub { case POST(p"/tpi/getspecificcase/v1") =>
          Tpi02TestData.tpi02Response200SctyClaimResult
        }(validateTpi02Request) {
          givenTpi02Connector { connector =>
            val response = await(connector.getSpecificClaim(CDFPayService.SCTY, "ABC-123"))
            inside(response) {
              case Right(
                    Response(GetSpecificCaseResponse(c, Some(responseDetail)))
                  ) =>
                c.status                       shouldBe "OK"
                responseDetail.CDFPayCaseFound shouldBe true
                responseDetail.CDFPayService   shouldBe "SCTY"
                responseDetail.NDRCCase        shouldBe None
                responseDetail.SCTYCase        shouldBe defined
            }
          }
        }
      }
      "get the 200 SCTY claim minimal response" in {
        givenEndpointStub { case POST(p"/tpi/getspecificcase/v1") =>
          Tpi02TestData.tpi02Response200SctyClaimMinimalResult
        }(validateTpi02Request) {
          givenTpi02Connector { connector =>
            val response = await(connector.getSpecificClaim(CDFPayService.SCTY, "SCTY-2110"))
            inside(response) {
              case Right(
                    Response(GetSpecificCaseResponse(c, Some(responseDetail)))
                  ) =>
                c.status                       shouldBe "OK"
                responseDetail.CDFPayCaseFound shouldBe true
                responseDetail.CDFPayService   shouldBe "SCTY"
                responseDetail.NDRCCase        shouldBe None
                responseDetail.SCTYCase        shouldBe defined
            }
          }
        }
      }
      "get the 200 NDRC no claims found response" in {
        givenEndpointStub { case POST(p"/tpi/getspecificcase/v1") =>
          Tpi02TestData.tpi02Response200NoClaimsFoundResult
        }(validateTpi02Request) {
          givenTpi02Connector { connector =>
            val response = await(connector.getSpecificClaim(CDFPayService.NDRC, "ABC-123"))
            inside(response) {
              case Right(
                    Response(GetSpecificCaseResponse(c, None))
                  ) =>
                c.status       shouldBe "OK"
                c.errorMessage shouldBe Some("Invalid CDFPayCaseNumber")
            }
          }
        }
      }
      "get the 400 missing field error" in {
        givenEndpointStub { case POST(p"/tpi/getspecificcase/v1") =>
          Tpi02TestData.tpi02Response400MissingFieldResult
        }(validateTpi02Request) {
          givenTpi02Connector { connector =>
            val response = await(connector.getSpecificClaim(CDFPayService.NDRC, "ABC-123"))
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
        givenEndpointStub { case POST(p"/tpi/getspecificcase/v1") =>
          Tpi02TestData.tpi02Response400PatternErrorResult
        }(validateTpi02Request) {
          givenTpi02Connector { connector =>
            val response = await(connector.getSpecificClaim(CDFPayService.NDRC, "ABC-123"))
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
        givenEndpointStub { case POST(p"/tpi/getspecificcase/v1") =>
          Tpi02TestData.tpi02Response500SystemTimeoutErrorResult
        }(validateTpi02Request) {
          givenTpi02Connector { connector =>
            val response = await(connector.getSpecificClaim(CDFPayService.NDRC, "ABC-123"))
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

trait WithTpi02Connector {

  def givenTpi02Connector(body: Tpi02Connector => Assertion): Port => HttpClient => Assertion = { port => httpClient =>
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

    val connector: Tpi02Connector = new Tpi02Connector(httpClient, new ServicesConfig(config)) {
      override def getExtraHeaders: immutable.Seq[(String, String)] =
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

object Tpi02TestData extends TestDataFromFile {

  private val jsonContentType = HeaderNames.CONTENT_TYPE -> MimeTypes.JSON

  lazy val tpi02Response200NdrcClaimResult: Result =
    Results
      .Ok(contentOfFile("conf/resources/tpi02/response-200-ndrc-claim.json"))
      .withHeaders(jsonContentType)

  lazy val tpi02Response200SctyClaimResult: Result =
    Results
      .Ok(contentOfFile("conf/resources/tpi02/response-200-scty-claim.json"))
      .withHeaders(jsonContentType)

  lazy val tpi02Response200SctyClaimMinimalResult: Result =
    Results
      .Ok(contentOfFile("conf/resources/tpi02/response-200-scty-claim-minimal.json"))
      .withHeaders(jsonContentType)

  lazy val tpi02Response200NoClaimsFoundResult: Result =
    Results
      .Ok(contentOfFile("conf/resources/tpi02/response-200-no-claims-found.json"))
      .withHeaders(jsonContentType)

  lazy val tpi02Response400MissingFieldResult =
    Results
      .BadRequest(contentOfFile("conf/resources/tpi02/response-400-mandatory-missing-field.json"))
      .withHeaders(jsonContentType)

  lazy val tpi02Response400PatternErrorResult =
    Results
      .BadRequest(contentOfFile("conf/resources/tpi02/response-400-pattern-error.json"))
      .withHeaders(jsonContentType)

  lazy val tpi02Response500SystemTimeoutErrorResult =
    Results
      .InternalServerError(contentOfFile("conf/resources/tpi02/response-500-system-timeout.json"))
      .withHeaders(jsonContentType)

}
