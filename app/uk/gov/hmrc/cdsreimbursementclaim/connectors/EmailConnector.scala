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

import cats.data.EitherT
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.cdsreimbursementclaim.connectors.DefaultEmailConnector.SendEmailRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ClaimSubmitResponse
import uk.gov.hmrc.cdsreimbursementclaim.models.email.EmailRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.http.AcceptLanguage
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import java.net.URL

@ImplementedBy(classOf[DefaultEmailConnector])
trait EmailConnector {
  def sendClaimSubmitConfirmationEmail(
    submitClaimResponse: ClaimSubmitResponse,
    emailRequest: EmailRequest
  )(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Error, HttpResponse]
}

@Singleton
class DefaultEmailConnector @Inject() (
  http: HttpClient,
  servicesConfig: ServicesConfig
)(implicit
  ec: ExecutionContext
) extends EmailConnector {

  val sendEmailUrl: URL = URL(s"${servicesConfig.baseUrl("email")}/hmrc/email")

  val claimSubmittedTemplateId: String = servicesConfig.getString("email.claim-submitted.template-id")

  override def sendClaimSubmitConfirmationEmail(
    submitClaimResponse: ClaimSubmitResponse,
    emailRequest: EmailRequest
  )(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Error, HttpResponse] = EitherT {
    http
      .POST[JsValue, HttpResponse](
        sendEmailUrl,
        Json.toJson(
          SendEmailRequest(
            List(emailRequest.email.value),
            DefaultEmailConnector.getEmailTemplate(
              AcceptLanguage.fromHeaderCarrier(hc).getOrElse(AcceptLanguage.EN),
              claimSubmittedTemplateId
            ),
            Map(
              "name"        -> emailRequest.contactName,
              "caseNumber"  -> submitClaimResponse.caseNumber,
              "claimAmount" -> emailRequest.claimAmount.toString
            ),
            force = false
          )
        )
      )
      .map(Right(_))
      .recover { case e =>
        Left(Error(e))
      }
  }
}

object DefaultEmailConnector {

  final case class SendEmailRequest(
    to: List[String],
    templateId: String,
    parameters: Map[String, String],
    force: Boolean
  )

  implicit val sendEmailRequestWrites: Writes[SendEmailRequest] = Json.writes[SendEmailRequest]

  @SuppressWarnings(Array("org.wartremover.warts.PlatformDefault"))
  def getEmailTemplate(language: AcceptLanguage, baseTemplateName: String): String =
    language match {
      case AcceptLanguage.EN => baseTemplateName
      case AcceptLanguage.CY => baseTemplateName + "_" + AcceptLanguage.CY.toString.toLowerCase
    }

}
