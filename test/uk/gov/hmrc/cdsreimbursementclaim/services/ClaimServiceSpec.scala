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

package uk.gov.hmrc.cdsreimbursementclaim.services

import cats.data.EitherT
import org.scalamock.handlers.{CallHandler1, CallHandler2, CallHandler4, CallHandler6}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.cdsreimbursementclaim.connectors.ClaimConnector
import uk.gov.hmrc.cdsreimbursementclaim.metrics.MockMetrics
import uk.gov.hmrc.cdsreimbursementclaim.models
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.audit.{SubmitClaimEvent, SubmitClaimResponseEvent}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{SubmitClaimRequest, SubmitClaimResponse}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.EisSubmitClaimRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.email.EmailRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.EmailRequestGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
import uk.gov.hmrc.cdsreimbursementclaim.services.audit.AuditService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClaimServiceSpec extends AnyWordSpec with Matchers with MockFactory {

  val mockClaimConnector: ClaimConnector = mock[ClaimConnector]

  val mockClaimTransformerService: ClaimTransformerService = mock[ClaimTransformerService]

  val mockAuditService: AuditService = mock[AuditService]

  val mockEmailService: EmailService = mock[EmailService]

  val claimService =
    new DefaultClaimService(
      mockClaimConnector,
      mockClaimTransformerService,
      mockEmailService,
      mockAuditService,
      MockMetrics.metrics
    )

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val request: Request[_] = FakeRequest()

  def mockSubmitClaim(eisSubmitClaimRequest: EisSubmitClaimRequest)(
    response: Either[Error, HttpResponse]
  ): CallHandler2[EisSubmitClaimRequest, HeaderCarrier, EitherT[Future, Error, HttpResponse]] =
    (mockClaimConnector
      .submitClaim(_: EisSubmitClaimRequest)(_: HeaderCarrier))
      .expects(eisSubmitClaimRequest, hc)
      .returning(EitherT.fromEither[Future](response))

  def mockAuditSubmitClaimEvent(
    submitClaimRequest: SubmitClaimRequest,
    eisSubmitClaimRequest: EisSubmitClaimRequest
  ): CallHandler6[String, SubmitClaimEvent, String, HeaderCarrier, Writes[SubmitClaimEvent], Request[_], Unit] =
    (mockAuditService
      .sendEvent(_: String, _: SubmitClaimEvent, _: String)(
        _: HeaderCarrier,
        _: Writes[SubmitClaimEvent],
        _: Request[_]
      ))
      .expects(
        "SubmitClaim",
        SubmitClaimEvent(eisSubmitClaimRequest, submitClaimRequest.signedInUserDetails.eori),
        "submit-claim",
        *,
        *,
        *
      )
      .returning(())

  private def mockAuditSubmitClaimResponseEvent(
    httpStatus: Int,
    responseBody: Option[JsValue],
    submitClaimRequest: SubmitClaimRequest,
    eisSubmitClaimRequest: EisSubmitClaimRequest
  ) =
    (mockAuditService
      .sendEvent(_: String, _: SubmitClaimResponseEvent, _: String)(
        _: HeaderCarrier,
        _: Writes[SubmitClaimResponseEvent],
        _: Request[_]
      ))
      .expects(
        "SubmitClaimResponse",
        SubmitClaimResponseEvent(
          httpStatus,
          responseBody.getOrElse(Json.parse("""{ "body" : "could not parse body as JSON: " }""")),
          Json.toJson(eisSubmitClaimRequest),
          submitClaimRequest
        ),
        "submit-claim-response",
        *,
        *,
        *
      )
      .returning(())

  def mockSendClaimSubmitConfirmationEmail(
    emailRequest: EmailRequest,
    submitClaimResponse: SubmitClaimResponse
  )(
    response: Either[Error, Unit]
  ): CallHandler4[EmailRequest, SubmitClaimResponse, HeaderCarrier, Request[_], EitherT[Future, models.Error, Unit]] =
    (mockEmailService
      .sendClaimConfirmationEmail(_: EmailRequest, _: SubmitClaimResponse)(_: HeaderCarrier, _: Request[_]))
      .expects(emailRequest, submitClaimResponse, *, *)
      .returning(EitherT(Future.successful(response)))

  def mockTransformSubmitClaimRequest(
    submitClaimRequest: SubmitClaimRequest
  )(
    response: Either[Error, EisSubmitClaimRequest]
  ): CallHandler1[SubmitClaimRequest, Either[Error, EisSubmitClaimRequest]] = (mockClaimTransformerService
    .toEisSubmitClaimRequest(_: SubmitClaimRequest))
    .expects(submitClaimRequest)
    .returning(response)

  "Claim Service" when {

    "handling submission of claims" should {

      "successfully submit a claim" in {

        val submitClaimRequest = sample[SubmitClaimRequest]

        val eisSubmitClaimRequest = sample[EisSubmitClaimRequest]

        val emailRequest = sample[EmailRequest].copy(
          email = submitClaimRequest.signedInUserDetails.verifiedEmail,
          contactName = submitClaimRequest.signedInUserDetails.contactName,
          claimAmount = submitClaimRequest.claim.totalReimbursementAmount
        )

        val responseJsonBody = Json.parse(
          """
            |{
            |    "postNewClaimsResponse": {
            |        "responseCommon": {
            |            "status": "OK",
            |            "processingDate": "2021-01-20T12:07540Z",
            |            "CDFPayService": "NDRC",
            |            "CDFPayCaseNumber": "4374422408"
            |        }
            |    }
            |}
            |""".stripMargin
        )

        val submitClaimResponse = sample[SubmitClaimResponse].copy(caseNumber = "4374422408")

        inSequence {
          mockTransformSubmitClaimRequest(submitClaimRequest)(Right(eisSubmitClaimRequest))
          mockAuditSubmitClaimEvent(submitClaimRequest, eisSubmitClaimRequest)
          mockSubmitClaim(eisSubmitClaimRequest)(
            Right(HttpResponse(200, responseJsonBody, Map.empty[String, Seq[String]]))
          )
          mockAuditSubmitClaimResponseEvent(
            200,
            Some(responseJsonBody),
            submitClaimRequest,
            eisSubmitClaimRequest
          )
          mockSendClaimSubmitConfirmationEmail(emailRequest, submitClaimResponse)(Right(()))
        }
        await(claimService.submitClaim(submitClaimRequest).value) shouldBe Right(submitClaimResponse)
      }

      "successfully submit a claim even though sending of the confirmation email was not successful" in {

        val submitClaimRequest = sample[SubmitClaimRequest]

        val eisSubmitClaimRequest = sample[EisSubmitClaimRequest]

        val emailRequest = sample[EmailRequest].copy(
          email = submitClaimRequest.signedInUserDetails.verifiedEmail,
          contactName = submitClaimRequest.signedInUserDetails.contactName,
          claimAmount = submitClaimRequest.claim.totalReimbursementAmount
        )

        val responseJsonBody = Json.parse(
          """
            |{
            |    "postNewClaimsResponse": {
            |        "responseCommon": {
            |            "status": "OK",
            |            "processingDate": "2021-01-20T12:07540Z",
            |            "CDFPayService": "NDRC",
            |            "CDFPayCaseNumber": "4374422408"
            |        }
            |    }
            |}
            |""".stripMargin
        )

        val submitClaimResponse = sample[SubmitClaimResponse].copy(caseNumber = "4374422408")

        inSequence {
          mockTransformSubmitClaimRequest(submitClaimRequest)(Right(eisSubmitClaimRequest))
          mockAuditSubmitClaimEvent(submitClaimRequest, eisSubmitClaimRequest)
          mockSubmitClaim(eisSubmitClaimRequest)(
            Right(HttpResponse(200, responseJsonBody, Map.empty[String, Seq[String]]))
          )
          mockAuditSubmitClaimResponseEvent(
            200,
            Some(responseJsonBody),
            submitClaimRequest,
            eisSubmitClaimRequest
          )
          mockSendClaimSubmitConfirmationEmail(emailRequest, submitClaimResponse)(Left(Error("some error")))
        }

        await(claimService.submitClaim(submitClaimRequest).value) shouldBe Right(submitClaimResponse)

      }

      "return an error" when {

        "the response payload contains an error" in {

          val submitClaimRequest = sample[SubmitClaimRequest]

          val eisSubmitClaimRequest = sample[EisSubmitClaimRequest]

          val errorResponseJsonBody = Json.parse(
            """
              |{
              |    "postNewClaimsResponse": {
              |        "responseCommon": {
              |            "status": "OK",
              |            "processingDate": "0000-00-00T00:00:00Z",
              |            "correlationId": "1682aaa9-d212-46ba-852e-43c2d01faf21",
              |            "errorMessage": "Invalid Claim Type",
              |            "returnParameters": [
              |                {
              |                    "paramName": "POSITION",
              |                    "paramValue": "FAIL"
              |                }
              |            ]
              |        }
              |    }
              |}
              |""".stripMargin
          )

          inSequence {
            mockTransformSubmitClaimRequest(submitClaimRequest)(Right(eisSubmitClaimRequest))
            mockAuditSubmitClaimEvent(submitClaimRequest, eisSubmitClaimRequest)
            mockSubmitClaim(eisSubmitClaimRequest)(
              Right(HttpResponse(200, errorResponseJsonBody, Map.empty[String, Seq[String]]))
            )
            mockAuditSubmitClaimResponseEvent(
              200,
              Some(errorResponseJsonBody),
              submitClaimRequest,
              eisSubmitClaimRequest
            )
          }
          await(claimService.submitClaim(submitClaimRequest).value).isLeft shouldBe true

        }

        "no case number is returned in the response" in {

          val submitClaimRequest = sample[SubmitClaimRequest]

          val eisSubmitClaimRequest = sample[EisSubmitClaimRequest]

          val errorResponseJsonBody = Json.parse(
            """
              |{
              |    "postNewClaimsResponse": {
              |        "responseCommon": {
              |            "status": "OK",
              |            "processingDate": "2021-01-20T12:07540Z",
              |            "CDFPayService": "NDRC"
              |        }
              |    }
              |}
              |""".stripMargin
          )

          inSequence {
            mockTransformSubmitClaimRequest(submitClaimRequest)(Right(eisSubmitClaimRequest))
            mockAuditSubmitClaimEvent(submitClaimRequest, eisSubmitClaimRequest)
            mockSubmitClaim(eisSubmitClaimRequest)(
              Right(HttpResponse(200, errorResponseJsonBody, Map.empty[String, Seq[String]]))
            )
            mockAuditSubmitClaimResponseEvent(
              200,
              Some(errorResponseJsonBody),
              submitClaimRequest,
              eisSubmitClaimRequest
            )
          }
          await(claimService.submitClaim(submitClaimRequest).value).isLeft shouldBe true

        }

        "a http response other than 200 OK was received" in {

          val submitClaimRequest = sample[SubmitClaimRequest]

          val eisSubmitClaimRequest = sample[EisSubmitClaimRequest]

          val errorResponseJsonBody = Json.parse(
            """
              |{
              |    "errorDetail": {
              |        "timestamp": "2018-08-08T13:57:53Z",
              |        "correlationId": "1682aaa9-d212-46ba-852e-43c2d01faf21",
              |        "errorCode": "400",
              |        "errorMessage": "Invalid message",
              |        "source": "EIS",
              |        "sourceFaultDetail": {
              |            "detail": [
              |                "some error"
              |            ]
              |        }
              |    }
              |}
              |""".stripMargin
          )

          inSequence {
            mockTransformSubmitClaimRequest(submitClaimRequest)(Right(eisSubmitClaimRequest))
            mockAuditSubmitClaimEvent(submitClaimRequest, eisSubmitClaimRequest)
            mockSubmitClaim(eisSubmitClaimRequest)(
              Right(HttpResponse(400, errorResponseJsonBody, Map.empty[String, Seq[String]]))
            )
            mockAuditSubmitClaimResponseEvent(
              400,
              Some(errorResponseJsonBody),
              submitClaimRequest,
              eisSubmitClaimRequest
            )
          }
          await(claimService.submitClaim(submitClaimRequest).value).isLeft shouldBe true

        }

      }

    }

  }

}
