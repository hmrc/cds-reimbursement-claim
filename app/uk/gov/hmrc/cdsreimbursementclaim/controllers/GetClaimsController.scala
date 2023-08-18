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
import uk.gov.hmrc.cdsreimbursementclaim.models.EisErrorResponse
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi01.{ClaimsResponse, ClaimsSelector, GetReimbursementClaimsResponse}
import uk.gov.hmrc.cdsreimbursementclaim.services.{GetClaimsService, GetXiEoriService}
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
  xiEoriService: GetXiEoriService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  final def getAllClaims(includeXiClaims: Boolean = false): Action[AnyContent]           =
    if (includeXiClaims) getGbAndXiClaims(ClaimsSelector.All) else getGbClaims(ClaimsSelector.All)
  final def getOverpaymentsClaims(includeXiClaims: Boolean = false): Action[AnyContent]  =
    if (includeXiClaims) getGbAndXiClaims(ClaimsSelector.Overpayments) else getGbClaims(ClaimsSelector.Overpayments)
  final def getNdrcClaims(includeXiClaims: Boolean = false): Action[AnyContent]          =
    if (includeXiClaims) getGbAndXiClaims(ClaimsSelector.Ndrc) else getGbClaims(ClaimsSelector.Ndrc)
  final def getSecuritiesClaims(includeXiClaims: Boolean = false): Action[AnyContent]    =
    if (includeXiClaims) getGbAndXiClaims(ClaimsSelector.Securities) else getGbClaims(ClaimsSelector.Securities)
  final def getUnderpaymentsClaims(includeXiClaims: Boolean = false): Action[AnyContent] =
    if (includeXiClaims) getGbAndXiClaims(ClaimsSelector.Underpayments) else getGbClaims(ClaimsSelector.Underpayments)

  private def wrapResponse(claimsResponse: ClaimsResponse): Result = Ok(
    Json.obj(
      "claims" -> Json.toJson(
        claimsResponse
      )
    )
  )

  private def getGbAndXiClaims(claimsSelector: ClaimsSelector): Action[AnyContent] =
    authorised.async({ case (r, eori) =>
      implicit val request: Request[AnyContent] = r
      val response                              = for {
        gbClaimsResponse <- service.getClaims(eori, claimsSelector)
        xiEori           <- xiEoriService.getXIEori(eori)
        xiClaimsResponse <- xiEori.traverse(service.getClaims(_, claimsSelector).map(_.toOption)).map(_.flatten)
        xiClaims          = xiClaimsResponse.flatMap(_.responseDetail)
      } yield (gbClaimsResponse, xiClaims) match {
        case (Right(GetReimbursementClaimsResponse(gbResponseCommon, gbResponseDetail)), xiResponseDetail) =>
          (gbResponseDetail, xiResponseDetail) match {
            case (Some(gbResponseDetail), Some(xiResponseDetail)) =>
              wrapResponse(
                ClaimsResponse.fromTpi01Response(gbResponseDetail) ++ ClaimsResponse.fromTpi01Response(xiResponseDetail)
              )
            case (Some(gbResponseDetail), None)                   =>
              wrapResponse(ClaimsResponse.fromTpi01Response(gbResponseDetail))
            case (None, Some(xiResponseDetail))                   =>
              wrapResponse(ClaimsResponse.fromTpi01Response(xiResponseDetail))
            case (None, None)                                     => BadRequest(Json.toJson(gbResponseCommon))
          }
        case (Left(EisErrorResponse(status, errorDetails, _)), _)                                          =>
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
            wrapResponse(ClaimsResponse.fromTpi01Response(responseDetail))

          case Right(GetReimbursementClaimsResponse(responseCommon, None)) =>
            BadRequest(Json.toJson(responseCommon))

          case Left(error @ EisErrorResponse(status, Some(errorDetails), _)) =>
            logger.error(error.getErrorDescriptionWithPrefix("A call to TPI01 API"))
            if (status < 499)
              BadRequest(Json.toJson(errorDetails))
            else
              ServiceUnavailable(Json.toJson(errorDetails))

          case Left(error @ EisErrorResponse(status, None, _)) =>
            logger.error(error.getErrorDescriptionWithPrefix("A call to TPI01 API"))
            if (status < 499)
              BadRequest
            else
              ServiceUnavailable
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
