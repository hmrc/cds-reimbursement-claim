package uk.gov.hmrc.cdsreimbursementclaim.connectors

import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

trait HttpSupport { this: MockFactory with Matchers ⇒

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val mockHttp: HttpClient = mock[HttpClient]

  def mockPost[A](url: String, headers: Seq[(String, String)], body: A)(result: Option[HttpResponse]): Unit =
    (mockHttp
      .POST(_: String, _: A, _: Seq[(String, String)])(
        _: Writes[A],
        _: HttpReads[HttpResponse],
        _: HeaderCarrier,
        _: ExecutionContext
      ))
      .expects(url, body, headers, *, *, *, *)
      .returning(
        result.fold[Future[HttpResponse]](Future.failed(new Exception("Test exception message")))(Future.successful)
      )

}
