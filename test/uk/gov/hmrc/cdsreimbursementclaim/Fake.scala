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

package uk.gov.hmrc.cdsreimbursementclaim

import play.api.mvc._
import play.api.test.Helpers
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.{AuthenticateWithUserActions, AuthenticatedUser, AuthenticatedUserRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.AuthorisedActions

object Fake {

  @SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext"))
  def login(
    user: AuthenticatedUser,
    timestamp: LocalDateTime,
    hc: HeaderCarrier = HeaderCarrier()
  ): AuthenticateWithUserActions =
    new AuthenticateWithUserActions {
      override def parser: BodyParser[AnyContent] = Helpers.stubBodyParser()

      override def invokeBlock[A](
        request: Request[A],
        block: AuthenticatedUserRequest[A] => Future[Result]
      ): Future[Result] =
        block(new AuthenticatedUserRequest(user, timestamp, hc, request))

      override protected def executionContext: ExecutionContext = ExecutionContext.global
    }

  val user: AuthenticatedUser = AuthenticatedUser("ggCredId", Some("name"), Some(Email("email@example.com")))

  def login(eori: Eori): AuthorisedActions =
    new AuthorisedActions {
      override def parser: BodyParser[AnyContent] = Helpers.stubBodyParser()

      override def invokeBlock[A](
        request: Request[A],
        block: AuthorisedActions.Input[A] => Future[Result]
      ): Future[Result] =
        block((request, eori))

      override protected def executionContext: ExecutionContext = ExecutionContext.global
    }

}
