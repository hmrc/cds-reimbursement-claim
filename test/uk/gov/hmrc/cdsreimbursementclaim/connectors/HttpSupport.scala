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

package uk.gov.hmrc.cdsreimbursementclaim.connectors

import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

trait HttpSupport { this: MockFactory with Matchers =>

  val mockHttp: HttpClient = mock[HttpClient]

  def mockPost[A](url: String, headers: immutable.Seq[(String, String)], body: A)(result: Option[HttpResponse]) =
    (mockHttp
      .POST(_: String, _: A, _: immutable.Seq[(String, String)])(
        _: Writes[A],
        _: HttpReads[HttpResponse],
        _: HeaderCarrier,
        _: ExecutionContext
      ))
      .expects(url, body, headers, *, *, *, *)
      .returning(
        result.fold[Future[HttpResponse]](Future.failed(new Exception("Test exception message")))(Future.successful)
      )

  def mockPostObject[I, O](url: String, headers: immutable.Seq[(String, String)], body: I)(result: Option[O]) =
    (mockHttp
      .POST(_: String, _: I, _: Seq[(String, String)])(
        _: Writes[I],
        _: HttpReads[O],
        _: HeaderCarrier,
        _: ExecutionContext
      ))
      .expects(url, body, headers, *, *, *, *)
      .returning(
        result.fold[Future[O]](Future.failed(new Exception("Test exception message")))(Future.successful)
      )

  def mockPostString[A](url: String, headers: immutable.Seq[(String, String)], body: String)(
    result: Option[HttpResponse]
  ) =
    (mockHttp
      .POSTString(_: String, _: String, _: Seq[(String, String)])(
        _: HttpReads[HttpResponse],
        _: HeaderCarrier,
        _: ExecutionContext
      ))
      .expects(url, body, headers, *, *, *)
      .returning(
        result.fold[Future[HttpResponse]](Future.failed(new Exception("Test exception message")))(Future.successful)
      )

}
