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

package uk.gov.hmrc.cdsreimbursementclaim.repositories.ccs

import cats.data.EitherT
import com.google.inject.ImplementedBy
import org.bson.types.ObjectId
import play.api.Configuration
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.services.ccs.CcsSubmissionRequest
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.workitem.ProcessingStatus
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.ResultStatus
import uk.gov.hmrc.mongo.workitem.WorkItem
import uk.gov.hmrc.mongo.workitem.WorkItemFields
import uk.gov.hmrc.mongo.workitem.WorkItemRepository
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.Mdc.preservingMdc

import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@ImplementedBy(classOf[DefaultCcsSubmissionRepo])
trait CcsSubmissionRepo {

  def set(ccsSubmissionRequest: CcsSubmissionRequest): EitherT[Future, Error, WorkItem[CcsSubmissionRequest]]

  def get: EitherT[Future, Error, Option[WorkItem[CcsSubmissionRequest]]]

  def setProcessingStatus(
    id: ObjectId,
    status: ProcessingStatus
  ): EitherT[Future, Error, Boolean]

  def setResultStatus(id: ObjectId, status: ResultStatus): EitherT[Future, Error, Boolean]
}

@Singleton
class DefaultCcsSubmissionRepo @Inject() (
  mongoComponent: MongoComponent,
  configuration: Configuration,
  servicesConfig: ServicesConfig
)(implicit ec: ExecutionContext)
    extends WorkItemRepository[CcsSubmissionRequest](
      collectionName = "ccs-submission-request-work-item",
      mongoComponent = mongoComponent,
      itemFormat = CcsSubmissionRequest.ccsSubmissionRequestFormat,
      workItemFields = WorkItemFields.default
    )
    with CcsSubmissionRepo {

  override def now(): Instant = Clock.systemUTC().instant()

  override val inProgressRetryAfter =
    Duration.ofMillis(configuration.get[Int]("ccs.submission-poller.in-progress-retry-after"))

  val retryPeriod: Int = inProgressRetryAfter.toMillis().intValue()

  override def set(ccsSubmissionRequest: CcsSubmissionRequest): EitherT[Future, Error, WorkItem[CcsSubmissionRequest]] =
    EitherT[Future, Error, WorkItem[CcsSubmissionRequest]](
      preservingMdc {
        pushNew(ccsSubmissionRequest, now(), (_: CcsSubmissionRequest) => ToDo).map(item => Right(item)).recover {
          case exception: Exception => Left(Error(exception))
        }
      }
    )

  override def get: EitherT[Future, Error, Option[WorkItem[CcsSubmissionRequest]]] =
    EitherT[Future, Error, Option[WorkItem[CcsSubmissionRequest]]](
      preservingMdc {
        super
          .pullOutstanding(failedBefore = now().minusMillis(retryPeriod), availableBefore = now())
          .map(workItem => Right(workItem))
          .recover { case exception: Exception =>
            Left(Error(exception))
          }
      }
    )

  override def setProcessingStatus(
    id: ObjectId,
    status: ProcessingStatus
  ): EitherT[Future, Error, Boolean] =
    EitherT[Future, Error, Boolean](
      preservingMdc {
        markAs(id, status, Some(now().plusMillis(retryPeriod)))
          .map(result => Right(result))
          .recover { case exception: Exception =>
            Left(Error(exception))
          }
      }
    )

  override def setResultStatus(id: ObjectId, status: ResultStatus): EitherT[Future, Error, Boolean] =
    EitherT[Future, Error, Boolean](
      preservingMdc {
        complete(id, status).map(result => Right(result)).recover { case exception: Exception =>
          Left(Error(exception))
        }
      }
    )
}
