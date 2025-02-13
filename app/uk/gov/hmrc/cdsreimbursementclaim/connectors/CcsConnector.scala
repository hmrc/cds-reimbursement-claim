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
import uk.gov.hmrc.cdsreimbursementclaim.connectors.eis.{EisConnector, XmlHeaders}
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.ccs.CcsSubmissionPayload
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.writeableOf_String

@ImplementedBy(classOf[DefaultCcsConnector])
trait CcsConnector {
  def submitToCcs(ccsSubmissionPayload: CcsSubmissionPayload)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Error, HttpResponse]
}

@Singleton
class DefaultCcsConnector @Inject() (http: HttpClientV2, val config: ServicesConfig)(implicit
  ec: ExecutionContext
) extends CcsConnector
    with EisConnector
    with XmlHeaders
    with Logging {

  private val ccsSubmissionUrl: String = s"${config.baseUrl("ccs")}/filetransfer/init/v1"

  override def submitToCcs(ccsSubmissionPayload: CcsSubmissionPayload)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Error, HttpResponse] =
    EitherT[Future, Error, HttpResponse](
      http
        .post(URL(ccsSubmissionUrl))
        .withBody(ccsSubmissionPayload.dec64Body)
        .setHeader(ccsSubmissionPayload.headers ++ getEISRequiredHeaders: _*)
        .execute[HttpResponse]
        .map(Right(_))
        .recover { case e => Left(Error(e)) }
    )

}
