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

package uk.gov.hmrc.cdsreimbursementclaim.connectors

import cats.data.EitherT
import com.google.inject.ImplementedBy
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultSubmitClaimConnector])
trait SubmitClaimConnector {

  def submitClaim(claimData: JsValue)(implicit hc: HeaderCarrier): EitherT[Future, Error, HttpResponse]

}

class DefaultSubmitClaimConnector @Inject() (http: HttpClient, val config: ServicesConfig)(implicit
  ec: ExecutionContext
) extends SubmitClaimConnector
    with EisConnector {

  val baseUrl: String = config.baseUrl("eis")

  override def submitClaim(claimData: JsValue)(implicit hc: HeaderCarrier): EitherT[Future, Error, HttpResponse] = {
    val submitClaimUrl: String = "" //TODO: add url here

    EitherT[Future, Error, HttpResponse]( //TODO: take this comment out - this lifts the future to this monad context - http exceptions get mapped to our Error (don't use Throwable anywhere in the code base)
      http
        .POST[JsValue, HttpResponse](submitClaimUrl, Json.toJson(claimData), headers)(
          implicitly[Writes[JsValue]],
          HttpReads[HttpResponse],
          hc.copy(authorization = None),
          ec
        )
        .map(Right(_))
        .recover { case e => Left(Error(e)) }
    )
  }

}
