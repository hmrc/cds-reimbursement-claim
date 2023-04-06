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
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request, Result}
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.AuthorisedActions
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi01.{ClaimsResponse, ClaimsSelector, ErrorResponse, GetReimbursementClaimsResponse}
import uk.gov.hmrc.cdsreimbursementclaim.services.{GetClaimsService, GetXIEoriService}
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import cats.implicits._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class GetClaimsController @Inject() (
  authorised: AuthorisedActions,
  service: GetClaimsService,
  xiEoriService: GetXIEoriService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  final val getAllGbClaims: Action[AnyContent]           = getGbClaims(ClaimsSelector.All)
  final val getOverpaymentsGbClaims: Action[AnyContent]  = getGbClaims(ClaimsSelector.Overpayments)
  final val getNdrcGbClaims: Action[AnyContent]          = getGbClaims(ClaimsSelector.Ndrc)
  final val getSecuritiesGbClaims: Action[AnyContent]    = getGbClaims(ClaimsSelector.Securities)
  final val getUnderpaymentsGbClaims: Action[AnyContent] = getGbClaims(ClaimsSelector.Underpayments)

  final val getAllGbAndXiClaims: Action[AnyContent] = getGbAndXiClaims(ClaimsSelector.All)
  final val getOverpaymentsGbAndXiClaims: Action[AnyContent] = getGbAndXiClaims(ClaimsSelector.Overpayments)
  final val getNdrcGbAndXiClaims: Action[AnyContent] = getGbAndXiClaims(ClaimsSelector.Ndrc)
  final val getSecuritiesGbAndXiClaims: Action[AnyContent] = getGbAndXiClaims(ClaimsSelector.Securities)
  final val getUnderpaymentsGbAndXiClaims: Action[AnyContent] = getGbAndXiClaims(ClaimsSelector.Underpayments)
  private def getGbAndXiClaims(claimsSelector: ClaimsSelector): Action[AnyContent] =
    authorised.async({ case (r, eori) =>
      implicit val request: Request[AnyContent] = r
      val response = for {
        gbClaimsResponse <- service.getClaims(eori, claimsSelector)
        xiEori <- xiEoriService.getXIEori(eori)
        xiClaimsResponse <- xiEori.traverse(service.getClaims(_, claimsSelector).map(_.toOption)).map(_.flatten)
        xiClaims = xiClaimsResponse.flatMap(_.responseDetail)
      } yield (gbClaimsResponse, xiClaims) match {
        case (Right(GetReimbursementClaimsResponse(gbResponseCommon, gbResponseDetail)), xiResponseDetail) =>
          (gbResponseDetail, xiResponseDetail) match {
            case (Some(gbResponseDetail), Some(xiResponseDetail)) => Ok(Json.obj("claims" -> Json.toJson(
              ClaimsResponse.fromTpi01Response(gbResponseDetail) ++
              ClaimsResponse.fromTpi01Response(xiResponseDetail)
            )))
            case (Some(gbResponseDetail), None) => Ok(Json.obj("claims" -> Json.toJson(
              ClaimsResponse.fromTpi01Response(gbResponseDetail)
            )))
            case (None, Some(xiResponseDetail)) => Ok(Json.obj("claims" -> Json.toJson(
              ClaimsResponse.fromTpi01Response(xiResponseDetail)
            )))
            case (None, None) => BadRequest(Json.toJson(gbResponseCommon))
          }
        case (Left(ErrorResponse(status, errorDetails)), _) =>
          if (status < 499)
            BadRequest(Json.toJson(errorDetails))
          else
            ServiceUnavailable(Json.toJson(errorDetails))
      }

      response.recover {
        case ex if ex.getMessage.contains("JSON validation") =>
          logger.error(s"getClaims failed JSON validation: ${ex.getMessage}")
          BadRequest(ex.getMessage)

        case NonFatal(error) =>
          logger.error(s"getClaims failed: ${error.getClass}: ${error.getMessage}")
          ServiceUnavailable(error.getMessage)
      }
    }: AuthorisedActions.Input[AnyContent] => Future[Result])


  private def getGbClaims(claimsSelector: ClaimsSelector): Action[AnyContent] =
    authorised.async({ case (r, eori) =>
      implicit val request: Request[AnyContent] = r
      service
        .getClaims(eori, claimsSelector)
        .map {
          case Right(GetReimbursementClaimsResponse(_, Some(responseDetail))) =>
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
