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

import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.libs.iteratee.{Enumerator, Iteratee}
import uk.gov.hmrc.cdsreimbursementclaim.config.AppConfig
import uk.gov.hmrc.cdsreimbursementclaim.models.{Dec64Body, HeadlessEnvelope, WorkItemHeaders, WorkItemPayload, WorkItemResult}
import uk.gov.hmrc.cdsreimbursementclaim.repositories.FileUploadsRepository
import uk.gov.hmrc.workitem.{Failed, PermanentlyFailed, WorkItem}
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging.LoggerOps
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileUploadQueue @Inject() (repository: FileUploadsRepository, fileUploadService: FileUploadService)(implicit
  appConfig: AppConfig,
  ec: ExecutionContext
) extends Logging {

  def queueRequests(dec64Bodies: Seq[Dec64Body])(implicit hc: HeaderCarrier): Future[Seq[WorkItem[WorkItemPayload]]] =
    Future.sequence(
      dec64Bodies
        .map(body => Dec64Body.soapEncoder.encode(HeadlessEnvelope(body)))
        .map(queueRequest(_))
    )

  def queueRequest(soapMessage: String)(implicit hc: HeaderCarrier): Future[WorkItem[WorkItemPayload]] = {
    val workItemPayload = new WorkItemPayload(soapMessage, WorkItemHeaders.apply(hc))
    repository.pushNew(workItemPayload, DateTime.now())
  }

  def processAllRequests(): Future[Seq[WorkItemResult]] = {
    val pullWorkItems: Enumerator[WorkItem[WorkItemPayload]] = Enumerator.generateM(repository.pullOutstanding)
    val processWorkItems                                     = Iteratee.foldM(Seq.empty[WorkItemResult])(processOneItem)
    pullWorkItems.run(processWorkItems)
  }

  protected def processOneItem(
    acc: Seq[WorkItemResult],
    workItem: WorkItem[WorkItemPayload]
  ): Future[Seq[WorkItemResult]] = {
    fileUploadService
      .upload(workItem.item.soapRequest)(workItem.item.headers.toHeaderCarrier)
      .fold(
        err => {
          logger.warn("Sending to Dec64 failed: ", err)
          (workItem.failureCount match {
            case x if x >= appConfig.queueMaxRetries =>
              logger.error(s"Dec64 Request ${workItem.id} permanently failed!")
              repository.markAs(workItem.id, PermanentlyFailed, None)
            case _                                   =>
              repository.markAs(workItem.id, Failed)
          }).map(_ => acc :+ WorkItemResult(workItem.item, Left(err)))
        },
        response => repository.complete(workItem.id).map(_ => acc :+ WorkItemResult(workItem.item, Right(response)))
      )
      .flatten
  }

}
