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

package uk.gov.hmrc.cdsreimbursementclaim.controllers

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.AuthenticateActions
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{C285Claim, SubmitClaimRequest, SubmitClaimResponse}
import uk.gov.hmrc.cdsreimbursementclaim.services.ClaimService
import uk.gov.hmrc.cdsreimbursementclaim.services.ccs.CcsSubmissionService
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging.LoggerOps
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class SubmitClaimController @Inject() (
  authenticate: AuthenticateActions,
  claimService: ClaimService,
  ccsSubmissionService: CcsSubmissionService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def submitC285Claim(): Action[JsValue] = authenticate(parse.json).async { implicit request =>
    withJsonBody[SubmitClaimRequest[C285Claim]] { submitClaimRequest =>
      val result =
        for {
          submitClaimResponse <- claimService.submitClaim(submitClaimRequest)
          _                   <- ccsSubmissionService.enqueue(submitClaimRequest, submitClaimResponse)
          _                    = logger.info(s"Enqueued supporting evidences for claim")
        } yield submitClaimResponse

      result.fold(
        { e =>
          logger.warn("Could not submit claim", e)
          InternalServerError
        },
        (submitClaimResponse: SubmitClaimResponse) => Ok(Json.toJson(submitClaimResponse))
      )
    }
  }

  def submitCE11779Claim(): Action[JsValue] = authenticate(parse.json).async { implicit request =>
    ???
  }
}
