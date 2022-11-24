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
import uk.gov.hmrc.cdsreimbursementclaim.models.CDFPayService
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi02.{ErrorResponse, GetSpecificCaseResponse, SpecificClaimResponse}
import uk.gov.hmrc.cdsreimbursementclaim.services.GetSpecificClaimService
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

@Singleton
class GetSpecificClaimController @Inject() (service: GetSpecificClaimService, cc: ControllerComponents)(implicit
  ec: ExecutionContext
) extends BackendController(cc)
    with Logging {

  final def getSpecificClaim(cdfPayService: CDFPayService, cdfPayCaseNumber: String): Action[AnyContent] =
    Action.async { implicit request =>
      service
        .getSpecificClaim(cdfPayService, cdfPayCaseNumber)
        .map {
          case Right(GetSpecificCaseResponse(responseCommon, Some(responseDetail))) =>
            Ok(Json.toJson(SpecificClaimResponse.fromTpi02Response(responseDetail)))

          case Right(GetSpecificCaseResponse(responseCommon, None)) =>
            BadRequest(Json.toJson(responseCommon))

          case Left(ErrorResponse(status, errorDetails)) =>
            if (status < 499)
              BadRequest(Json.toJson(errorDetails))
            else
              ServiceUnavailable(Json.toJson(errorDetails))
        }
        .recover {
          case ex if ex.getMessage.contains("JSON validation") =>
            logger.error(s"getSpecificClaim failed: ${ex.getMessage}")
            BadRequest(ex.getMessage)

          case NonFatal(error) =>
            logger.error(s"getSpecificClaim failed: ${error.getMessage}")
            ServiceUnavailable(error.getMessage)
        }
    }
}
