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

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}

import uk.gov.hmrc.cdsreimbursementclaim.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization

trait EisConnector {

  val appConfig: AppConfig

  val dateFormat = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z").withZone(ZoneId.systemDefault())

  val jsonHeaders = Seq(("Content-Type" -> "application/json"), ("Accept" -> "application/json"))
  val xmlHeaders  = Seq(("Content-Type" -> "application/xml; charset=UTF-8"), ("Accept" -> "application/xml"))

  def getStandardHeaders(): Seq[(String, String)] =
    Seq(
      ("Date"             -> LocalDateTime.now().format(dateFormat)),
      ("X-Correlation-ID" -> java.util.UUID.randomUUID().toString),
      ("X-Forwarded-Host" -> "MDTP")
    )

  def enrichHC(isJson: Boolean)(implicit hc: HeaderCarrier): HeaderCarrier = {
    val jsonOrXmlHeaders = if (isJson) jsonHeaders else xmlHeaders
    hc.copy(
      authorization = Some(Authorization(s"Bearer ${appConfig.eisBearerToken}")),
      extraHeaders = hc.extraHeaders ++ getStandardHeaders() ++ jsonOrXmlHeaders
    )
  }

}
