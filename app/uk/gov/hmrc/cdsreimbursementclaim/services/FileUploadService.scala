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

import cats.data.EitherT
import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.http.Status._
import uk.gov.hmrc.cdsreimbursementclaim.connectors.FileUploadConnector
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[FileUploadServiceImpl])
trait FileUploadService {
  def upload(body: String)(implicit hc: HeaderCarrier): EitherT[Future, Error, Unit]
}

@Singleton
class FileUploadServiceImpl @Inject() (uploadConnector: FileUploadConnector)(implicit ec: ExecutionContext)
    extends FileUploadService
    with Logging {

  def upload(dec64: String)(implicit hc: HeaderCarrier): EitherT[Future, Error, Unit] =
    uploadConnector
      .upload(dec64)
      .subflatMap { httpResponse =>
        httpResponse.status match {
          case NO_CONTENT =>
            Right(())
          case _          =>
            Left(
              Error(
                s"Call to submit claim came back with status ${httpResponse.status}, body: ${httpResponse.body}"
              )
            )
        }
      }
}
