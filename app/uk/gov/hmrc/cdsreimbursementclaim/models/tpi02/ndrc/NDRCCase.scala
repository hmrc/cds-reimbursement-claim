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

package uk.gov.hmrc.cdsreimbursementclaim.models.tpi02.ndrc

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

final case class NDRCCase(
  NDRCDetail: NDRCDetail,
  NDRCAmounts: NDRCAmounts
)

object NDRCCase {
  implicit val reads: Reads[NDRCCase] =
    (JsPath.read[NDRCDetail] and JsPath.read[NDRCAmounts])(NDRCCase.apply _)

  implicit val writes: Writes[NDRCCase] =
    (JsPath.write[NDRCDetail] and JsPath.write[NDRCAmounts])(unlift(NDRCCase.unapply))
}
