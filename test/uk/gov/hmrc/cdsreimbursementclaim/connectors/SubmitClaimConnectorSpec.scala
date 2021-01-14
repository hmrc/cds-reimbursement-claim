package uk.gov.hmrc.cdsreimbursementclaim.connectors

import com.typesafe.config.ConfigFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.libs.json.Json
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global

class SubmitClaimConnectorSpec extends AnyWordSpec with Matchers with MockFactory with HttpSupport {

  val (eisBearerToken, eisEnvironment) = "token" -> "environment"

  val config: Configuration = Configuration(
    ConfigFactory.parseString(
      s"""
         |microservice {
         |  services {
         |      eis {
         |        host = localhost
         |        port = 7502
         |        context-base = "/CDFPAY/v1/PostNewClaims"
         |        bearer-token = "blablabla"
         |        environment = "qa"
         |    }
         |  }
         |}
         |
         |""".stripMargin
    )
  )

  val connector = new DefaultSubmitClaimConnector(mockHttp, new ServicesConfig(config))

  "SubmitClaimConnectorSpec" when {

    implicit val hc: HeaderCarrier = HeaderCarrier()
    val expectedHeaders            = Seq(
      "Authorization" -> s"Bearer $eisBearerToken",
      "Environment"   -> eisEnvironment
    ) // TODO: add the other headers here that you need after gettting the API spec

    "handling request to submit claim" must {

      "do a post http call and get the TPIO5 API response" in {

        mockPost(
          "some url", //TODO: put the correct url here
          expectedHeaders,
          *
        )(
          Some(
            HttpResponse(200, "{}")
          ) //TODO: change later to what is actually expected to be return ie the JSON payload
        )

        await(connector.submitClaim(Json.parse("{}")).value) shouldBe Right(HttpResponse(200, "{}"))

      }
    }

    "return an error" when {

      "the call fails" in {
        mockPost(
          "some url", //TODO: change
          expectedHeaders,
          *
        )(None)

        await(connector.submitClaim(Json.parse("{}")).value).isLeft shouldBe true
      }
    }

  }
}
