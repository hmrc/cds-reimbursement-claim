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

package uk.gov.hmrc.cdsreimbursementclaim.services.ccs

import akka.actor.{ActorSystem, Cancellable}
import cats.data.EitherT
import cats.syntax.eq._
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.http.Status
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.Ids.UUIDGenerator
import uk.gov.hmrc.cdsreimbursementclaim.models.ccs.CcsSubmissionPayload
import uk.gov.hmrc.cdsreimbursementclaim.services.ccs.CcsSubmissionPoller.OnCompleteHandler
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.workitem.{Failed, PermanentlyFailed, Succeeded, WorkItem}

import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random

@Singleton
class CcsSubmissionPoller @Inject() (
  actorSystem: ActorSystem,
  ccsSubmissionService: CcsSubmissionService,
  ccsSubmissionPollerExecutionContext: CcsSubmissionPollerExecutionContext,
  servicesConfig: ServicesConfig,
  onCompleteHandler: OnCompleteHandler,
  uuidGenerator: UUIDGenerator
)(implicit
  executionContext: CcsSubmissionPollerExecutionContext
) extends Logging {

  private val jitteredInitialDelay: FiniteDuration = FiniteDuration(
    servicesConfig.getDuration("ccs.submission-poller.initial-delay").toMillis,
    TimeUnit.MILLISECONDS
  ) + FiniteDuration(
    Random.nextInt((servicesConfig.getDuration("ccs.submission-poller.jitter-period").toMillis.toInt + 1)).toLong,
    TimeUnit.MILLISECONDS
  )

  private val pollerInterval: FiniteDuration =
    FiniteDuration(servicesConfig.getDuration("ccs.submission-poller.interval").toMillis, TimeUnit.MILLISECONDS)

  private val failureCountLimit: Int = servicesConfig.getInt("ccs.submission-poller.failure-count-limit")

  val _: Cancellable =
    actorSystem.scheduler.schedule(jitteredInitialDelay, pollerInterval)(poller())(ccsSubmissionPollerExecutionContext)

  def getLogMessage(workItem: WorkItem[CcsSubmissionRequest], stateIndicator: String): String =
    s"CCS File Submission poller: $stateIndicator:  work-item-id: ${workItem.id}, work-item-failure-count: ${workItem.failureCount}, " +
      s"work-item-status: ${workItem.status}, work-item-updatedAt : ${workItem.updatedAt}, " +
      s"work-item-eori: ${workItem.item} "

  def poller(): Unit = {
    val result: EitherT[Future, Error, Unit] = ccsSubmissionService.dequeue.semiflatMap {
      case Some(workItem) =>
        if (workItem.failureCount === failureCountLimit) {
          logger.warn(getLogMessage(workItem, "ccs submission work-item permanently failed"))
          val _ = ccsSubmissionService.setResultStatus(workItem.id, PermanentlyFailed)
          Future.successful(())
        } else {
          val id = uuidGenerator.nextId()
          logger.info(getLogMessage(workItem, s"processing ccs submission work-item with id $id"))

          //TODO: reconstruct the headercarrier
          implicit val hc: HeaderCarrier = HeaderCarrier()

          ccsSubmissionService
            .submitToCcs(
              CcsSubmissionPayload(
                workItem.item.payload,
                workItem.item.headers
              )
            )
            .fold(
              { error =>
                logger.warn(getLogMessage(workItem, s"work-item ccs submission failed with error: $error"))
                val _ = ccsSubmissionService.setProcessingStatus(workItem.id, Failed)
              },
              httpResponse =>
                if (httpResponse.status === Status.NO_CONTENT) {
                  logger.info(getLogMessage(workItem, s"work-item ccs submission succeeded: $httpResponse"))
                  val _ = ccsSubmissionService.setResultStatus(workItem.id, Succeeded)
                } else {
                  logger.info(getLogMessage(workItem, s"work-item ccs submission failed: $httpResponse"))
                  val _ = ccsSubmissionService.setResultStatus(workItem.id, Failed)
                }
            )
        }

      case None =>
        logger.info("CCS File Submission poller: no work items")
        Future.successful(())
    }

    result.value.onComplete(_ => onCompleteHandler.onComplete())
  }

}

object CcsSubmissionPoller {

  @ImplementedBy(classOf[DefaultOnCompleteHandler])
  trait OnCompleteHandler {
    def onComplete(): Unit
  }

  @Singleton
  class DefaultOnCompleteHandler extends OnCompleteHandler {
    override def onComplete(): Unit = ()
  }

}
