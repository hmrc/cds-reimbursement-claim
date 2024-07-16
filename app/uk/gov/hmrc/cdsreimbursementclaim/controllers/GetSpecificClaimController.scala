/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.cdsreimbursementclaim.models.EisErrorResponse
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi02.{GetSpecificCaseResponse, SpecificClaimResponse}
import uk.gov.hmrc.cdsreimbursementclaim.services.GetSpecificClaimService
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.AuthorisedActions
import scala.concurrent.Future
import play.api.mvc.Result
import play.api.mvc.Request

@Singleton
class GetSpecificClaimController @Inject() (
  authorised: AuthorisedActions,
  service: GetSpecificClaimService,
  cc: ControllerComponents
)(implicit
  ec: ExecutionContext
) extends BackendController(cc)
    with Logging {

  final def getSpecificClaim(cdfPayService: CDFPayService, cdfPayCaseNumber: String): Action[AnyContent] =
    authorised.async({ case (r, _) =>
      implicit val request: Request[AnyContent] = r
      service
        .getSpecificClaim(cdfPayService, cdfPayCaseNumber)
        .map {
          case Right(GetSpecificCaseResponse(responseCommon, Some(responseDetail))) =>
            Ok(Json.toJson(SpecificClaimResponse.fromTpi02Response(responseDetail)))

          case Right(GetSpecificCaseResponse(responseCommon, None)) =>
            BadRequest(Json.toJson(responseCommon))

          case Left(error @ EisErrorResponse(status, Some(errorDetails), _)) =>
            logger.error(error.getErrorDescriptionWithPrefix("A call to TPI02 API"))
            if (status < 499)
              BadRequest(Json.toJson(errorDetails))
            else
              InternalServerError(Json.toJson(errorDetails))

          case Left(error @ EisErrorResponse(status, None, _)) =>
            logger.error(error.getErrorDescriptionWithPrefix("A call to TPI02 API"))
            if (status < 499)
              BadRequest
            else
              InternalServerError
        }
        .recover {
          case ex if ex.getMessage.contains("JSON validation") =>
            logger.error(s"getSpecificClaim failed: ${ex.getMessage}")
            BadRequest(ex.getMessage)

          case NonFatal(error) =>
            logger.error(s"getSpecificClaim failed: ${error.getMessage}")
            ServiceUnavailable(error.getMessage)
        }
    }: AuthorisedActions.Input[AnyContent] => Future[Result])
}
