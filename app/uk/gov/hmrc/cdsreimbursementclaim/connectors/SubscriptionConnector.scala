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

import com.google.inject.ImplementedBy
import uk.gov.hmrc.cdsreimbursementclaim.connectors.eis.{EisConnector, JsonHeaders}
import uk.gov.hmrc.cdsreimbursementclaim.models.EisErrorResponse
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori
import uk.gov.hmrc.cdsreimbursementclaim.models.sub09.SubscriptionResponse
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@ImplementedBy(classOf[DefaultSubscriptionConnector])
trait SubscriptionConnector {
  def getSubscription(eori: Eori)(implicit
    hc: HeaderCarrier
  ): Future[Either[String, Option[SubscriptionResponse]]]
}

@Singleton
class DefaultSubscriptionConnector @Inject() (http: HttpClientV2, val config: ServicesConfig)(implicit
  ec: ExecutionContext
) extends SubscriptionConnector
    with EisConnector
    with JsonHeaders {

  import SubscriptionConnector._

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

  override def getSubscription(
    eori: Eori
  )(implicit hc: HeaderCarrier): Future[Either[String, Option[SubscriptionResponse]]] = {
    val url: String                            = getSubscriptionUrl
    val queryParameters: Seq[(String, String)] = getQueryParameters(eori)
    http
      .get(URL(url))
      .transform(_.addHttpHeaders(getEISRequiredHeaders: _*).addQueryStringParameters(queryParameters: _*))
      .execute[Either[EisErrorResponse, SubscriptionResponse]]
      .flatMap {
        case Right(subscriptionResponse) =>
          subscriptionResponse.subscriptionDisplayResponse.responseDetail match {
            case Some(_) => Future(Right(Some(subscriptionResponse)))
            case None    =>
              Future.successful(
                Left(
                  s"A call to SUB09 API failed with business error ${subscriptionResponse.subscriptionDisplayResponse.responseCommon.status} ${subscriptionResponse.subscriptionDisplayResponse.responseCommon.statusText
                      .getOrElse("")}"
                )
              )
          }
        case Left(errorResponse)         =>
          if (errorResponse.status == 404)
            Future.successful(Right(None))
          else {
            Future.successful(Left(errorResponse.getErrorDescriptionWithPrefix("A call to SUB09 API")))
          }
      }
      .recover { case NonFatal(e) =>
        Left(s"A call to SUB09 API failed with the exception: $e")
      }
  }

}

object SubscriptionConnector {

  final implicit val reads: HttpReads[Either[EisErrorResponse, SubscriptionResponse]] =
    EisErrorResponse.readsErrorDetailOr[SubscriptionResponse]

}
