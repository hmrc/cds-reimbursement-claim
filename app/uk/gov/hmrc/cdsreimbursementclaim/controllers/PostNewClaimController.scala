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

package uk.gov.hmrc.cdsreimbursementclaim.controllers

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}

import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.cdsreimbursementclaim.config.AppConfig
import uk.gov.hmrc.cdsreimbursementclaim.utils
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton()
class PostNewClaimController @Inject() (appConfig:AppConfig, http: HttpClient, cc: ControllerComponents)
                                       (implicit ec: ExecutionContext)extends BackendController(cc) with utils.Logging{


  def claim():Action[JsValue] = Action.async(parse.json) { implicit request =>
    val dateFormat = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z").withZone(ZoneId.systemDefault())
    val localDate = LocalDateTime.now().format(dateFormat)

    val headers = Seq(("Date"-> localDate),
      ("X-Correlation-ID"->java.util.UUID.randomUUID().toString),
      ("X-Forwarded-Host"->"MDTP"),
      ("Content-Type"->"application/json"),
      ("Accept"->"application/json"))

    val hcWithExtraHeaders: HeaderCarrier = hc.copy(authorization = Some(Authorization(s"Bearer ${appConfig.eisBearerToken}")), extraHeaders = hc.extraHeaders ++ headers)
    http.POST[JsValue,HttpResponse](appConfig.newClaimEndpoint, request.body)(implicitly, implicitly, hcWithExtraHeaders, implicitly)
      .map(response => response.status match {
        case OK =>
          Ok(response.json)
        case s =>
          val message = s"PostNewClaimController response status: $s, body: ${response.body}"
          logger.error(message)
          BadRequest(message)
      })
  }
}
