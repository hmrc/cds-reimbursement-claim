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

package uk.gov.hmrc.cdsreimbursementclaim.connectors.eis

import play.api.http.{HeaderNames, MimeTypes}
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.http.CustomHeaderNames
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.RFC7231DateTime
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.CorrelationId

import scala.collection.immutable

trait JsonHeaders {

  def getExtraHeaders: immutable.Seq[(String, String)] =
    Seq(
      HeaderNames.DATE                   -> RFC7231DateTime.now,
      CustomHeaderNames.X_CORRELATION_ID -> CorrelationId(),
      HeaderNames.X_FORWARDED_HOST       -> Platform.MDTP,
      HeaderNames.CONTENT_TYPE           -> MimeTypes.JSON,
      HeaderNames.ACCEPT                 -> MimeTypes.JSON
    )

}
