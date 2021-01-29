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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.cdsreimbursementclaim.models.GetDeclarationResponse._
import uk.gov.hmrc.cdsreimbursementclaim.models.MRN
import uk.gov.hmrc.cdsreimbursementclaim.services.DeclarationService
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging.LoggerOps
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton()
class GetDeclarationController @Inject() (declarationInfoService: DeclarationService, cc: ControllerComponents)(implicit
  ec: ExecutionContext
) extends BackendController(cc)
    with Logging {

  def declaration(declarationId: MRN): Action[AnyContent] = Action.async { implicit request =>
    declarationInfoService
      .getDeclaration(declarationId)
      .fold(
        e => {
          logger.warn(s"could not get declaration information", e)
          InternalServerError
        },
        declaration => Ok(Json.toJson(declaration))
      )
  }
}