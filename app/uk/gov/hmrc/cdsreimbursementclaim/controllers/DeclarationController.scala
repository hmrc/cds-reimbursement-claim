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
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.AuthenticateActions
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.GetDeclarationError
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.services.DeclarationService
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging.LoggerOps
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class DeclarationController @Inject() (
  authenticate: AuthenticateActions,
  declarationService: DeclarationService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def declaration(mrn: MRN): Action[AnyContent] = authenticate.async { implicit request =>
    declarationService
      .getDeclaration(mrn)
      .fold(
        e => {
          logger.warn(s"could not get declaration", e)
          InternalServerError
        },
        maybeDisplayDeclaration =>
          maybeDisplayDeclaration.fold {
            logger.info(s"received no declaration information for ${mrn.value}")
            NoContent
          }(declaration => Ok(Json.toJson(declaration)))
      )
  }

  def declarationWithReasonForSecurity(mrn: MRN, reasonForSecurity: ReasonForSecurity): Action[AnyContent] =
    authenticate.async { implicit request =>
      declarationService
        .getDeclarationWithErrorCodes(mrn, Some(reasonForSecurity.acc14Code))
        .fold(
          (e: GetDeclarationError) =>
            e match {
              case GetDeclarationError.invalidReasonForSecurity     => BadRequest(Json.toJson(e))
              case GetDeclarationError.declarationNotFound          => BadRequest(Json.toJson(e))
              case _                                                => InternalServerError(Json.toJson(e))
            },
          (declaration: DisplayDeclaration) => {
            val acc14SecurityReason: Option[String] = declaration.displayResponseDetail.securityReason
            val hasCorrectRfs                       = acc14SecurityReason.contains(reasonForSecurity.acc14Code)
            val hasCorrectMrn = mrn.value.equals(MRN(declaration.displayResponseDetail.declarationId).value)
            if (!hasCorrectRfs) {
              logger.error( // this can happen if the user selects the wrong reason for security in the radio list,
                // do we need to log this?
                s"[strange] declaration for ${mrn.value} have returned with security reason [${acc14SecurityReason
                  .getOrElse("<none>")}] but the query was for [${reasonForSecurity.acc14Code}], returning none to the caller"
              )
              BadRequest(
                Json.toJson(
                  GetDeclarationError.mismatchRfs
                )
              )
            } else if (!hasCorrectMrn) {
              BadRequest(
                Json.toJson(
                  GetDeclarationError.mismatchMrn
                )
              )
            }else {
              Ok(Json.toJson(declaration))
            }

            
          }
        )
    }
}
