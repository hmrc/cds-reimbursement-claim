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

///*
// * Copyright 2021 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.cdsreimbursementclaim.services
//
//import akka.actor.ActorSystem
//import com.google.inject.{Inject, Singleton}
//import uk.gov.hmrc.cdsreimbursementclaim.config.AppConfig
//import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
//import uk.gov.hmrc.play.scheduling.ExclusiveScheduledJob
//
//import scala.concurrent.duration._
//import scala.concurrent.{ExecutionContext, Future}
//import scala.util.{Failure, Success}
//
//@Singleton
//class FileUploadScheduler @Inject() (actorSystem: ActorSystem, appConfig: AppConfig, fileUploadQueue: FileUploadQueue)(
//  implicit ec: ExecutionContext
//) extends ExclusiveScheduledJob
//    with Logging {
//
//  override def name: String = "FileUploadScheduler"
//
//  println("================================= " + name)
//
//  override def executeInMutex(implicit ec: ExecutionContext): Future[Result] =
//    fileUploadQueue.processAllRequests().map(items => Result(s"Processed ${items.size} file uploads"))
//
//  lazy val initialDelay: FiniteDuration = appConfig.scheduleInitialDelay
//  lazy val interval: FiniteDuration     = appConfig.scheduleInterval
//
//  actorSystem.scheduler.schedule(initialDelay, interval) {
//    execute.onComplete({
//      case Success(Result(message)) => logger.info(message)
//      case Failure(throwable)       => logger.error(s"Exception completing work item", throwable)
//    })
//  }
//}
