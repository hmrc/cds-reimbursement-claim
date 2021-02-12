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

//package uk.gov.hmrc.cdsreimbursementclaim.repositories
//
//import org.scalamock.scalatest.MockFactory
//import org.scalatest.matchers.should.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import play.api.inject.DefaultApplicationLifecycle
//import play.modules.reactivemongo.ReactiveMongoComponentImpl
//import reactivemongo.bson.BSONObjectID
//import uk.gov.hmrc.cdsreimbursementclaim.controllers.BaseSpec
//import uk.gov.hmrc.cdsreimbursementclaim.models.WorkItemPayload
//import uk.gov.hmrc.http.HeaderCarrier
//import uk.gov.hmrc.time.DateTimeUtils._
//import uk.gov.hmrc.workitem.{Failed, PermanentlyFailed, ToDo, WorkItem}
//
//import scala.concurrent.duration._
//import scala.concurrent.{Await, ExecutionContext, Future}
//
//class FileUploadsRepositorySpec extends BaseSpec  {
//
////  trait TestSetup {
////    val repository = mock[AssessRequestsQueueRepository]
////    val service = mock[BankAccountReputationService]
////    val metrics = mock[AssessmentMetrics]
////    val appConfig = mock[AppConfig]
////    when(appConfig.maxRetries).thenReturn(5)
////    val fixture = new AssessQueueService(repository, service, metrics, appConfig)
////    val payload = anAssessmentPayload
////    val workItem = WorkItem(null, now, now, now, ToDo, 0, payload)
////  }
//
//  trait TestSetup {
//    val lifeCycle = new DefaultApplicationLifecycle()
//  val reactiveMongoComponent = new ReactiveMongoComponentImpl(config,env, lifeCycle)
//  val repository = new FileUploadsRepository(config,reactiveMongoComponent)
//  }
//
//
//  "queue" should {
//    "enqueue a request" in new TestSetup {
//val payload = WorkItemPayload("TheData")
//      repository.pushNew(payload)
//
//      when(repository.pushNew(same(payload), any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(workItem))
//
//      val result = Await.result(fixture.queueRequest(payload), 5 seconds)
//      result shouldBe true
//
//      verify(metrics, times(1)).enqueued()
//    }
//
////    "dequeue a request successfully" in new TestSetup {
////
////      val outcome = new AssessmentOutcome(false, Some(AssessmentResponse(accountNumberWithSortCodeIsValid = true, "yes", "yes", "yes", "no", "no", "no")))
////
////      when(repository.pullOutstanding(any[ExecutionContext])).thenReturn(Future.successful(Some(workItem)), Future.successful(None))
////      when(repository.complete(any[BSONObjectID])(any[ExecutionContext])).thenReturn(Future.successful(true))
////      when(service.assess(any[AssessmentRequest])(any[HeaderCarrier])).thenReturn(Future.successful(outcome))
////
////      val result = Await.result(fixture.processAllRequests, 5 seconds)
////      result shouldBe Seq(AssessmentReport(payload, outcome))
////
////      val requestCaptor = ArgumentCaptor.forClass(classOf[AssessmentRequest])
////      val headerCaptor = ArgumentCaptor.forClass(classOf[HeaderCarrier])
////      verify(service, times(1)).assess(requestCaptor.capture())(headerCaptor.capture())
////
////      val assessmentRequest: AssessmentRequest = requestCaptor.getValue
////      assessmentRequest shouldBe payload.request
////
////      val headerCarrier: HeaderCarrier = headerCaptor.getValue
////      headerCarrier.requestId.map(_.value) shouldBe payload.headers.requestId
////      headerCarrier.sessionId.map(_.value) shouldBe payload.headers.sessionId
////
////
////      verify(metrics, times(1)).dequeued()
////      verify(metrics, times(1)).passed()
////    }
////
////    "dequeue a request that needs to be retried" in new TestSetup {
////
////      val outcome = new AssessmentOutcome(true, Some(AssessmentResponse(accountNumberWithSortCodeIsValid = false, "no", "yes", "yes", "no", "no", "no")))
////
////      when(repository.pullOutstanding(any[ExecutionContext])).thenReturn(Future.successful(Some(workItem)), Future.successful(None))
////      when(repository.markAs(any[BSONObjectID], same(Failed), same(None))(any[ExecutionContext])).thenReturn(Future.successful(true))
////      when(service.assess(any[AssessmentRequest])(any[HeaderCarrier])).thenReturn(Future.successful(outcome))
////
////      val result = Await.result(fixture.processAllRequests, 5 seconds)
////      result shouldBe Seq(AssessmentReport(payload, outcome))
////
////      verify(metrics, times(1)).dequeued()
////      verify(metrics, times(1)).repeated()
////    }
////
////    "no longer retry forever" in new TestSetup {
////
////      val outcome = new AssessmentOutcome(true, Some(AssessmentResponse(accountNumberWithSortCodeIsValid = false, "no", "yes", "yes", "no", "no", "no")))
////
////      when(repository.pullOutstanding(any[ExecutionContext])).thenReturn(Future.successful(Some(workItem.copy(failureCount = 10))), Future.successful(None))
////      when(repository.markAs(any[BSONObjectID], same(PermanentlyFailed), same(None))(any[ExecutionContext])).thenReturn(Future.successful(true))
////      when(service.assess(any[AssessmentRequest])(any[HeaderCarrier])).thenReturn(Future.successful(outcome))
////
////      val result = Await.result(fixture.processAllRequests, 5 seconds)
////      result shouldBe Seq(AssessmentReport(payload, outcome))
////
////      verify(metrics, times(1)).dequeued()
////      verify(metrics, times(1)).failed()
////    }
////  }
//
//}
