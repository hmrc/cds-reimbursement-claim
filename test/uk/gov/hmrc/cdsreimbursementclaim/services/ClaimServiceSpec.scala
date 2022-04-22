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

package uk.gov.hmrc.cdsreimbursementclaim.services

import cats.data.EitherT
import org.scalamock.handlers.{CallHandler1, CallHandler2, CallHandler4, CallHandler6}
import org.scalamock.scalatest.MockFactory
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.cdsreimbursementclaim.connectors.ClaimConnector
import uk.gov.hmrc.cdsreimbursementclaim.metrics.MockMetrics
import uk.gov.hmrc.cdsreimbursementclaim.models
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.audit.{SubmitClaimEvent, SubmitClaimResponseEvent}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{C285ClaimRequest, ClaimSubmitResponse, MultipleRejectedGoodsClaim, RejectedGoodsClaimRequest, SingleRejectedGoodsClaim}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.EisSubmitClaimRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.email.EmailRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Acc14DeclarationGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.C285ClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.RejectedGoodsClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TPI05RequestGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.services.audit.AuditService
import uk.gov.hmrc.cdsreimbursementclaim.services.tpi05.ClaimToTPI05Mapper
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClaimServiceSpec
    extends AnyWordSpec
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with MockFactory
    with OptionValues {

  val claimConnectorMock: ClaimConnector = mock[ClaimConnector]

  val declarationServiceMock: DeclarationService = mock[DeclarationService]

  val emailServiceMock: EmailService = mock[EmailService]

  val auditServiceMock: AuditService = mock[AuditService]

  val claimService =
    new DefaultClaimService(
      claimConnectorMock,
      declarationServiceMock,
      emailServiceMock,
      auditServiceMock,
      MockMetrics.metrics
    )

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val request: Request[_] = FakeRequest()

  implicit val c285ClaimMapper: ClaimToTPI05Mapper[C285ClaimRequest] =
    mock[ClaimToTPI05Mapper[C285ClaimRequest]]

  implicit val singleRejectedGoodsClaimMapper
    : ClaimToTPI05Mapper[(SingleRejectedGoodsClaim, List[DisplayDeclaration])] =
    mock[ClaimToTPI05Mapper[(SingleRejectedGoodsClaim, List[DisplayDeclaration])]]

  implicit val multipleRejectedGoodsClaimMapper
    : ClaimToTPI05Mapper[(MultipleRejectedGoodsClaim, List[DisplayDeclaration])] =
    mock[ClaimToTPI05Mapper[(MultipleRejectedGoodsClaim, List[DisplayDeclaration])]]

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 1)

  def mockDeclarationRetrieving(mrn: MRN)(
    displayDeclaration: DisplayDeclaration
  ): CallHandler2[MRN, HeaderCarrier, EitherT[Future, Error, Option[DisplayDeclaration]]] =
    (declarationServiceMock
      .getDeclaration(_: MRN)(_: HeaderCarrier))
      .expects(mrn, *)
      .returning(EitherT.rightT(Some(displayDeclaration)))

  def mockClaimMapping[A](claim: A, eis: EisSubmitClaimRequest)(implicit
    claimMapper: ClaimToTPI05Mapper[A]
  ): CallHandler1[A, Either[Error, EisSubmitClaimRequest]] =
    (claimMapper
      .map(_: A))
      .expects(claim)
      .returning(Right(eis))

  def mockSubmitClaim(eisSubmitClaimRequest: EisSubmitClaimRequest)(
    response: Either[Error, HttpResponse]
  ): CallHandler2[EisSubmitClaimRequest, HeaderCarrier, EitherT[Future, Error, HttpResponse]] =
    (claimConnectorMock
      .submitClaim(_: EisSubmitClaimRequest)(_: HeaderCarrier))
      .expects(eisSubmitClaimRequest, hc)
      .returning(EitherT.fromEither[Future](response))

  def mockAuditSubmitClaimEvent(
    eisSubmitClaimRequest: EisSubmitClaimRequest
  ): CallHandler6[String, SubmitClaimEvent, String, HeaderCarrier, Writes[SubmitClaimEvent], Request[_], Unit] =
    (auditServiceMock
      .sendEvent(_: String, _: SubmitClaimEvent, _: String)(
        _: HeaderCarrier,
        _: Writes[SubmitClaimEvent],
        _: Request[_]
      ))
      .expects(
        "SubmitClaim",
        SubmitClaimEvent(
          eisSubmitClaimRequest,
          eisSubmitClaimRequest.postNewClaimsRequest.requestDetail.claimantEORI.value
        ),
        "submit-claim",
        *,
        *,
        *
      )
      .returning(())

  private def mockAuditSubmitClaimResponseEvent[A](
    httpStatus: Int,
    responseBody: Option[JsValue],
    submitClaimRequest: A,
    eisSubmitClaimRequest: EisSubmitClaimRequest
  ) =
    (auditServiceMock
      .sendEvent(_: String, _: SubmitClaimResponseEvent[A], _: String)(
        _: HeaderCarrier,
        _: Writes[SubmitClaimResponseEvent[A]],
        _: Request[_]
      ))
      .expects(
        "SubmitClaimResponse",
        SubmitClaimResponseEvent[A](
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
    eisSubmitClaimRequest: EisSubmitClaimRequest,
    submitClaimResponse: ClaimSubmitResponse
  )(
    response: Either[Error, Unit]
  ): CallHandler4[EmailRequest, ClaimSubmitResponse, HeaderCarrier, Request[_], EitherT[Future, models.Error, Unit]] =
    (emailServiceMock
      .sendClaimConfirmationEmail(_: EmailRequest, _: ClaimSubmitResponse)(_: HeaderCarrier, _: Request[_]))
      .expects(
        EmailRequest(eisSubmitClaimRequest.postNewClaimsRequest.requestDetail).value,
        submitClaimResponse,
        *,
        *
      )
      .returning(EitherT(Future.successful(response)))

  "Claim Service" when {

    "handling submission of claims" should {

      "successfully submit a C285 claim" in forAll {
        (c285ClaimRequest: C285ClaimRequest, eisRequest: EisSubmitClaimRequest) =>
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

          val submitClaimResponse = ClaimSubmitResponse(caseNumber = "4374422408")

          inSequence {
            mockClaimMapping(c285ClaimRequest, eisRequest)
            mockAuditSubmitClaimEvent(eisRequest)
            mockSubmitClaim(eisRequest)(
              Right(HttpResponse(200, responseJsonBody, Map.empty[String, Seq[String]]))
            )
            mockAuditSubmitClaimResponseEvent(
              httpStatus = 200,
              responseBody = Some(responseJsonBody),
              submitClaimRequest = c285ClaimRequest,
              eisSubmitClaimRequest = eisRequest
            )
            mockSendClaimSubmitConfirmationEmail(eisRequest, submitClaimResponse)(Right(()))
          }

          await(claimService.submitC285Claim(c285ClaimRequest).value) shouldBe Right(submitClaimResponse)
      }

      "successfully submit a Single Rejected Goods claim" in forAll {
        (
          ce1779ClaimRequest: RejectedGoodsClaimRequest[SingleRejectedGoodsClaim],
          displayDeclaration: DisplayDeclaration,
          eisRequest: EisSubmitClaimRequest
        ) =>
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

          val submitClaimResponse = ClaimSubmitResponse(caseNumber = "4374422408")

          inSequence {
            mockDeclarationRetrieving(ce1779ClaimRequest.claim.leadMrn)(displayDeclaration)
            mockClaimMapping((ce1779ClaimRequest.claim, List(displayDeclaration)), eisRequest)
            mockAuditSubmitClaimEvent(eisRequest)
            mockSubmitClaim(eisRequest)(
              Right(HttpResponse(200, responseJsonBody, Map.empty[String, Seq[String]]))
            )
            mockAuditSubmitClaimResponseEvent(
              httpStatus = 200,
              responseBody = Some(responseJsonBody),
              submitClaimRequest = ce1779ClaimRequest,
              eisSubmitClaimRequest = eisRequest
            )
            mockSendClaimSubmitConfirmationEmail(eisRequest, submitClaimResponse)(Right(()))
          }

          await(claimService.submitRejectedGoodsClaim(ce1779ClaimRequest).value) shouldBe Right(submitClaimResponse)
      }

      "successfully submit a Multiple Rejected Goods claim" in forAll {
        (
          details: (MultipleRejectedGoodsClaim, List[DisplayDeclaration]),
          eisRequest: EisSubmitClaimRequest
        ) =>
          val claim        = details._1
          val declarations = details._2.reverse

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

          val submitClaimResponse = ClaimSubmitResponse(caseNumber = "4374422408")

          inSequence {
            details._2.foreach { dd =>
              val mrn = MRN(dd.displayResponseDetail.declarationId)
              mockDeclarationRetrieving(mrn)(dd)
            }
            mockClaimMapping((claim, declarations), eisRequest)
            mockAuditSubmitClaimEvent(eisRequest)
            mockSubmitClaim(eisRequest)(
              Right(HttpResponse(200, responseJsonBody, Map.empty[String, Seq[String]]))
            )
            mockAuditSubmitClaimResponseEvent(
              httpStatus = 200,
              responseBody = Some(responseJsonBody),
              submitClaimRequest = RejectedGoodsClaimRequest(details._1),
              eisSubmitClaimRequest = eisRequest
            )
            mockSendClaimSubmitConfirmationEmail(eisRequest, submitClaimResponse)(Right(()))
          }

          await(
            claimService.submitMultipleRejectedGoodsClaim(RejectedGoodsClaimRequest(details._1)).value
          ) shouldBe Right(submitClaimResponse)
      }

      "successfully submit a C285 claim even though sending of the confirmation email was not successful" in forAll {
        (c285ClaimRequest: C285ClaimRequest, eisRequest: EisSubmitClaimRequest) =>
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

          val submitClaimResponse = ClaimSubmitResponse(caseNumber = "4374422408")

          inSequence {
            mockClaimMapping(c285ClaimRequest, eisRequest)
            mockAuditSubmitClaimEvent(eisRequest)
            mockSubmitClaim(eisRequest)(
              Right(HttpResponse(200, responseJsonBody, Map.empty[String, Seq[String]]))
            )
            mockAuditSubmitClaimResponseEvent(
              200,
              Some(responseJsonBody),
              c285ClaimRequest,
              eisRequest
            )
            mockSendClaimSubmitConfirmationEmail(eisRequest, submitClaimResponse)(Left(Error("some error")))
          }

          await(claimService.submitC285Claim(c285ClaimRequest).value) shouldBe Right(submitClaimResponse)
      }

      "successfully submit a Single Rejected Goods claim even though sending of the confirmation email was not successful" in forAll {
        (
          ce1779ClaimRequest: RejectedGoodsClaimRequest[SingleRejectedGoodsClaim],
          displayDeclaration: DisplayDeclaration,
          eisRequest: EisSubmitClaimRequest
        ) =>
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

          val submitClaimResponse = ClaimSubmitResponse(caseNumber = "4374422408")

          inSequence {
            mockDeclarationRetrieving(ce1779ClaimRequest.claim.movementReferenceNumber)(displayDeclaration)
            mockClaimMapping((ce1779ClaimRequest.claim, List(displayDeclaration)), eisRequest)
            mockAuditSubmitClaimEvent(eisRequest)
            mockSubmitClaim(eisRequest)(
              Right(HttpResponse(200, responseJsonBody, Map.empty[String, Seq[String]]))
            )
            mockAuditSubmitClaimResponseEvent(
              200,
              Some(responseJsonBody),
              ce1779ClaimRequest,
              eisRequest
            )
            mockSendClaimSubmitConfirmationEmail(eisRequest, submitClaimResponse)(Left(Error("some error")))
          }

          await(claimService.submitRejectedGoodsClaim(ce1779ClaimRequest).value) shouldBe Right(submitClaimResponse)
      }

      "return an error" when {

        "the response payload contains an error" in forAll {
          (c285ClaimRequest: C285ClaimRequest, eisRequest: EisSubmitClaimRequest) =>
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
              mockClaimMapping(c285ClaimRequest, eisRequest)
              mockAuditSubmitClaimEvent(eisRequest)
              mockSubmitClaim(eisRequest)(
                Right(HttpResponse(200, errorResponseJsonBody, Map.empty[String, Seq[String]]))
              )
              mockAuditSubmitClaimResponseEvent(
                200,
                Some(errorResponseJsonBody),
                c285ClaimRequest,
                eisRequest
              )
            }

            await(claimService.submitC285Claim(c285ClaimRequest).value).isLeft shouldBe true
        }

        "a http response other than 200 OK was received" in forAll {
          (
            ce1779ClaimRequest: RejectedGoodsClaimRequest[SingleRejectedGoodsClaim],
            displayDeclaration: DisplayDeclaration,
            eisRequest: EisSubmitClaimRequest
          ) =>
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
              mockDeclarationRetrieving(ce1779ClaimRequest.claim.movementReferenceNumber)(displayDeclaration)
              mockClaimMapping((ce1779ClaimRequest.claim, List(displayDeclaration)), eisRequest)
              mockAuditSubmitClaimEvent(eisRequest)
              mockSubmitClaim(eisRequest)(
                Right(HttpResponse(400, errorResponseJsonBody, Map.empty[String, Seq[String]]))
              )
              mockAuditSubmitClaimResponseEvent(
                400,
                Some(errorResponseJsonBody),
                ce1779ClaimRequest,
                eisRequest
              )
            }

            await(claimService.submitRejectedGoodsClaim(ce1779ClaimRequest).value).isLeft shouldBe true
        }

        "no case number is returned in the response" in forAll {
          (c285ClaimRequest: C285ClaimRequest, eisRequest: EisSubmitClaimRequest) =>
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
              mockClaimMapping(c285ClaimRequest, eisRequest)
              mockAuditSubmitClaimEvent(eisRequest)
              mockSubmitClaim(eisRequest)(
                Right(HttpResponse(200, errorResponseJsonBody, Map.empty[String, Seq[String]]))
              )
              mockAuditSubmitClaimResponseEvent(
                200,
                Some(errorResponseJsonBody),
                c285ClaimRequest,
                eisRequest
              )
            }

            await(claimService.submitC285Claim(c285ClaimRequest).value).isLeft shouldBe true
        }
      }
    }
  }
}
