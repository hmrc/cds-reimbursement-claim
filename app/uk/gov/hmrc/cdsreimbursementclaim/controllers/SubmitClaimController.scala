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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.SubmitClaimRequest
import uk.gov.hmrc.cdsreimbursementclaim.services.SubmitClaimService
import uk.gov.hmrc.cdsreimbursementclaim.services.ccs.CcsSubmissionService
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging.LoggerOps
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class SubmitClaimController @Inject() (
  claimService: SubmitClaimService,
  ccsSubmissionService: CcsSubmissionService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def test(): Action[AnyContent] = Action {
    Ok("sdfsdfs")
  }

  //TODO: auth
  def submitClaim(): Action[JsValue] = Action(parse.json).async { implicit request =>
    withJsonBody[SubmitClaimRequest] { claimRequest =>
      val result =
        for {
          submitClaimResponse <- claimService.submitClaim(claimRequest)
          _                   <- ccsSubmissionService.enqueue(claimRequest, submitClaimResponse)
          _                    = logger.info(s"enqueued supporting evidences for claim")
        } yield submitClaimResponse

      result.fold(
        { e =>
          logger.warn("could not submit claim", e)
          InternalServerError
        },
        submitClaimResponse => Ok(Json.toJson(submitClaimResponse))
      )
    }
  }

}
