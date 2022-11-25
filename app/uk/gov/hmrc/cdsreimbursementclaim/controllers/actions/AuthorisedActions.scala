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

package uk.gov.hmrc.cdsreimbursementclaim.controllers.actions

import cats.syntax.eq._
import com.google.inject.ImplementedBy
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori

@ImplementedBy(classOf[AuthorisedActionsBuilder])
trait AuthorisedActions extends ActionBuilder[AuthorisedActions.Input, AnyContent]

object AuthorisedActions {
  type Input[A] = (Request[A], Eori)
}

@Singleton
class AuthorisedActionsBuilder @Inject() (
  val authConnector: AuthConnector,
  val parser: BodyParsers.Default,
  val executionContext: ExecutionContext
) extends AuthorisedActions
    with AuthorisedFunctions
    with BackendHeaderCarrierProvider {

  override def invokeBlock[A](
    request: Request[A],
    block: AuthorisedActions.Input[A] => Future[Result]
  ): Future[Result] = {

    val carrier = hc(request)
    authorised(AuthProviders(GovernmentGateway))
      .retrieve(v2.Retrievals.allEnrolments) {
        _.getEnrolment("HMRC-CUS-ORG") match {
          case Some(enrolment) =>
            enrolment.identifiers.find(_.key === "EORINumber") match {
              case Some(EnrolmentIdentifier(_, eoriNumber)) =>
                block((request, Eori(eoriNumber)))

              case None =>
                Future.successful(
                  Results.Forbidden("Missing EORINumber identifier")
                )
            }

          case None =>
            Future.successful(
              Results.Forbidden("Missing HMRC-CUS-ORG enrolment")
            )
        }
      }(carrier, executionContext)
      .recover { case _: NoActiveSession =>
        Results.Forbidden("No active session found.")
      }(executionContext)
  }
}
