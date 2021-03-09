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
import com.google.inject.ImplementedBy
import uk.gov.hmrc.cdsreimbursementclaim.connectors.eis.{EisConnector, XmlHeaders}
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.ccs.CcsSubmissionPayload
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.cdsreimbursementclaim.http.CustomHeaderNames
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import cats.syntax.eq._
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultCcsConnector])
trait CcsConnector {
  def submitToCcs(ccsSubmissionPayload: CcsSubmissionPayload)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Error, HttpResponse]
}

@Singleton
class DefaultCcsConnector @Inject() (http: HttpClient, val config: ServicesConfig)(implicit
  ec: ExecutionContext
) extends CcsConnector
    with EisConnector
    with XmlHeaders
    with Logging {

  private val ccsSubmissionUrl: String = s"${config.baseUrl("ccs")}/filetransfer/init/v1"

  override def submitToCcs(ccsSubmissionPayload: CcsSubmissionPayload)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Error, HttpResponse] = {
    logger.info(
      "submitToCcs request CORRELATION_ID: " + ccsSubmissionPayload.headers
        .find(_._1 === CustomHeaderNames.X_CORRELATION_ID)
        .map(_._2)
        .getOrElse("CORRELATION_ID was not found")
    )
    EitherT[Future, Error, HttpResponse](
      http
        .POSTString[HttpResponse](
          ccsSubmissionUrl,
          ccsSubmissionPayload.dec64Body,
          ccsSubmissionPayload.headers
        )(
          HttpReads[HttpResponse],
          extraHeaders,
          ec
        )
        .map { response =>
          if (response.status != 204) {
            logger.warn(s"submitToCcs response status: ${response.status}, body: ${response.body}")
          }
          response
        }
        .map(Right(_))
        .recover { case e =>
          logger.warn(s"submitToCcs failed", e)
          Left(Error(e))
        }
    )
  }

}
