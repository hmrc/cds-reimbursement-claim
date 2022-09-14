/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.cdsreimbursementclaim.repositories.upscan

import cats.data.EitherT
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.upscan._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.cache.MongoCacheRepository
import uk.gov.hmrc.mongo.cache.CacheIdType
import uk.gov.hmrc.mongo.TimestampSupport
import uk.gov.hmrc.mongo.cache.DataKey

@ImplementedBy(classOf[DefaultUpscanRepository])
trait UpscanRepository {

  def insert(
    upscanUpload: UpscanUpload
  ): EitherT[Future, Error, Unit]

  def select(
    uploadReference: UploadReference
  ): EitherT[Future, Error, Option[UpscanUpload]]

  def update(
    uploadReference: UploadReference,
    upscanUpload: UpscanUpload
  ): EitherT[Future, Error, Unit]

}

@Singleton
class DefaultUpscanRepository @Inject() (
  mongoComponent: MongoComponent,
  timestampSupport: TimestampSupport,
  configuration: Configuration
)(implicit
  val ec: ExecutionContext
) extends MongoCacheRepository(
      mongoComponent = mongoComponent,
      collectionName = "upscan",
      ttl = configuration.get[FiniteDuration]("mongodb.upscan.expiry-time"),
      timestampSupport = timestampSupport,
      cacheIdType = CacheIdType.SimpleCacheId
    )
    with UpscanRepository {

  val dataKey: DataKey[UpscanUpload] = DataKey("upload")

  override def insert(
    upscanUpload: UpscanUpload
  ): EitherT[Future, Error, Unit] =
    EitherT(
      put(upscanUpload.uploadReference.value)(dataKey, upscanUpload)
        .map(_ => Right(()))
        .recover { case e ⇒ Left(Error(e)) }
    )

  override def select(
    uploadReference: UploadReference
  ): EitherT[Future, Error, Option[UpscanUpload]] =
    EitherT(
      get(uploadReference.value)(dataKey)
        .map(opt => Right(opt))
        .recover { case e ⇒ Left(Error(e)) }
    )

  override def update(
    uploadReference: UploadReference,
    upscanUpload: UpscanUpload
  ): EitherT[Future, Error, Unit] =
    EitherT(
      put(uploadReference.value)(dataKey, upscanUpload)
        .map(_ => Right(()))
        .recover { case e ⇒ Left(Error(e)) }
    )

}
