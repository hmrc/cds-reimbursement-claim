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
import play.api.mvc.{AnyContent, Result, Results}
import play.api.routing.sird._
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.http.CustomHeaderNames
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen.arbitraryEori
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori
import uk.gov.hmrc.cdsreimbursementclaim.models.sub09._
import uk.gov.hmrc.cdsreimbursementclaim.utils.{SchemaValidation, TestDataFromFile, ValidateEisHeaders}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global

@SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext"))
class SubscriptionConnectorSpec
    extends ConnectorSpec
    with WithSubscriptionConnector
    with ValidateEisHeaders
    with SchemaValidation {

  def validateSubscriptionRequest(request: play.api.mvc.Request[AnyContent]): Unit =
    validateEisHeaders(request.headers)

  "SubscriptionConnector" when {
    "handling request for subscription" must {
      "get the 200 with XI EORI information" in {
        val eori = Generators.sample[Eori]
        givenEndpointStub {
          case GET(p"/subscriptions/subscriptiondisplay/v1" ? q"EORI=${requestEori}") if requestEori === eori.value =>
            SubscriptionTestData.subscriptionResponse200WithXiEori
          case _                                                                                                    =>
            Results.ExpectationFailed
        }(validateSubscriptionRequest) {
          givenSubscriptionConnector { connector =>
            val response = await(connector.getSubscription(eori))
            inside(response) {
              case Right(Some(SubscriptionResponse(SubscriptionDisplayResponse(_, details)))) =>
                details.XI_Subscription.get.XI_EORINo shouldBe "MY_OWN_EORI"
              case _                                                                          =>
                fail("expected some subscription but got none")
            }
          }
        }
      }

      "get the 200 without XI EORI information" in {
        val eori = Generators.sample[Eori]
        givenEndpointStub {
          case GET(p"/subscriptions/subscriptiondisplay/v1" ? q"EORI=${requestEori}") if requestEori === eori.value =>
            SubscriptionTestData.subscriptionResponse200WithoutXiEori
          case _                                                                                                    =>
            Results.ExpectationFailed
        }(validateSubscriptionRequest) {
          givenSubscriptionConnector { connector =>
            val response = await(connector.getSubscription(eori))
            inside(response) {
              case Right(Some(SubscriptionResponse(SubscriptionDisplayResponse(_, details)))) =>
                details.XI_Subscription.isEmpty shouldBe true
              case _                                                                          =>
                fail("expected some subscription but got none")
            }
          }
        }
      }

      "get the 400 with business error" in {
        val eori = Generators.sample[Eori]
        givenEndpointStub {
          case GET(p"/subscriptions/subscriptiondisplay/v1" ? q"EORI=${requestEori}") if requestEori === eori.value =>
            SubscriptionTestData.subscriptionResponse400WithError
          case _                                                                                                    =>
            Results.ExpectationFailed
        }(validateSubscriptionRequest) {
          givenSubscriptionConnector { connector =>
            val result = await(connector.getSubscription(eori))
            result shouldBe Left(
              """A call to SUB09 API returned unexpected status 400 with body: {  "errorDetail": {    "timestamp": "2016-10-10T14:30:44Z",    "correlationId": "6bbd0963-f9f0-4d00-8169-9438d8d3044d",    "errorCode": "400",    "errorMessage": "REGIME missing or invalid",    "source": "Back End",    "sourceFaultDetail": {      "detail": [        "002 - SAP_NUMBER missing or invalid"      ]    }  }}"""
            )
          }
        }
      }

      "get the 404" in {
        val eori = Generators.sample[Eori]
        givenEndpointStub {
          case GET(p"/subscriptions/subscriptiondisplay/v1" ? q"EORI=${requestEori}") if requestEori === eori.value =>
            Results.NotFound
          case _                                                                                                    =>
            Results.ExpectationFailed
        }(validateSubscriptionRequest) {
          givenSubscriptionConnector { connector =>
            val response = await(connector.getSubscription(eori))
            inside(response) {
              case Right(Some(_)) =>
                fail("expected none subscription but got some")
              case _              =>
                succeed
            }
          }
        }
      }

      "get the 503" in {
        val eori = Generators.sample[Eori]
        givenEndpointStub {
          case GET(p"/subscriptions/subscriptiondisplay/v1" ? q"EORI=${requestEori}") if requestEori === eori.value =>
            Results.ServiceUnavailable
          case _                                                                                                    =>
            Results.ExpectationFailed
        }(validateSubscriptionRequest) {
          givenSubscriptionConnector { connector =>
            val result = await(connector.getSubscription(eori))
            result shouldBe Left("A call to SUB09 API returned unexpected status 503 with empty body.")
          }
        }
      }

      "get the 503 with non-empty body" in {
        val eori = Generators.sample[Eori]
        givenEndpointStub {
          case GET(p"/subscriptions/subscriptiondisplay/v1" ? q"EORI=${requestEori}") if requestEori === eori.value =>
            Results.ServiceUnavailable("Some reason for the error.")
          case _                                                                                                    =>
            Results.ExpectationFailed
        }(validateSubscriptionRequest) {
          givenSubscriptionConnector { connector =>
            val result = await(connector.getSubscription(eori))
            result shouldBe Left(
              "A call to SUB09 API returned unexpected status 503 with body: Some reason for the error."
            )
          }
        }
      }
    }
  }
}

trait WithSubscriptionConnector {

  def givenSubscriptionConnector(body: SubscriptionConnector => Assertion): Port => HttpClient => Assertion = {
    port => httpClient =>
      val config: Configuration = Configuration(
        ConfigFactory.parseString(
          s"""
        | self {
        |   url = host1.com
        |  },
        |  microservice {
        |    services {
        |      subscription {
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

      val connector: SubscriptionConnector = new DefaultSubscriptionConnector(httpClient, new ServicesConfig(config)) {
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

object SubscriptionTestData extends TestDataFromFile {

  private val jsonContentType = HeaderNames.CONTENT_TYPE -> MimeTypes.JSON

  lazy val subscriptionResponse200WithXiEori: Result =
    Results
      .Ok(contentOfFile("conf/resources/sub09/companyInformationResponse.json"))
      .withHeaders(jsonContentType)

  lazy val subscriptionResponse200WithoutXiEori: Result =
    Results
      .Ok(contentOfFile("conf/resources/sub09/companyInformationNoXiEori.json"))
      .withHeaders(jsonContentType)

  lazy val subscriptionResponse400WithError: Result =
    Results
      .BadRequest(contentOfFile("conf/resources/sub09/companyInformationErrorResponse.json"))
      .withHeaders(jsonContentType)

}
