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
import com.google.inject.ImplementedBy
import play.api.libs.json.Json
import uk.gov.hmrc.cdsreimbursementclaim.connectors.eis.{EisConnector, JsonHeaders}
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.request.DeclarationRequest
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import play.api.libs.ws.JsonBodyWritables.*

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultDeclarationConnector])
trait DeclarationConnector {
  def getDeclaration(declarationRequest: DeclarationRequest)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Error, HttpResponse]
}

@Singleton
class DefaultDeclarationConnector @Inject() (http: HttpClientV2, val config: ServicesConfig)(implicit
  ec: ExecutionContext
) extends DeclarationConnector
    with EisConnector
    with JsonHeaders
    with Logging {

  private val getDeclarationUrl: String = s"${config.baseUrl("declaration")}/accounts/overpaymentdeclarationdisplay/v1"

  def getDeclaration(
    declarationRequest: DeclarationRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, Error, HttpResponse] =
    EitherT[Future, Error, HttpResponse](
      http
        .post(URL(getDeclarationUrl))
        .setHeader(getEISRequiredHeaders: _*)
        .withBody(Json.toJson(declarationRequest))
        .execute[HttpResponse]
        .map(Right(_))
        .recover { case e => Left(Error(e)) }
    )

}
