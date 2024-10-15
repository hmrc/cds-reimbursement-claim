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

import play.api.libs.json.{JsNull, JsString, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request, Result, Results}
import uk.gov.hmrc.cdsreimbursementclaim.connectors.SubscriptionConnector
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.AuthorisedActions
import uk.gov.hmrc.cdsreimbursementclaim.models.sub09.{SubscriptionDisplayResponse, SubscriptionResponse}
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class GetEoriDetailsController @Inject() (
  authorised: AuthorisedActions,
  connector: SubscriptionConnector,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  final val getEoriDetails: Action[AnyContent] =
    authorised.async({ case (r, eori) =>
      implicit val request: Request[AnyContent] = r
      connector
        .getSubscription(eori)
        .map {
          case Right(Some(SubscriptionResponse(SubscriptionDisplayResponse(_, Some(details))))) =>
            details.EORINo match {
              case Some(subscriptionEori) =>
                Results.Ok(
                  Json.obj(
                    "eoriGB"      -> JsString(subscriptionEori.value),
                    "eoriXI"      -> (details.XI_Subscription match {
                      case Some(s) => JsString(s.XI_EORINo)
                      case None    => JsNull
                    }),
                    "fullName"    -> details.CDSFullName,
                    "eoriEndDate" -> (details.EORIEndDate match {
                      case Some(date) => JsString(date)
                      case None       => JsNull
                    })
                  )
                )
              case None                   =>
                Results.NoContent
            }

          case Left(error) =>
            logger.error(error)
            Results.NoContent

          case _ =>
            Results.NoContent
        }
        .recover { case NonFatal(error) =>
          logger.error(s"getXiEori failed: ${error.getClass}: ${error.getMessage}")
          Results.NoContent
        }
    }: AuthorisedActions.Input[AnyContent] => Future[Result])
}
