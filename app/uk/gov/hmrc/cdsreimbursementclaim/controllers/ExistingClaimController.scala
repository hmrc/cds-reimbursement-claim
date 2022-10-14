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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.cdsreimbursementclaim.connectors.ExistingDeclarationConnector
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.AuthenticateActions
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging.LoggerOps
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import scala.concurrent.ExecutionContext

@Singleton
class ExistingClaimController @Inject() (
  authenticate: AuthenticateActions,
  existingDeclarationConnector: ExistingDeclarationConnector,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def claimExists(mrn: MRN, reasonForSecurity: ReasonForSecurity): Action[AnyContent] = authenticate.async {
    implicit request =>
      existingDeclarationConnector
        .checkExistingDeclaration(mrn, reasonForSecurity)
        .fold(
          error => {
            logger.warn(s"could not get existing declaration", error)
            InternalServerError
          },
          existingDeclaration => Ok(Json.toJson(existingDeclaration))
        )
  }
}
