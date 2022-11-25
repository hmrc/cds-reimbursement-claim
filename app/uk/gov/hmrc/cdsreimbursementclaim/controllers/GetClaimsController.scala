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
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request, Result}
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.AuthorisedActions
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi01.{ClaimsResponse, ClaimsSelector, ErrorResponse, GetReimbursementClaimsResponse}
import uk.gov.hmrc.cdsreimbursementclaim.services.GetClaimsService
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class GetClaimsController @Inject() (
  authorised: AuthorisedActions,
  service: GetClaimsService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  final val getAllClaims: Action[AnyContent]           = getClaims(ClaimsSelector.All)
  final val getOverpaymentsClaims: Action[AnyContent]  = getClaims(ClaimsSelector.Overpayments)
  final val getNdrcClaims: Action[AnyContent]          = getClaims(ClaimsSelector.Ndrc)
  final val getSecuritiesClaims: Action[AnyContent]    = getClaims(ClaimsSelector.Securities)
  final val getUnderpaymentsClaims: Action[AnyContent] = getClaims(ClaimsSelector.Underpayments)

  private def getClaims(claimsSelector: ClaimsSelector): Action[AnyContent] =
    authorised.async({ case (r, eori) =>
      implicit val request: Request[AnyContent] = r
      service
        .getClaims(eori, claimsSelector)
        .map { x =>
          println(x)
          x

        }
        .map {
          case Right(GetReimbursementClaimsResponse(responseCommon, Some(responseDetail))) =>
            Ok(Json.obj("claims" -> Json.toJson(ClaimsResponse.fromTpi01Response(responseDetail))))

          case Right(GetReimbursementClaimsResponse(responseCommon, None))                 =>
            BadRequest(Json.toJson(responseCommon))

          case Left(ErrorResponse(status, errorDetails)) =>
            if (status < 499)
              BadRequest(Json.toJson(errorDetails))
            else
              ServiceUnavailable(Json.toJson(errorDetails))
        }
        .recover {
          case ex if ex.getMessage.contains("JSON validation") =>
            logger.error(s"getClaims failed JSON validation: ${ex.getMessage}")
            BadRequest(ex.getMessage)

          case NonFatal(error) =>
            logger.error(s"getClaims failed: ${error.getClass}: ${error.getMessage}")
            ServiceUnavailable(error.getMessage)
        }
    }: AuthorisedActions.Input[AnyContent] => Future[Result])
}
