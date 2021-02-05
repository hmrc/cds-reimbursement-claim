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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.cdsreimbursementclaim.models.SubmitClaimRequest
import uk.gov.hmrc.cdsreimbursementclaim.services.SubmitClaimService
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging.LoggerOps
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton()
class SubmitClaimController @Inject() (
  eisService: SubmitClaimService,
  cc: ControllerComponents
)(implicit
  ec: ExecutionContext
) extends BackendController(cc)
    with Logging {

  def claim(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    (for {
      response <- eisService.submitClaim(request.body.as[SubmitClaimRequest])
    } yield response)
      .fold(
        e => {
          logger.warn(s"could not submit claim", e)
          InternalServerError
        },
        response => Ok(Json.toJson(response))
      )
  }

}
