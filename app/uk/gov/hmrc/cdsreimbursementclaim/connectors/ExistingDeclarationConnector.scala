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
import play.api.libs.json.Json

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.connectors.eis.{EisConnector, JsonHeaders}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ExistingClaim
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ExistingClaim.*
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.request.{RequestCommon, TPI04Request}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.request.TPI04Request.*
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.{CorrelationId, MRN}
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.ISO8601DateTime
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import play.api.libs.ws.JsonBodyWritables.*
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExistingDeclarationConnector @Inject() (
  http: HttpClientV2,
  val config: ServicesConfig
)(implicit ec: ExecutionContext)
    extends EisConnector
    with JsonHeaders
    with Logging {

  private lazy val baseUrl = config.baseUrl("declaration")
  private lazy val url     = s"$baseUrl/tpi/getexistingclaim/v1"

  def checkExistingDeclaration(
    mrn: MRN,
    reasonForSecurity: ReasonForSecurity,
    receiptDate: String = ISO8601DateTime.now,
    acknowledgementReference: String = CorrelationId.compact
  )(implicit hc: HeaderCarrier): EitherT[Future, Error, ExistingClaim] = {
    val requestDetails = TPI04Request(
      RequestCommon(
        Platform.MDTP,
        receiptDate,
        acknowledgementReference
      ),
      mrn,
      reasonForSecurity
    )
    EitherT[
      Future,
      Error,
      ExistingClaim
    ](
      http
        .post(URL(url))
        .setHeader(getEISRequiredHeaders: _*)
        .withBody(Json.toJson(requestDetails))
        .execute[ExistingClaim]
        .map(Right(_))
        .recover { case e => Left(Error(e)) }
    )
  }
}
