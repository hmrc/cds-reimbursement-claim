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

import uk.gov.hmrc.cdsreimbursementclaim.utils.TimeUtils
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization

trait EisHeaders {
  val contentHeaders: Seq[(String, String)]

  def getStandardHeaders(): Seq[(String, String)] =
    Seq(
      ("Date"             -> TimeUtils.rfc7231DateTimeNow),
      ("X-Correlation-ID" -> java.util.UUID.randomUUID().toString),
      ("X-Forwarded-Host" -> "MDTP")
    )
}

trait JsonHeaders extends EisHeaders {
  val contentHeaders = Seq(("Content-Type" -> "application/json"), ("Accept" -> "application/json"))
}

trait XmlHeaders extends EisHeaders {
  val contentHeaders = Seq(("Content-Type" -> "application/xml; charset=UTF-8"), ("Accept" -> "application/xml"))
}

trait EisConnector { self: EisHeaders =>
  def enrichHC(bearerToken: String)(implicit hc: HeaderCarrier): HeaderCarrier =
    hc.copy(
      authorization = Some(Authorization(s"Bearer $bearerToken")),
      extraHeaders = hc.extraHeaders ++ getStandardHeaders() ++ contentHeaders
    )

}
