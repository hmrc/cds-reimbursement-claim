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

package uk.gov.hmrc.cdsreimbursementclaim.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ExistingClaim
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{ExistingClaimStatus, ReasonForSecurity}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton
class DuplicateClaimController @Inject() (cc: ControllerComponents) extends BackendController(cc) {

  def isDuplicate(mrn: MRN, reasonForSecurity: ReasonForSecurity): Action[AnyContent] = Action {
    // Temporary result, this will be replaced in CDSR-1783
    val sampleAnswer = reasonForSecurity match {
      case ReasonForSecurity.AccountSales => ExistingClaim(true, Some(mrn.value), Some(ExistingClaimStatus.Open))
      case _                              => ExistingClaim(false, None, None)
    }
    Ok(Json.toJson(sampleAnswer))
  }
}
