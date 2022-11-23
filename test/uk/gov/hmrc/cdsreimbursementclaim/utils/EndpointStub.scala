package uk.gov.hmrc.cdsreimbursementclaim.utils

import com.typesafe.config.ConfigFactory
import play.api.http.Port
import play.api.inject.DefaultApplicationLifecycle
import play.api.mvc.{RequestHeader, Result}
import play.api.routing.Router
import play.api.test.WsTestClient
import play.api.{BuiltInComponents, BuiltInComponentsFromContext, _}
import play.core.server.{ServerConfig, ServerProvider}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditChannel, AuditConnector, DatastreamMetrics}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.util.matching.Regex

trait EndpointStub {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  def givenEndpointStub[A](
    routes: PartialFunction[RequestHeader, Result]
  )(block: Port => HttpClient => A)(implicit provider: ServerProvider): A = {

    val config: ServerConfig = ServerConfig(port = Some(0), mode = Mode.Test)

    val context: ApplicationLoader.Context = ApplicationLoader.Context(
      environment = Environment.simple(path = config.rootDir, mode = config.mode),
      initialConfiguration = Configuration(ConfigFactory.load()),
      lifecycle = new DefaultApplicationLifecycle,
      devContext = None
    )
    val application                        =
      (new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents { self: BuiltInComponents =>
        def router = Router.from(routes.andThen(r => self.defaultActionBuilder(_ => r)))
      }).application

    Play.start(application)

    val server = provider.createServer(config, application)

    try WsTestClient.withClient { wsClient =>
      val httpClient =
        new DefaultHttpClient(
          config.configuration,
          new HttpAuditing {
            val appName                                 = "test"
            val auditConnector                          = new AuditConnector {
              override def auditingConfig: AuditingConfig       = AuditingConfig.fromConfig(config.configuration)
              override def auditChannel: AuditChannel           = ???
              override def datastreamMetrics: DatastreamMetrics = ???
            }
            override def auditDisabledForPattern: Regex = ".+".r
          },
          wsClient,
          application.actorSystem
        )
      block(new Port(server.httpPort.orElse(server.httpsPort).get))(httpClient)
    } finally server.stop()
  }

}
