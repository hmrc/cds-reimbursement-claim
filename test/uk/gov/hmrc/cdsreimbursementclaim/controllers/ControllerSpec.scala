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

import com.typesafe.config.ConfigFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.{Application, Configuration, Play}

import scala.reflect.ClassTag
import play.api.libs.json.JsValue
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.AuthenticatedUserRequest
import uk.gov.hmrc.cdsreimbursementclaim.Fake
import java.time.LocalDateTime
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import play.api.mvc.Headers
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.MimeTypes.JSON
import play.api.inject.bind
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.cdsreimbursementclaim.connectors.ClaimConnector
import uk.gov.hmrc.cdsreimbursementclaim.connectors.DeclarationConnector

trait ControllerSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with MockFactory {

  val overrideBindings: List[GuiceableModule] = List.empty[GuiceableModule]

  def buildApplication(
    authConnector: AuthConnector,
    declarationConnector: DeclarationConnector,
    claimConnector: ClaimConnector
  ): Application =
    new GuiceApplicationBuilder()
      .configure(
        Configuration(
          ConfigFactory.parseString(
            """
              | metrics.jvm = false
              | metrics.logback = false
              |
              | auditing {
              |  enabled = false
              |  traceRequests = false
              |  consumer {
              |    baseUri {
              |      host = localhost
              |      port = 8100
              |    }
              |  }
              |}
          """.stripMargin
          )
        )
      )
      .overrides(bind[AuthConnector].to(authConnector))
      .overrides(bind[ClaimConnector].to(claimConnector))
      .overrides(bind[DeclarationConnector].to(declarationConnector))
      .build()

  def buildFakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        Configuration(
          ConfigFactory.parseString(
            """
              | metrics.jvm = false
              | metrics.logback = false
              |
              | auditing {
              |  enabled = false
              |  traceRequests = false
              |  consumer {
              |    baseUri {
              |      host = localhost
              |      port = 8100
              |    }
              |  }
              |}
          """.stripMargin
          )
        )
      )
      .build()

  lazy val fakeApplication: Application = buildFakeApplication()

  def instanceOf[A : ClassTag]: A = fakeApplication.injector.instanceOf[A]

  abstract override def beforeAll(): Unit = {
    Play.start(fakeApplication)
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    Play.stop(fakeApplication)
    super.afterAll()
  }

  def fakeRequestWithJsonBody(body: JsValue) =
    new AuthenticatedUserRequest(
      Fake.user,
      LocalDateTime.now(),
      HeaderCarrier(),
      FakeRequest()
    ).withHeaders(Headers.apply(CONTENT_TYPE -> JSON)).withBody(body)

}
