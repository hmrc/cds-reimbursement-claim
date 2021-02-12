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
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Writes}
import uk.gov.hmrc.cdsreimbursementclaim.config.AppConfig
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultDeclarationConnector])
trait DeclarationConnector {
  def getDeclarationInfo(declarationInfo: JsValue)(implicit hc: HeaderCarrier): EitherT[Future, Error, HttpResponse]
}

@Singleton
class DefaultDeclarationConnector @Inject() (http: HttpClient, val appConfig: AppConfig)(implicit ec: ExecutionContext)
    extends DeclarationConnector
    with EisConnector {

  def getDeclarationInfo(declarationInfo: JsValue)(implicit hc: HeaderCarrier): EitherT[Future, Error, HttpResponse] =
    EitherT[Future, Error, HttpResponse](
      http
        .POST[JsValue, HttpResponse](appConfig.decInfoEndpoint, declarationInfo)(
          implicitly[Writes[JsValue]],
          HttpReads[HttpResponse],
          enrichHC(true),
          ec
        )
        .map(Right(_))
        .recover { case e => Left(Error(e)) }
    )

}
