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

import cats.syntax.eq._
import com.google.inject.ImplementedBy
import uk.gov.hmrc.cdsreimbursementclaim.connectors.eis.{EisConnector, JsonHeaders}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori
import uk.gov.hmrc.cdsreimbursementclaim.models.sub09.SubscriptionResponse
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpResponse

@ImplementedBy(classOf[DefaultSubscriptionConnector])
trait SubscriptionConnector {
  def getSubscription(eori: Eori)(implicit
    hc: HeaderCarrier
  ): Future[Option[SubscriptionResponse]]
}

@Singleton
class DefaultSubscriptionConnector @Inject() (http: HttpClient, val config: ServicesConfig)(implicit
  ec: ExecutionContext
) extends SubscriptionConnector
    with EisConnector
    with JsonHeaders {

  private val MDG_MAX_ACKNOWLEDGEMENT_REFERENCE_LENGTH = 32

  private def acknowledgementReference: String =
    UUID
      .randomUUID()
      .toString
      .replace("-", "")
      .takeRight(MDG_MAX_ACKNOWLEDGEMENT_REFERENCE_LENGTH)

  private val getSubscriptionUrl: String =
    s"${config.baseUrl("subscription")}/subscriptions/subscriptiondisplay/v1"

  private def getQueryParameters(eori: Eori): Seq[(String, String)] =
    Seq("EORI" -> eori.value, "acknowledgementReference" -> acknowledgementReference, "regime" -> "CDS")

  override def getSubscription(eori: Eori)(implicit hc: HeaderCarrier): Future[Option[SubscriptionResponse]] = {
    val url: String                            = getSubscriptionUrl
    val queryParameters: Seq[(String, String)] = getQueryParameters(eori)
    http
      .GET[HttpResponse](url, queryParameters, getEISRequiredHeaders)
      .flatMap {
        case response if response.status === 200 =>
          Future(response.json.as[SubscriptionResponse]).map(Some.apply)
        case response if response.status === 404 =>
          Future.successful(None)
        case response                            =>
          Future.failed(new Exception(s"Request to GET $url returned ${response.status} with body:\n${response.body}"))
      }
  }

}
