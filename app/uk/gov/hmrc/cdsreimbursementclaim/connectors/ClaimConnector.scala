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

package uk.gov.hmrc.cdsreimbursementclaim.connectors

import cats.data.EitherT
import com.google.inject.ImplementedBy
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.cdsreimbursementclaim.connectors.eis.{EisConnector, JsonHeaders}
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.EisSubmitClaimRequest
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultClaimConnector])
trait ClaimConnector {
  def submitClaim(submitClaimRequest: EisSubmitClaimRequest)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Error, HttpResponse]
}

@Singleton
class DefaultClaimConnector @Inject() (http: HttpClient, val config: ServicesConfig)(implicit
  ec: ExecutionContext
) extends ClaimConnector
    with EisConnector
    with JsonHeaders {

  private val submitClaimUrl: String = s"${config.baseUrl("claim")}/tpi/postoverpaymentclaim/v1"

  override def submitClaim(
    submitClaimRequest: EisSubmitClaimRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, Error, HttpResponse] =
    EitherT[Future, Error, HttpResponse](
      http
        .POST[JsValue, HttpResponse](submitClaimUrl, Json.toJson(submitClaimRequest), getEISRequiredHeaders)(
          implicitly[Writes[JsValue]],
          HttpReads[HttpResponse],
          hc,
          ec
        )
        .map(Right(_))
        .recover { case e => Left(Error(e)) }
    )

}
