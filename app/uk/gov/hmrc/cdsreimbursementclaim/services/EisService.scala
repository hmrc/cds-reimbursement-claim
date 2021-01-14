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
import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import uk.gov.hmrc.cdsreimbursementclaim.config.AppConfig
import uk.gov.hmrc.cdsreimbursementclaim.connectors.EisConnector
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.http.HttpErrorFunctions.is2xx
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@ImplementedBy(classOf[EisServiceImpl])
trait EisService {
  def submitClaim(body: JsValue)(implicit hc: HeaderCarrier): EitherT[Future, Throwable, HttpResponse]
}

@Singleton
class EisServiceImpl @Inject() (appConfig: AppConfig, http: HttpClient)
    extends EisService
    with EisConnector
    with Logging {
  def submitClaim(body: JsValue)(implicit hc: HeaderCarrier): EitherT[Future, Throwable, HttpResponse] = {
    val hcWithExtraHeaders = addHeaders(hc, appConfig.eisBearerToken)
    val response           = http
      .POST[JsValue, HttpResponse](appConfig.newClaimEndpoint, body)(
        implicitly,
        implicitly,
        hcWithExtraHeaders,
        implicitly
      )
      .map { response =>
        if (!is2xx(response.status))
          logger.warn(s"Downstream error,response status: ${response.status}, body: ${response.body}")
        response
      }
    EitherT.right(response)
  }
}
