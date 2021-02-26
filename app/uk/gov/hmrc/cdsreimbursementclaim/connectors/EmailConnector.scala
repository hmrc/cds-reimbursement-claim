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

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.cdsreimbursementclaim.connectors.DefaultEmailConnector.SendEmailRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.SubmitClaimResponse
import uk.gov.hmrc.cdsreimbursementclaim.models.email.EmailRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.http.AcceptLanguage
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultEmailConnector])
trait EmailConnector {
  def sendClaimSubmitConfirmationEmail(
    submitClaimResponse: SubmitClaimResponse,
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

  val sendEmailUrl: String = s"${servicesConfig.baseUrl("email")}/hmrc/email"

  val claimSubmittedTemplateId: String = servicesConfig.getString("email.claim-submitted.template-id")

  override def sendClaimSubmitConfirmationEmail(
    submitClaimResponse: SubmitClaimResponse,
    emailRequest: EmailRequest
  )(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Error, HttpResponse] =
    for {
      acceptLanguage <- EitherT.fromOption[Future](
                          AcceptLanguage.fromHeaderCarrier(hc),
                          Error("could not find Accept-Language HTTP header")
                        )
      httpResponse   <- EitherT[Future, Error, HttpResponse](
                          http
                            .POST[JsValue, HttpResponse](
                              sendEmailUrl,
                              Json.toJson(
                                SendEmailRequest(
                                  List(emailRequest.email.value),
                                  DefaultEmailConnector.getEmailTemplate(acceptLanguage, claimSubmittedTemplateId),
                                  Map(
                                    "name"       -> emailRequest.contactName.value,
                                    "caseNumber" -> submitClaimResponse.caseNumber
                                  ),
                                  force = false
                                )
                              )
                            )
                            .map(Right(_))
                            .recover { case e =>
                              Left(Error(e))
                            }
                        )
    } yield httpResponse

}

object DefaultEmailConnector {

  final case class SendEmailRequest(
    to: List[String],
    templateId: String,
    parameters: Map[String, String],
    force: Boolean
  )

  implicit val sendEmailRequestWrites: Writes[SendEmailRequest] = Json.writes[SendEmailRequest]

  def getEmailTemplate(language: AcceptLanguage, baseTemplateName: String): String =
    language match {
      case AcceptLanguage.EN => baseTemplateName
      case AcceptLanguage.CY => baseTemplateName + "_" + AcceptLanguage.CY.toString.toLowerCase
    }

}
