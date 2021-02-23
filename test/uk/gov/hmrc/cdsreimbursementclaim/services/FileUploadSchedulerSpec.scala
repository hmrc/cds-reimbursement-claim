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

import akka.actor.ActorSystem
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.cdsreimbursementclaim.controllers.BaseSpec
import uk.gov.hmrc.cdsreimbursementclaim.models.{WorkItemHeaders, WorkItemPayload, WorkItemResult}
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.Future

class FileUploadSchedulerSpec extends BaseSpec with Eventually {

  val actorSystem          = ActorSystem.create()
  val uploadQueue          = mock[FileUploadQueue]
  val applicationLifecycle = new ApplicationLifecycle {
    override def addStopHook(hook: () => Future[_]): Unit = {}

    override def stop(): Future[_] = Future.successful(())
  }
  val fileUploadScheduler  = new FileUploadScheduler(actorSystem, appConfig, uploadQueue, applicationLifecycle)

  "Scheduler" should {
    "process requests while called once" in {
      val workItem = WorkItemResult(WorkItemPayload("soap", WorkItemHeaders(None, None)), Right(()))
      (uploadQueue.processAllRequests _).expects().returning(Future.successful(Seq(workItem))).once()

      fileUploadScheduler.fileUploadActor ! fileUploadScheduler.SendTheFiles

      eventually(Timeout(scaled(Span(2, Seconds))), Interval(scaled(Span(200, Millis)))) {
        true
      }
    }
  }

}
