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

package uk.gov.hmrc.cdsreimbursementclaim.services

import cats.data.EitherT
import cats.implicits.catsSyntaxTuple2Semigroupal
import org.scalamock.handlers._
import org.scalamock.scalatest.MockFactory
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.cdsreimbursementclaim.connectors.ClaimConnector
import uk.gov.hmrc.cdsreimbursementclaim.metrics.MockMetrics
import uk.gov.hmrc.cdsreimbursementclaim.models
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.audit.{SubmitClaimEvent, SubmitClaimResponseEvent}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.EisSubmitClaimRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.email.{Email, EmailRequest}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Acc14DeclarationGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.OverpaymentsClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.RejectedGoodsClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TPI05RequestGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.services.audit.AuditService
import uk.gov.hmrc.cdsreimbursementclaim.services.email.{ClaimToEmailMapper, OverpaymentsMultipleClaimToEmailMapper, OverpaymentsScheduledClaimToEmailMapper, OverpaymentsSingleClaimToEmailMapper}
import uk.gov.hmrc.cdsreimbursementclaim.services.tpi05.{ClaimToTPI05Mapper, OverpaymentsMultipleClaimToTPI05Mapper, OverpaymentsScheduledClaimToTPI05Mapper, OverpaymentsSingleClaimToTPI05Mapper}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext"))
class ClaimServiceSpec
    extends AnyWordSpec
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with MockFactory
    with OptionValues {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 100)

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

  implicit val overpaymentsSingleClaimMapper: OverpaymentsSingleClaimToTPI05Mapper =
    mock[OverpaymentsSingleClaimToTPI05Mapper]

  implicit val overpaymentsMultipleClaimMapper: OverpaymentsMultipleClaimToTPI05Mapper =
    mock[OverpaymentsMultipleClaimToTPI05Mapper]

  implicit val overpaymentsScheduledClaimMapper: OverpaymentsScheduledClaimToTPI05Mapper =
    mock[OverpaymentsScheduledClaimToTPI05Mapper]

  implicit val singleRejectedGoodsClaimMapper
    : ClaimToTPI05Mapper[(SingleRejectedGoodsClaim, List[DisplayDeclaration])] =
    mock[ClaimToTPI05Mapper[(SingleRejectedGoodsClaim, List[DisplayDeclaration])]]

  implicit val multipleRejectedGoodsClaimMapper
    : ClaimToTPI05Mapper[(MultipleRejectedGoodsClaim, List[DisplayDeclaration])] =
    mock[ClaimToTPI05Mapper[(MultipleRejectedGoodsClaim, List[DisplayDeclaration])]]

  implicit val overpaymentsSingleClaimEmailMapperMock: OverpaymentsSingleClaimToEmailMapper =
    mock[OverpaymentsSingleClaimToEmailMapper]

  implicit val overpaymentsMultipleClaimEmailMapperMock: OverpaymentsMultipleClaimToEmailMapper =
    mock[OverpaymentsMultipleClaimToEmailMapper]

  implicit val overpaymentsScheduledClaimEmailMapperMock: OverpaymentsScheduledClaimToEmailMapper =
    mock[OverpaymentsScheduledClaimToEmailMapper]

  implicit val singleRejectedGoodsClaimEmailMapperMock
    : ClaimToEmailMapper[(SingleRejectedGoodsClaim, List[DisplayDeclaration])] =
    mock[ClaimToEmailMapper[(SingleRejectedGoodsClaim, List[DisplayDeclaration])]]

  implicit val multipleRejectedGoodsClaimEmailMapperMock
    : ClaimToEmailMapper[(MultipleRejectedGoodsClaim, List[DisplayDeclaration])] =
    mock[ClaimToEmailMapper[(MultipleRejectedGoodsClaim, List[DisplayDeclaration])]]

  def mockDeclarationRetrieving(mrn: MRN)(
    displayDeclaration: DisplayDeclaration
  ): CallHandler3[MRN, Option[String], HeaderCarrier, EitherT[Future, Error, Option[DisplayDeclaration]]] =
    (declarationServiceMock
      .getDeclaration(_: MRN, _: Option[String])(_: HeaderCarrier))
      .expects(mrn, *, *)
      .returning(EitherT.rightT(Some(displayDeclaration)))

  def mockClaimMapping[A](claim: A, eis: EisSubmitClaimRequest)(implicit
    claimMapper: ClaimToTPI05Mapper[A]
  ): CallHandler1[A, Either[Error, EisSubmitClaimRequest]] =
    (claimMapper
      .map(_: A))
      .expects(claim)
      .returning(Right(eis))

  def mockClaimEmailRequestMapping[A](claim: A, emailRequest: EmailRequest)(implicit
    claimMapper: ClaimToEmailMapper[A]
  ): CallHandler1[A, Either[Error, EmailRequest]] =
    (claimMapper
      .map(_: A))
      .expects(claim)
      .returning(Right(emailRequest))

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

  def mockSendClaimSubmitConfirmationEmail(
    emailRequest: EmailRequest,
    submitClaimResponse: ClaimSubmitResponse
  )(
    response: Either[Error, Unit]
  ): CallHandler4[EmailRequest, ClaimSubmitResponse, HeaderCarrier, Request[_], EitherT[Future, models.Error, Unit]] =
    (emailServiceMock
      .sendClaimConfirmationEmail(_: EmailRequest, _: ClaimSubmitResponse)(_: HeaderCarrier, _: Request[_]))
      .expects(
        emailRequest,
        submitClaimResponse,
        *,
        *
      )
      .returning(EitherT(Future.successful(response)))

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

  "Claim Service" when {

    "handling submission of claims" should {

      "successfully submit a Single Overpayments claim" in forAll(
        genOverpaymentsSingleClaimAllTypes,
        genC285EisRequest
      ) {
        (
          singleOverpaymentsClaimData: (SingleOverpaymentsClaim, DisplayDeclaration, Option[DisplayDeclaration]),
          eisRequest: EisSubmitClaimRequest
        ) =>
          val (claim, declaration, duplicateDeclaration) = singleOverpaymentsClaimData
          val responseJsonBody                           = Json.parse(
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
          val emailRequest        = EmailRequest(
            Email(claim.claimantInformation.contactInformation.emailAddress.value),
            claim.claimantInformation.contactInformation.contactPerson.value,
            claim.reimbursements.map(_.amount).sum
          )

          inAnyOrder {
            mockDeclarationRetrieving(claim.movementReferenceNumber)(declaration).atLeastOnce()
            (overpaymentsSingleClaimMapper
              .map(_: (SingleOverpaymentsClaim, DisplayDeclaration, Option[DisplayDeclaration])))
              .expects((claim, declaration, duplicateDeclaration))
              .returning(Right(eisRequest))
            (claim.duplicateMovementReferenceNumber, duplicateDeclaration).mapN(
              mockDeclarationRetrieving(_)(_).atLeastOnce()
            )
            mockAuditSubmitClaimEvent(eisRequest)
            mockSubmitClaim(eisRequest)(
              Right(HttpResponse(200, responseJsonBody, Map.empty[String, Seq[String]]))
            )
            mockAuditSubmitClaimResponseEvent(
              httpStatus = 200,
              responseBody = Some(responseJsonBody),
              submitClaimRequest = SingleOverpaymentsClaimRequest(claim),
              eisSubmitClaimRequest = eisRequest
            )
            mockClaimEmailRequestMapping(singleOverpaymentsClaimData, emailRequest)
            mockSendClaimSubmitConfirmationEmail(emailRequest, submitClaimResponse)(Right(()))
          }

          await(
            claimService.submitSingleOverpaymentsClaim(SingleOverpaymentsClaimRequest(claim)).value
          ) shouldBe Right(submitClaimResponse)
      }

      "successfully submit a Scheduled Overpayments claim" in forAll(
        genOverpaymentsScheduledClaimAllTypes,
        genC285EisRequest
      ) {
        (
          scheduledOverpaymentsClaimData: (ScheduledOverpaymentsClaim, DisplayDeclaration),
          eisRequest: EisSubmitClaimRequest
        ) =>
          val (claim, declaration) = scheduledOverpaymentsClaimData
          val responseJsonBody     = Json.parse(
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
          val emailRequest        = EmailRequest(
            Email(claim.claimantInformation.contactInformation.emailAddress.value),
            claim.claimantInformation.contactInformation.contactPerson.value,
            claim.totalReimbursementAmount
          )

          inSequence {
            mockDeclarationRetrieving(claim.movementReferenceNumber)(declaration)

            (overpaymentsScheduledClaimMapper
              .map(_: (ScheduledOverpaymentsClaim, DisplayDeclaration)))
              .expects((claim, declaration))
              .returning(Right(eisRequest))

            mockAuditSubmitClaimEvent(eisRequest)
            mockSubmitClaim(eisRequest)(
              Right(HttpResponse(200, responseJsonBody, Map.empty[String, Seq[String]]))
            )
            mockAuditSubmitClaimResponseEvent(
              httpStatus = 200,
              responseBody = Some(responseJsonBody),
              submitClaimRequest = ScheduledOverpaymentsClaimRequest(claim),
              eisSubmitClaimRequest = eisRequest
            )
            mockClaimEmailRequestMapping(scheduledOverpaymentsClaimData, emailRequest)
            mockSendClaimSubmitConfirmationEmail(emailRequest, submitClaimResponse)(Right(()))
          }

          await(
            claimService.submitScheduledOverpaymentsClaim(ScheduledOverpaymentsClaimRequest(claim)).value
          ) shouldBe Right(submitClaimResponse)
      }

      "successfully submit a multiple Overpayments claim" in forAll {
        (
          multipleOverpaymentsClaimData: (MultipleOverpaymentsClaim, List[DisplayDeclaration]),
          eisRequest: EisSubmitClaimRequest
        ) =>
          val claim                = multipleOverpaymentsClaimData._1
          val declarations         = multipleOverpaymentsClaimData._2
          val reversedDeclarations = declarations.reverse
          val responseJsonBody     = Json.parse(
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
          val emailRequest        = EmailRequest(
            Email(claim.claimantInformation.contactInformation.emailAddress.value),
            claim.claimantInformation.contactInformation.contactPerson.value,
            claim.reimbursementClaims.values.flatMap(_.values).sum
          )

          inSequence {
            declarations.foreach { dd =>
              val mrn = MRN(dd.displayResponseDetail.declarationId)
              mockDeclarationRetrieving(mrn)(dd)
            }

            mockClaimMapping((claim, reversedDeclarations), eisRequest)

            mockAuditSubmitClaimEvent(eisRequest)

            mockSubmitClaim(eisRequest)(
              Right(HttpResponse(200, responseJsonBody, Map.empty[String, Seq[String]]))
            )
            mockAuditSubmitClaimResponseEvent(
              httpStatus = 200,
              responseBody = Some(responseJsonBody),
              submitClaimRequest = MultipleOverpaymentsClaimRequest(claim),
              eisSubmitClaimRequest = eisRequest
            )
            mockClaimEmailRequestMapping((claim, reversedDeclarations), emailRequest)
            mockSendClaimSubmitConfirmationEmail(emailRequest, submitClaimResponse)(Right(()))
          }

          await(
            claimService.submitMultipleOverpaymentsClaim(MultipleOverpaymentsClaimRequest(claim)).value
          ) shouldBe Right(submitClaimResponse)
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
          val emailRequest        = EmailRequest(
            Email(ce1779ClaimRequest.claim.claimantInformation.contactInformation.emailAddress.value),
            ce1779ClaimRequest.claim.claimantInformation.contactInformation.contactPerson.value,
            ce1779ClaimRequest.claim.reimbursements.map(_.amount).sum
          )

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
            mockClaimEmailRequestMapping((ce1779ClaimRequest.claim, List(displayDeclaration)), emailRequest)
            mockSendClaimSubmitConfirmationEmail(emailRequest, submitClaimResponse)(Right(()))
          }

          await(claimService.submitRejectedGoodsClaim(ce1779ClaimRequest).value) shouldBe Right(submitClaimResponse)
      }

      "successfully submit a Multiple Rejected Goods claim" in forAll {
        (
          details: (MultipleRejectedGoodsClaim, List[DisplayDeclaration]),
          eisRequest: EisSubmitClaimRequest
        ) =>
          val claim                = details._1
          val declarations         = details._2
          val reversedDeclarations = declarations.reverse

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
          val emailRequest        = EmailRequest(
            Email(claim.claimantInformation.contactInformation.emailAddress.value),
            claim.claimantInformation.contactInformation.contactPerson.value,
            claim.reimbursementClaims.values.flatMap(_.values).sum
          )

          inSequence {
            declarations.foreach { dd =>
              val mrn = MRN(dd.displayResponseDetail.declarationId)
              mockDeclarationRetrieving(mrn)(dd)
            }
            mockClaimMapping((claim, reversedDeclarations), eisRequest)
            mockAuditSubmitClaimEvent(eisRequest)
            mockSubmitClaim(eisRequest)(
              Right(HttpResponse(200, responseJsonBody, Map.empty[String, Seq[String]]))
            )
            mockAuditSubmitClaimResponseEvent(
              httpStatus = 200,
              responseBody = Some(responseJsonBody),
              submitClaimRequest = RejectedGoodsClaimRequest(claim),
              eisSubmitClaimRequest = eisRequest
            )
            mockClaimEmailRequestMapping((claim, reversedDeclarations), emailRequest)
            mockSendClaimSubmitConfirmationEmail(emailRequest, submitClaimResponse)(Right(()))
          }

          await(
            claimService.submitMultipleRejectedGoodsClaim(RejectedGoodsClaimRequest(claim)).value
          ) shouldBe Right(submitClaimResponse)
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
          val emailRequest        = EmailRequest(
            Email(ce1779ClaimRequest.claim.claimantInformation.contactInformation.emailAddress.value),
            ce1779ClaimRequest.claim.claimantInformation.contactInformation.contactPerson.value,
            ce1779ClaimRequest.claim.reimbursements.map(_.amount).sum
          )

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
            mockClaimEmailRequestMapping((ce1779ClaimRequest.claim, List(displayDeclaration)), emailRequest)
            mockSendClaimSubmitConfirmationEmail(emailRequest, submitClaimResponse)(Left(Error("some error")))
          }

          await(claimService.submitRejectedGoodsClaim(ce1779ClaimRequest).value) shouldBe Right(submitClaimResponse)
      }

      "return an error" when {

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

      }
    }
  }
}
