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

package uk.gov.hmrc.cdsreimbursementclaim.routes

import akka.stream.Materializer
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.{BodyParsers, Result}
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.cdsreimbursementclaim.Fake
import uk.gov.hmrc.cdsreimbursementclaim.controllers.ControllerSpec
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.{AuthenticateActionBuilder, AuthenticateActions}
import uk.gov.hmrc.cdsreimbursementclaim.services.UpscanService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class UpscanRoutesSpec extends AnyWordSpec with ControllerSpec {

  val mockUpscanService: UpscanService = mock[UpscanService]
  val fixedTimestamp: LocalDateTime    = LocalDateTime.of(2019, 9, 24, 15, 47, 20)

  val executionContext: ExecutionContextExecutor = ExecutionContext.global

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  val builder = new AuthenticateActionBuilder(
    mockAuthConnector,
    new BodyParsers.Default()(NoMaterializer),
    executionContext
  )

  override val overrideBindings: List[GuiceableModule] =
    List(
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AuthenticateActions].toInstance(Fake.login(Fake.user, fixedTimestamp)),
      bind[UpscanService].toInstance(mockUpscanService)
    )

  implicit lazy val mat: Materializer = fakeApplication.materializer

  val headerCarrier: HeaderCarrier = HeaderCarrier()

  "respond to the index Action" in {
    val uuid                                = UUID.randomUUID().toString
    val mayBeResult: Option[Future[Result]] =
      route(fakeApplication, FakeRequest("GET", s"/cds-reimbursement-claim/upscan/upload-reference/$uuid"))

    //TODO; finish off
    mayBeResult.map { result =>
      1 == 1
    }

  }

}
