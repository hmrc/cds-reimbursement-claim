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

import java.util.concurrent.{Executors, TimeUnit}

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import com.google.inject.{AbstractModule, Inject, Singleton}
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.cdsreimbursementclaim.config.AppConfig
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging

import scala.concurrent.Future

class SchedulerModule extends AbstractModule {
  override def configure(): Unit =
    bind(classOf[FileUploadScheduler]).asEagerSingleton()
}

@Singleton
class FileUploadScheduler @Inject() (
  actorSystem: ActorSystem,
  appConfig: AppConfig,
  fileUploadQueue: FileUploadQueue,
  applicationLifecycle: ApplicationLifecycle
) extends Logging {

  case object SendTheFiles

  class FileUploadActor extends Actor {
    @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Any"))
    def receive: Receive = { case SendTheFiles =>
      fileUploadQueue.processAllRequests()
      ()
    }
  }

  val bootstrap: Runnable       = new Runnable {
    override def run(): Unit = fileUploadActor ! SendTheFiles
  }
  val fileUploadActor: ActorRef = actorSystem.actorOf(Props(new FileUploadActor()))
  private val scheduledFuture   =
    Executors
      .newScheduledThreadPool(1)
      .scheduleAtFixedRate(
        bootstrap,
        appConfig.scheduleInitialDelay.toSeconds,
        appConfig.scheduleInterval.toSeconds,
        TimeUnit.SECONDS
      )
  logger.debug("Scheduler Started: " + scheduledFuture.toString)

  def shutDown(): Unit =
    fileUploadActor ! PoisonPill

  applicationLifecycle.addStopHook { () =>
    logger.debug("Scheduler shutting down")
    shutDown()
    Future.successful(())
  }

}
