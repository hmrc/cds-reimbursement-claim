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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.request

import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN

final case class TPI04Request(requestCommon: RequestCommon, mrn: MRN, reasonForSecurity: ReasonForSecurity)

object TPI04Request {
  implicit val tpi04RequestWrites: Writes[TPI04Request] = new Writes[TPI04Request] {
    override def writes(o: TPI04Request): JsValue =
      Json.obj(
        "getExistingClaimRequest" -> Json.obj(
          "requestCommon" -> Json.toJson(o.requestCommon),
          "requestDetail" -> Json.obj(
            "CDFPayService"     -> Platform.CDF_PAY_SERVICE,
            "declarationID"     -> o.mrn,
            "reasonForSecurity" -> o.reasonForSecurity.acc14Code
          )
        )
      )
  }
}
