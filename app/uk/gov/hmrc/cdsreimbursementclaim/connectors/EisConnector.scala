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

import play.api.http.{HeaderNames, MimeTypes}
import uk.gov.hmrc.cdsreimbursementclaim.models.Ids.UUIDGeneratorImpl
import uk.gov.hmrc.cdsreimbursementclaim.utils.TimeUtils
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait EisConnector {

  val config: ServicesConfig

  val eisBearerToken: String = config.getString("eis.bearer-token")

  def getExtraHeaders: Seq[(String, String)] =
    Seq(
      HeaderNames.DATE             -> TimeUtils.rfc7231DateTimeNow,
      "X-Correlation-ID"           -> new UUIDGeneratorImpl().correlationId,
      HeaderNames.X_FORWARDED_HOST -> "MDTP",
      HeaderNames.CONTENT_TYPE     -> MimeTypes.JSON,
      HeaderNames.ACCEPT           -> MimeTypes.JSON
    )

  def enrichHC(implicit hc: HeaderCarrier): HeaderCarrier =
    hc.copy(
      authorization = Some(Authorization(s"Bearer $eisBearerToken")),
      extraHeaders = hc.extraHeaders ++ getExtraHeaders
    )

}
