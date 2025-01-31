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

package uk.gov.hmrc.cdsreimbursementclaim.utils

import com.typesafe.config.ConfigFactory
import play.api.http.Port
import play.api.inject.DefaultApplicationLifecycle
import play.api.mvc.{RequestHeader, Result}
import play.api.routing.Router
import play.api.test.WsTestClient
import play.api.*
import play.core.server.{ServerConfig, ServerProvider}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditChannel, AuditConnector, DatastreamMetrics}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.util.matching.Regex
import play.api.mvc.Request
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.client.{HttpClientV2, HttpClientV2Impl}

import scala.collection.immutable.Seq

/** Provides method to stub an external endpoint with the expected result.
  */
trait EndpointStub {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  @SuppressWarnings(
    Array(
      "org.wartremover.warts.OptionPartial",
      "org.wartremover.warts.TripleQuestionMark"
    )
  )
  final def givenEndpointStub[A](
    routes: PartialFunction[RequestHeader, Result]
  )(
    validateRequest: Request[AnyContent] => Unit = _ => ()
  )(block: Port => HttpClientV2 => A)(implicit provider: ServerProvider): A = {

    val config: ServerConfig = ServerConfig(port = Some(0), mode = Mode.Test)

    val context: ApplicationLoader.Context = ApplicationLoader.Context(
      environment = Environment.simple(path = config.rootDir, mode = config.mode),
      initialConfiguration = Configuration(ConfigFactory.load()),
      lifecycle = new DefaultApplicationLifecycle,
      devContext = None
    )
    val application                        =
      (new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents { self: BuiltInComponents =>
        def router = Router.from(
          routes.andThen(result =>
            self.defaultActionBuilder { request =>
              validateRequest(request)
              result
            }
          )
        )
      }).application

    Play.start(application)

    val server = provider.createServer(config, application)

    val httpAuditing = new HttpAuditing {
      val appName        = "test"
      val auditConnector = new AuditConnector {
        override def auditingConfig: AuditingConfig = AuditingConfig.fromConfig(config.configuration)

        override def auditChannel: AuditChannel = ???

        override def datastreamMetrics: DatastreamMetrics = ???
      }

      override def auditDisabledForPattern: Regex = ".+".r
    }

    try
      WsTestClient.withClient { wsClient =>
        val httpClient =
          new HttpClientV2Impl(
            wsClient,
            application.actorSystem,
            config.configuration,
            hooks = Seq.empty
          )
        block(new Port(server.httpPort.orElse(server.httpsPort).get))(httpClient)
      }
    finally server.stop()
  }

}
