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

package uk.gov.hmrc.cdsreimbursementclaim.repositories.ccs

import cats.data._
import cats.implicits._
import com.google.inject.ImplementedBy
import configs.syntax._
import org.mongodb.scala.bson.ObjectId
import play.api.Configuration
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import uk.gov.hmrc.mongo.cache.{CacheIdType, CacheItem, DataKey, MongoCacheRepository}

import scala.concurrent.duration.FiniteDuration
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.services.ccs.CcsSubmissionRequest
import uk.gov.hmrc.workitem._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultCcsSubmissionRepo])
trait CcsSubmissionRepo {
  def set(ccsSubmissionRequest: CcsSubmissionRequest): EitherT[Future, Error, CacheItem]
  def get: EitherT[Future, Error, Option[CcsSubmissionRequest]]
  def setProcessingStatus(
    id: ObjectId,
    status: ProcessingStatus
  ): EitherT[Future, Error, Boolean]
  def setResultStatus(id: ObjectId, status: ResultStatus): EitherT[Future, Error, Boolean]
}
object CcsSubmissionRepo {
  implicit val ccsSubmissionCacheId: CacheIdType[ObjectId] =
    new CacheIdType[ObjectId] {
      def run: ObjectId => String = a => a.toString
    }
}

@Singleton
class DefaultCcsSubmissionRepo @Inject() (
  mongoComponent: MongoComponent,
  timestampSupport: TimestampSupport,
  config: Configuration
)(implicit ec: ExecutionContext)
  extends MongoCacheRepository[ObjectId] (
    mongoComponent = mongoComponent,
    collectionName = "ccs-submission-request",
    ttl = config.underlying.get[FiniteDuration]("mongodb.work-item.expiry-time").value,
    replaceIndexes = true,
    timestampSupport = timestampSupport,
    cacheIdType      = CcsSubmissionRepo.ccsSubmissionCacheId
    )
    with CcsSubmissionRepo {

  val dataKey: DataKey[CcsSubmissionRequest] = DataKey[CcsSubmissionRequest]("ccs-submission-request")

  lazy val inProgressRetryAfter: FiniteDuration = config.underlying.get[FiniteDuration]("ccs.submission-poller.in-progress-retry-after").value

  //private lazy val ttl = servicesConfig.getDuration("ccs.submission-poller.mongo.ttl").toSeconds

  //private val retryPeriod = inProgressRetryAfter.getMillis.toInt

  override def set(ccsSubmissionRequest: CcsSubmissionRequest): EitherT[Future, Error, CacheItem] =
    OptionT.liftF(
      put(new ObjectId())(dataKey, ccsSubmissionRequest))
      .toRight(Error(s"failed to insert ${ccsSubmissionRequest.toString} into cache"))

  override def get: EitherT[Future, Error, Option[CcsSubmissionRequest]] = ???
/*    EitherT[Future, Error, Option[WorkItem[CcsSubmissionRequest]]](
      preservingMdc {
        super
          .pullOutstanding(failedBefore = now.minusMillis(retryPeriod), availableBefore = now)
          .map(workItem => Right(workItem))
          .recover { case exception: Exception =>
            Left(Error(exception))
          }
      }
    )
  }*/

  override def setProcessingStatus(
    id: ObjectId,
    status: ProcessingStatus
  ): EitherT[Future, Error, Boolean] = ???
/*    EitherT[Future, Error, Boolean](
      preservingMdc {
        markAs(id, status, Some(now.plusMillis(retryPeriod)))
          .map(result => Right(result))
          .recover { case exception: Exception =>
            Left(Error(exception))
          }
      }
    )
*/
  override def setResultStatus(id: ObjectId, status: ResultStatus): EitherT[Future, Error, Boolean] = ???
 /*   EitherT[Future, Error, Boolean](
      preservingMdc {
        complete(id, status).map(result => Right(result)).recover { case exception: Exception =>
          Left(Error(exception))
        }
      }
    )*/
}
