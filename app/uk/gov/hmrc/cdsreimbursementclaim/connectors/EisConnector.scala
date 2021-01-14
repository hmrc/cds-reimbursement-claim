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

import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait EisConnector {

  val config: ServicesConfig

  val bearerToken: String = config.getString("eis.bearer-token")

  val environment: String = config.getString("eis.environment")

  //TODO: add the ones you need here
  val headers: Seq[(String, String)] = Seq(
    "Authorization" -> s"Bearer $bearerToken",
    "Environment"   -> environment
  )

//  def addHeaders(hc: HeaderCarrier, bearerToken: String): HeaderCarrier = {
//    val dateFormat = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z").withZone(ZoneId.systemDefault())
//    val localDate  = LocalDateTime.now().format(dateFormat)
//
//    val headers = Seq(
//      ("Date"             -> localDate),
//      ("X-Correlation-ID" -> java.util.UUID.randomUUID().toString),
//      ("X-Forwarded-Host" -> "MDTP"),
//      ("Content-Type"     -> "application/json"),
//      ("Accept"           -> "application/json")
//    )
//
//    hc.copy(
//      authorization = Some(Authorization(s"Bearer $bearerToken")),
//      extraHeaders = hc.extraHeaders ++ headers
//    )
//  }
}
