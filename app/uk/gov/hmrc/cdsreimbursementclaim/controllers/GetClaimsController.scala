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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi01.ClaimsSelector
import uk.gov.hmrc.cdsreimbursementclaim.services.GetClaimsService
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class GetClaimsController @Inject() (service: GetClaimsService, cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  final def getClaims(eori: Eori, claimsSelector: ClaimsSelector): Action[AnyContent] =
    Action.async { implicit request =>
      service
        .getClaims(eori, claimsSelector)
        .map {
          case Some(response) => Ok(Json.obj("claims" -> Json.toJson(response)))
          case None           => InternalServerError
        }
        .recover {
          case ex if ex.getMessage.contains("JSON validation") =>
            logger.error(s"getClaims failed: ${ex.getMessage}")
            InternalServerError("JSON Validation Error")
          case NonFatal(error)                                 =>
            logger.error(s"getClaims failed: ${error.getMessage}")
            ServiceUnavailable
        }
    }
}
