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

package uk.gov.hmrc.cdsreimbursementclaim.services

import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status._
import play.api.libs.json._
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.cdsreimbursementclaim.config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class EisServiceSpec extends AnyWordSpec with Matchers with MockFactory with DefaultAwaitTimeout with FutureAwaits {

  val env: Environment              = Environment.simple()
  val configuration: Configuration  = Configuration.load(env)
  val serviceConfig: ServicesConfig = new ServicesConfig(configuration)
  val appConfig: AppConfig          = new AppConfig(configuration, serviceConfig)

  val httpClient  = mock[HttpClient]
  val service     = new EisServiceImpl(appConfig, httpClient)
  implicit val hc = HeaderCarrier()

  "Eis Service" should {
    "return http 200" in {
      val response = JsObject(Seq("hello" -> JsString("word")))
      (
        httpClient
          .POST[JsValue, HttpResponse](_: String, _: JsValue, _: Seq[(String, String)])(
            _: Writes[JsValue],
            _: HttpReads[HttpResponse],
            _: HeaderCarrier,
            _: ExecutionContext
          )
        )
        .expects(*, *, *, *, *, *, *)
        .returning(Future.successful(HttpResponse(OK, response, Map[String, Seq[String]]().empty)))

      val result       = await(service.submitClaim(JsString("The Claim")).value)
      //val httpResponse = result.right.get      //Wart remover error
      val httpResponse = result.getOrElse(null) //TODO This is bad
      httpResponse.status shouldBe OK
      httpResponse.json   shouldBe response
    }

    "return http 400" in {
      val response = JsObject(Seq("hello" -> JsString("word")))
      (
        httpClient
          .POST[JsValue, HttpResponse](_: String, _: JsValue, _: Seq[(String, String)])(
            _: Writes[JsValue],
            _: HttpReads[HttpResponse],
            _: HeaderCarrier,
            _: ExecutionContext
          )
        )
        .expects(*, *, *, *, *, *, *)
        .returning(Future.successful(HttpResponse(BAD_REQUEST, response, Map[String, Seq[String]]().empty)))

      val result       = await(service.submitClaim(JsString("The Claim")).value)
      //val httpResponse = result.right.get       //Wart remover error
      val httpResponse = result.getOrElse(null) //TODO This is bad
      httpResponse.status shouldBe BAD_REQUEST
      httpResponse.json   shouldBe response
    }

  }
}
