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
import cats.instances.future._
import org.scalamock.handlers.{CallHandler3, CallHandler6}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Writes
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.cdsreimbursementclaim.connectors.EmailConnector
import uk.gov.hmrc.cdsreimbursementclaim.metrics.MockMetrics
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ClaimSubmitResponse
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.audit.ClaimConfirmationEmailSentEvent
import uk.gov.hmrc.cdsreimbursementclaim.models.email.EmailRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.EmailRequestGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TPI05RequestGen._
import uk.gov.hmrc.cdsreimbursementclaim.services.audit.AuditService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext"))
class EmailServiceSpec extends AnyWordSpec with Matchers with MockFactory {

  val mockAuditService: AuditService = mock[AuditService]

  val mockEmailConnector: EmailConnector = mock[EmailConnector]

  val service = new DefaultEmailService(mockEmailConnector, mockAuditService, MockMetrics.metrics)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val request: Request[_] = FakeRequest()

  def mockSendClaimConfirmationEmail(submitClaimResponse: ClaimSubmitResponse, emailRequest: EmailRequest)(
    result: Either[Error, HttpResponse]
  ): CallHandler3[ClaimSubmitResponse, EmailRequest, HeaderCarrier, EitherT[Future, Error, HttpResponse]] =
    (mockEmailConnector
      .sendClaimSubmitConfirmationEmail(_: ClaimSubmitResponse, _: EmailRequest)(_: HeaderCarrier))
      .expects(submitClaimResponse, emailRequest, *)
      .returning(EitherT.fromEither[Future](result))

  def mockAuditClaimConfirmationEmailEvent(
    emailRequest: EmailRequest,
    submitClaimResponse: ClaimSubmitResponse
  ): CallHandler6[String, ClaimConfirmationEmailSentEvent, String, HeaderCarrier, Writes[
    ClaimConfirmationEmailSentEvent
  ], Request[_], Unit] =
    (
      mockAuditService
        .sendEvent(_: String, _: ClaimConfirmationEmailSentEvent, _: String)(
          _: HeaderCarrier,
          _: Writes[ClaimConfirmationEmailSentEvent],
          _: Request[_]
        )
      )
      .expects(
        "ClaimConfirmationEmailSent",
        ClaimConfirmationEmailSentEvent(
          emailRequest.email.value,
          submitClaimResponse.caseNumber
        ),
        "claim-confirmation-email-sent",
        *,
        *,
        *
      )
      .returning(())

  "Email Service" when {

    "handling requests to send claim submission confirmation emails" must {

      val (submitClaimResponse, emailRequest) = sample[ClaimSubmitResponse] -> sample[EmailRequest]

      "return an error" when {

        "the http status of the response is not 202 (accepted)" in {
          mockSendClaimConfirmationEmail(submitClaimResponse, emailRequest)(
            Right(HttpResponse(200, ""))
          )

          await(
            service.sendClaimConfirmationEmail(emailRequest, submitClaimResponse).value
          ).isLeft shouldBe true
        }

        "the call to send the email fails" in {
          mockSendClaimConfirmationEmail(submitClaimResponse, emailRequest)(Left(Error("")))

          await(
            service.sendClaimConfirmationEmail(emailRequest, submitClaimResponse).value
          ).isLeft shouldBe true
        }

      }

      "return a successful response" when {

        "the http status of the response is 202 (accepted)" in {
          inSequence {
            mockSendClaimConfirmationEmail(submitClaimResponse, emailRequest)(
              Right(HttpResponse(202, ""))
            )
            mockAuditClaimConfirmationEmailEvent(
              emailRequest,
              submitClaimResponse
            )
          }

          await(
            service.sendClaimConfirmationEmail(emailRequest, submitClaimResponse).value
          ) shouldBe Right(())

        }

      }

    }

  }

}
