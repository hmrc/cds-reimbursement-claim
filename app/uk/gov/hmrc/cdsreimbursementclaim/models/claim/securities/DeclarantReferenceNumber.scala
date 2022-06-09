/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.cdsreimbursementclaim.models.claim.securities

import play.api.libs.json.Format
import uk.gov.hmrc.cdsreimbursementclaim.utils.SimpleStringFormat

final case class DeclarantReferenceNumber(value: String) extends AnyVal

object DeclarantReferenceNumber {
  implicit val format: Format[DeclarantReferenceNumber] =
    SimpleStringFormat(DeclarantReferenceNumber(_), _.value)
}
