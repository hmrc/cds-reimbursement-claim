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

import cats.implicits._
import cats.data._
import com.google.inject.{ImplementedBy, Inject, Singleton}
import configs.syntax._
import play.api.Configuration
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.upscan.{UpscanUpload, _}
import uk.gov.hmrc.cdsreimbursementclaim.repositories.upscan.UpscanRepository._
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import uk.gov.hmrc.mongo.cache.{CacheIdType, DataKey, MongoCacheRepository}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultUpscanRepository])
trait UpscanRepository {

  def insert(
    upscanUpload: UpscanUpload
  ): EitherT[Future, Error, Unit]

  def select(
    uploadReference: UploadReference
  ): EitherT[Future, Error, UpscanUpload]

  def update(
    uploadReference: UploadReference,
    upscanUpload: UpscanUpload
  ): EitherT[Future, Error, Unit]

  def selectAll(
    uploadReference: List[UploadReference]
  ): EitherT[Future, Error, List[UpscanUpload]]

}
object UpscanRepository {
  implicit val upscanCacheId: CacheIdType[UploadReference] =
    new CacheIdType[UploadReference] {
      def run: UploadReference => String = a => a.value
    }

}
//  val cacheTtl: FiniteDuration = config.underlying.get[FiniteDuration]("mongodb.upscan.expiry-time").value
@Singleton
class DefaultUpscanRepository @Inject() (
  mongoComponent: MongoComponent,
  timestampSupport: TimestampSupport,
  config: Configuration
)(implicit
  val ec: ExecutionContext
) extends MongoCacheRepository[UploadReference] (
      mongoComponent = mongoComponent,
      collectionName = "upscan",
      ttl = config.underlying.get[FiniteDuration]("mongodb.upscan.expiry-time").value,
      replaceIndexes = true,
      timestampSupport = timestampSupport,
      cacheIdType = upscanCacheId
      //UpscanUpload.format,
      //ReactiveMongoFormats.objectIdFormats
    )
    with UpscanRepository {

  //override val cacheTtlIndexName: String = "upscan-cache-ttl"

  //override val objName: String = "upscan"
  val objName: DataKey[UpscanUpload] = DataKey[UpscanUpload]("upscan")

  val cacheTtl: FiniteDuration = config.underlying.get[FiniteDuration]("mongodb.upscan.expiry-time").value

  override def insert(
    upscanUpload: UpscanUpload
  ): EitherT[Future, Error, Unit] =
    OptionT.liftF(
      put(upscanUpload.uploadReference)(objName, upscanUpload))
      .toRight( Error(s"failed to insert ${upscanUpload.uploadReference} into cache"))
      .map(_ => ())

  override def select(
    uploadReference: UploadReference
  ): EitherT[Future, Error, UpscanUpload] =
    EitherT(get(uploadReference)(objName)
      .map { a =>
        a.toRight(Error(s"failed to retrieve $uploadReference from cache"))
      })

  override def update(
    uploadReference: UploadReference,
    upscanUpload: UpscanUpload
  ): EitherT[Future, Error, Unit] =
    EitherT(delete(uploadReference)(objName)
      .flatMap { _ =>
        insert(upscanUpload).value
      })

  override def selectAll(
    uploadReferences: List[UploadReference]
  ): EitherT[Future, Error, List[UpscanUpload]] =
    uploadReferences
      .map { a => select(a)}
      .sequence
}
