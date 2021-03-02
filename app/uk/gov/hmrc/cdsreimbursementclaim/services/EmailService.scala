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
import cats.implicits.catsSyntaxEq
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.http.Status.ACCEPTED
import play.api.mvc.Request
import uk.gov.hmrc.cdsreimbursementclaim.connectors.EmailConnector
import uk.gov.hmrc.cdsreimbursementclaim.metrics.Metrics
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.SubmitClaimResponse
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.audit.ClaimConfirmationEmailSentEvent
import uk.gov.hmrc.cdsreimbursementclaim.models.email.EmailRequest
import uk.gov.hmrc.cdsreimbursementclaim.services.audit.AuditService
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[EmailServiceImpl])
trait EmailService {

  def sendClaimConfirmationEmail(
    emailRequest: EmailRequest,
    submitClaimResponse: SubmitClaimResponse
  )(implicit hc: HeaderCarrier, request: Request[_]): EitherT[Future, Error, Unit]

}

@Singleton
class EmailServiceImpl @Inject() (connector: EmailConnector, auditService: AuditService, metrics: Metrics)(implicit
  ec: ExecutionContext
) extends EmailService
    with Logging {

  def sendClaimConfirmationEmail(
    emailRequest: EmailRequest,
    submitClaimResponse: SubmitClaimResponse
  )(implicit hc: HeaderCarrier, request: Request[_]): EitherT[Future, Error, Unit] = {
    val timer = metrics.submitClaimConfirmationEmailTimer.time()

    connector
      .sendClaimSubmitConfirmationEmail(
        submitClaimResponse,
        emailRequest
      )
      .subflatMap { httpResponse =>
        timer.close()
        if (httpResponse.status === ACCEPTED)
          Right(auditClaimConfirmationEmailSent(emailRequest, submitClaimResponse))
        else {
          metrics.submitClaimConfirmationEmailErrorCounter.inc()
          Left(Error(s"call to send claim confirmation email came back with status ${httpResponse.status}"))
        }
      }
  }

  private def auditClaimConfirmationEmailSent(
    emailRequest: EmailRequest,
    submitClaimResponse: SubmitClaimResponse
  )(implicit hc: HeaderCarrier, request: Request[_]): Unit =
    auditService.sendEvent(
      "claimConfirmationEmailSent",
      ClaimConfirmationEmailSentEvent(
        emailRequest.email.value,
        submitClaimResponse.caseNumber
      ),
      "claim-confirmation-email-sent"
    )

}
