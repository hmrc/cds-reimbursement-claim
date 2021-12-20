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

import play.api.libs.json.{Format, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.{AuthenticateActions, AuthenticatedRequest}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{C285ClaimRequest, RejectedGoodsClaimRequest}
import uk.gov.hmrc.cdsreimbursementclaim.services.ClaimService
import uk.gov.hmrc.cdsreimbursementclaim.services.ccs.CcsSubmissionService
import uk.gov.hmrc.cdsreimbursementclaim.services.tpi05.{C285ClaimToTPI05Mapper, CE1779ClaimToTPI05Mapper, ClaimToTPI05Mapper}
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging.LoggerOps
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class SubmitClaimController @Inject() (
  authenticate: AuthenticateActions,
  claimService: ClaimService,
  ccsSubmissionService: CcsSubmissionService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext, c285Mapper: C285ClaimToTPI05Mapper, ce1779Mapper: CE1779ClaimToTPI05Mapper)
    extends BackendController(cc)
    with Logging {

  def submitC285Claim(): Action[JsValue] = authenticate(parse.json).async { implicit request =>
    withJsonBody[C285ClaimRequest](submitClaim(_))
  }

  def submitRejectedGoodsClaim(): Action[JsValue] = authenticate(parse.json).async { implicit request =>
    withJsonBody[RejectedGoodsClaimRequest](submitClaim(_))
  }

  private def submitClaim[A](claimRequest: A)(implicit
    request: AuthenticatedRequest[_],
    claimToTPI05Mapper: ClaimToTPI05Mapper[A],
    claimRequestFormat: Format[A]
  ): Future[Result] = {
    val result =
      for {
        submitClaimResponse <- claimService.submitClaim(claimRequest)
        _                   <- ccsSubmissionService.enqueue(claimRequest, submitClaimResponse)
        _                    = logger.info(s"Enqueued supporting evidences for claim")
      } yield submitClaimResponse

    result.fold(
      { e =>
        logger.warn("Could not submit claim", e)
        InternalServerError
      },
      submitClaimResponse => Ok(Json.toJson(submitClaimResponse))
    )
  }
}
