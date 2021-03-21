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
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.SubmitClaimResponse
import uk.gov.hmrc.cdsreimbursementclaim.models.email.EmailRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.EmailRequestGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ClaimGen._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global

class EmailConnectorSpec extends AnyWordSpec with Matchers with MockFactory with HttpSupport {

  val claimSubmittedTemplateId = "template-claim-submitted"

  val config: Configuration = Configuration(
    ConfigFactory.parseString(
      s"""
         |microservice {
         |  services {
         |    email {
         |      protocol = http
         |      host     = host
         |      port     = 123
         |    }
         |  }
         |}
         |
         |email {
         |    claim-submitted {
         |        template-id = "$claimSubmittedTemplateId"
         |    }
         |}
         |""".stripMargin
    )
  )

  private val emptyJsonBody = "{}"

  val connector = new DefaultEmailConnector(mockHttp, new ServicesConfig(config))

  "Email Connector" when {

    "it receives a request to send a claim submitted confirmation email" must {

      val submitClaimResponse = sample[SubmitClaimResponse].copy(caseNumber = "case-number")
      val emailRequest        = sample[EmailRequest]

      val expectedRequestBody = Json.parse(
        s"""{
           |  "to": ["${emailRequest.email.value}"],
           |  "templateId": "$claimSubmittedTemplateId",
           |  "parameters": {
           |    "name": "${emailRequest.contactName.value}",
           |    "caseNumber": "${submitClaimResponse.caseNumber}",
           |    "claimAmount": "${emailRequest.claimAmount.toString}"
           |  },
           |  "force": false
           |}
           |""".stripMargin
      )

      "make a http post call and return a result" in {
        implicit val hc: HeaderCarrier = HeaderCarrier().copy(otherHeaders = Seq("Accept-Language" -> "en"))

        List(
          HttpResponse(204, emptyJsonBody),
          HttpResponse(401, emptyJsonBody),
          HttpResponse(400, emptyJsonBody)
        ).foreach { httpResponse =>
          withClue(s"For http response [${httpResponse.toString}]") {

            mockPost(
              s"http://host:123/hmrc/email",
              Seq.empty,
              expectedRequestBody
            )(Some(httpResponse))

            await(
              connector.sendClaimSubmitConfirmationEmail(submitClaimResponse, emailRequest).value
            ) shouldBe Right(
              httpResponse
            )
          }
        }
      }

      "return an error" when {
        implicit val hc: HeaderCarrier = HeaderCarrier().copy(otherHeaders = Seq("Accept-Language" -> "cy"))

        val expectedRequestBody = Json.parse(
          s"""{
             |  "to": ["${emailRequest.email.value}"],
             |  "templateId": "${claimSubmittedTemplateId}_cy",
             |  "parameters": {
             |    "name": "${emailRequest.contactName.value}",
             |    "caseNumber": "${submitClaimResponse.caseNumber}",
             |    "claimAmount": "${emailRequest.claimAmount.toString}"
             |  },
             |  "force": false
             |}
             |""".stripMargin
        )

        "the call fails" in {
          mockPost(
            s"http://host:123/hmrc/email",
            Seq.empty,
            expectedRequestBody
          )(None)

          await(
            connector.sendClaimSubmitConfirmationEmail(submitClaimResponse, emailRequest).value
          ).isLeft shouldBe true
        }

        "if there is no language specified" in {
          implicit val hc: HeaderCarrier = HeaderCarrier().copy(otherHeaders = Seq())
          await(
            connector.sendClaimSubmitConfirmationEmail(submitClaimResponse, emailRequest).value
          ).isLeft shouldBe true

        }

        "if an invalid language is passed" in {
          implicit val hc: HeaderCarrier = HeaderCarrier().copy(otherHeaders = Seq("Accept-Language" -> "ru"))
          await(
            connector.sendClaimSubmitConfirmationEmail(submitClaimResponse, emailRequest).value
          ).isLeft shouldBe true

        }
      }
    }

  }
}
