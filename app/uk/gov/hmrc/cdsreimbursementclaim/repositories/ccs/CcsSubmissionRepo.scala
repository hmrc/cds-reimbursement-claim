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

package uk.gov.hmrc.cdsreimbursementclaim.repositories.ccs

import cats.data.EitherT
import com.google.inject.ImplementedBy
import org.joda.time.DateTime
import play.api.Configuration
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.repositories.CacheRepository
import uk.gov.hmrc.cdsreimbursementclaim.services.ccs.CcsSubmissionRequest
import uk.gov.hmrc.cdsreimbursementclaim.utils.TimeUtils._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.Mdc.preservingMdc
import uk.gov.hmrc.workitem._

import java.time.Clock
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultCcsSubmissionRepo])
trait CcsSubmissionRepo {
  def set(ccsSubmissionRequest: CcsSubmissionRequest): EitherT[Future, Error, WorkItem[CcsSubmissionRequest]]
  def get: EitherT[Future, Error, Option[WorkItem[CcsSubmissionRequest]]]
  def setProcessingStatus(
    id: BSONObjectID,
    status: ProcessingStatus
  ): EitherT[Future, Error, Boolean]
  def setResultStatus(id: BSONObjectID, status: ResultStatus): EitherT[Future, Error, Boolean]
}

@Singleton
class DefaultCcsSubmissionRepo @Inject() (
  reactiveMongoComponent: ReactiveMongoComponent,
  configuration: Configuration,
  servicesConfig: ServicesConfig
)(implicit ec: ExecutionContext)
    extends WorkItemRepository[CcsSubmissionRequest, BSONObjectID](
      collectionName = "ccs-submission-request-work-item",
      mongo = reactiveMongoComponent.mongoConnector.db,
      itemFormat = CcsSubmissionRequest.workItemFormat,
      configuration.underlying
    )
    with CcsSubmissionRepo {

  override def now: DateTime = Clock.systemUTC().nowAsJoda

  override def workItemFields: WorkItemFieldNames =
    new WorkItemFieldNames {
      val receivedAt   = "receivedAt"
      val updatedAt    = "updatedAt"
      val availableAt  = "availableAt"
      val status       = "status"
      val id           = "_id"
      val failureCount = "failureCount"
    }

  override def inProgressRetryAfterProperty: String = "ccs.submission-poller.in-progress-retry-after"

  private lazy val ttl = servicesConfig.getDuration("ccs.submission-poller.mongo.ttl").toSeconds

  private val retryPeriod = inProgressRetryAfter.getMillis.toInt

  private val ttlIndexName: String = "receivedAtTime"

  private val ttlIndex: Index =
    Index(
      key = Seq("receivedAt" -> IndexType.Ascending),
      name = Some(ttlIndexName),
      options = BSONDocument("expireAfterSeconds" -> ttl)
    )

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] =
    for {
      result <- super.ensureIndexes(ExecutionContext.global)
      _      <- CacheRepository.setTtlIndex(ttlIndex, ttlIndexName, ttl.seconds, collection, logger)(ExecutionContext.global)
    } yield result

  override def set(ccsSubmissionRequest: CcsSubmissionRequest): EitherT[Future, Error, WorkItem[CcsSubmissionRequest]] =
    EitherT[Future, Error, WorkItem[CcsSubmissionRequest]](
      preservingMdc {
        pushNew(ccsSubmissionRequest, now, (_: CcsSubmissionRequest) => ToDo).map(item => Right(item)).recover {
          case exception: Exception => Left(Error(exception))
        }
      }
    )

  override def get: EitherT[Future, Error, Option[WorkItem[CcsSubmissionRequest]]] =
    EitherT[Future, Error, Option[WorkItem[CcsSubmissionRequest]]](
      preservingMdc {
        super
          .pullOutstanding(failedBefore = now.minusMillis(retryPeriod), availableBefore = now)
          .map(workItem => Right(workItem))
          .recover { case exception: Exception =>
            Left(Error(exception))
          }
      }
    )

  override def setProcessingStatus(
    id: BSONObjectID,
    status: ProcessingStatus
  ): EitherT[Future, Error, Boolean] =
    EitherT[Future, Error, Boolean](
      preservingMdc {
        markAs(id, status, Some(now.plusMillis(retryPeriod)))
          .map(result => Right(result))
          .recover { case exception: Exception =>
            Left(Error(exception))
          }
      }
    )

  override def setResultStatus(id: BSONObjectID, status: ResultStatus): EitherT[Future, Error, Boolean] =
    EitherT[Future, Error, Boolean](
      preservingMdc {
        complete(id, status).map(result => Right(result)).recover { case exception: Exception =>
          Left(Error(exception))
        }
      }
    )
}
