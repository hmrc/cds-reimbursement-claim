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

package uk.gov.hmrc.cdsreimbursementclaim.services.ccs

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import cats.data.EitherT
import cats.instances.future._
import com.typesafe.config.ConfigFactory
import org.scalamock.handlers.{CallHandler0, CallHandler2}
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.Configuration
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.cdsreimbursementclaim.models
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.ccs.CcsSubmissionPayload
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.CcsSubmissionGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
import uk.gov.hmrc.cdsreimbursementclaim.repositories.ccs.CcsSubmissionRepo
import uk.gov.hmrc.cdsreimbursementclaim.services.ccs.CcsSubmissionPoller.OnCompleteHandler
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.workitem._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class CcsSubmissionPollerSpec
    extends TestKit(ActorSystem.create("ccs-submission-poller"))
    with AnyWordSpecLike
    with Matchers
    with MockFactory
    with Eventually
    with BeforeAndAfterAll {

  object TestOnCompleteHandler {
    case object Completed
  }

  class TestOnCompleteHandler(reportTo: ActorRef) extends OnCompleteHandler {
    override def onComplete(): Unit = reportTo ! TestOnCompleteHandler.Completed
  }

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  val config: Configuration = Configuration(
    ConfigFactory.parseString(
      """
        |ccs {
        |    submission-poller {
        |        jitter-period = 1 millisecond
        |        initial-delay = 1 millisecond
        |        interval = 120 seconds
        |        failure-count-limit = 10
        |        in-progress-retry-after = 5000 # milliseconds as required by work-item-repo library
        |        mongo {
        |            ttl = 10 days
        |        }
        |    }
        |}
        |
        |""".stripMargin
    )
  )

  implicit val executionContext: ExecutionContextExecutor                               = ExecutionContext.global
  implicit val hc: HeaderCarrier                                                        = HeaderCarrier()
  implicit val ccsSubmissionPollerExecutionContext: CcsSubmissionPollerExecutionContext =
    new CcsSubmissionPollerExecutionContext(system)

  val mockCcsSubmissionRepo: CcsSubmissionRepo       = mock[CcsSubmissionRepo]
  val mockCcsSubmissionService: CcsSubmissionService = mock[CcsSubmissionService]
  val servicesConfig                                 = new ServicesConfig(config)

  def mockCcsSubmissionRequestDequeue()(
    response: Either[Error, Option[WorkItem[CcsSubmissionRequest]]]
  ): CallHandler0[EitherT[Future, models.Error, Option[WorkItem[CcsSubmissionRequest]]]] =
    (mockCcsSubmissionService.dequeue _)
      .expects()
      .returning(EitherT.fromEither[Future](response))

  def mockSubmitToCcs(
    ccsSubmissionPayload: CcsSubmissionPayload
  )(
    response: Either[Error, HttpResponse]
  ): CallHandler2[CcsSubmissionPayload, HeaderCarrier, EitherT[Future, Error, HttpResponse]] =
    (mockCcsSubmissionService
      .submitToCcs(_: CcsSubmissionPayload)(_: HeaderCarrier))
      .expects(ccsSubmissionPayload, *)
      .returning(EitherT.fromEither(response))

  def mockSetProcessingStatus(id: BSONObjectID, status: ProcessingStatus)(
    response: Either[Error, Boolean]
  ): CallHandler2[BSONObjectID, ProcessingStatus, EitherT[Future, models.Error, Boolean]] =
    (mockCcsSubmissionService
      .setProcessingStatus(_: BSONObjectID, _: ProcessingStatus))
      .expects(id, status)
      .returning(EitherT.fromEither[Future](response))

  def mockSetResultStatus(id: BSONObjectID, status: ResultStatus)(
    response: Either[Error, Boolean]
  ): CallHandler2[BSONObjectID, ResultStatus, EitherT[Future, models.Error, Boolean]] =
    (mockCcsSubmissionService
      .setResultStatus(_: BSONObjectID, _: ResultStatus))
      .expects(id, status)
      .returning(EitherT.fromEither[Future](response))

  "Ccs Submission Poller" when {
    "it picks up a work item" must {
      "process the work item and set it to succeed if the ccs submission is successful" in {
        val onCompleteListener = TestProbe()
        val workItem           = sample[WorkItem[CcsSubmissionRequest]].copy(failureCount = 0, status = ToDo)

        inSequence {
          mockCcsSubmissionRequestDequeue()(Right(Some(workItem)))
          mockSubmitToCcs(
            CcsSubmissionPayload(
              workItem.item.payload,
              workItem.item.headers
            )
          )(Right(HttpResponse(204, "successful response")))
          mockSetResultStatus(workItem.id, Succeeded)(Right(true))
        }

        val _ =
          new CcsSubmissionPoller(
            system,
            mockCcsSubmissionService,
            ccsSubmissionPollerExecutionContext,
            servicesConfig,
            new TestOnCompleteHandler(onCompleteListener.ref)
          )

        onCompleteListener.expectMsg(TestOnCompleteHandler.Completed)
      }
    }

    "process the work item and set it to fail if the ccs submission is not successful" in {
      val onCompleteListener = TestProbe()

      val workItem = sample[WorkItem[CcsSubmissionRequest]].copy(failureCount = 0, status = ToDo)

      inSequence {
        mockCcsSubmissionRequestDequeue()(Right(Some(workItem)))
        mockSubmitToCcs(
          CcsSubmissionPayload(
            workItem.item.payload,
            workItem.item.headers
          )
        )(Right(HttpResponse(400, "bad request")))
        mockSetResultStatus(workItem.id, Failed)(Right(true))
      }

      val _ =
        new CcsSubmissionPoller(
          system,
          mockCcsSubmissionService,
          ccsSubmissionPollerExecutionContext,
          servicesConfig,
          new TestOnCompleteHandler(onCompleteListener.ref)
        )

      onCompleteListener.expectMsg(TestOnCompleteHandler.Completed)
    }

    "process the work item and set it to permanently failed if failure count has been reached" in {
      val onCompleteListener = TestProbe()

      val workItem = sample[WorkItem[CcsSubmissionRequest]].copy(failureCount = 10, status = Failed)
      inSequence {
        mockCcsSubmissionRequestDequeue()(Right(Some(workItem)))
        mockSetResultStatus(workItem.id, PermanentlyFailed)(Right(true))
      }

      val _ =
        new CcsSubmissionPoller(
          system,
          mockCcsSubmissionService,
          ccsSubmissionPollerExecutionContext,
          servicesConfig,
          new TestOnCompleteHandler(onCompleteListener.ref)
        )

      onCompleteListener.expectMsg(TestOnCompleteHandler.Completed)
    }

    "it polls and there are no work items" must {
      "do nothing" in {
        val onCompleteListener = TestProbe()

        mockCcsSubmissionRequestDequeue()(Right(None))
        val _ =
          new CcsSubmissionPoller(
            system,
            mockCcsSubmissionService,
            ccsSubmissionPollerExecutionContext,
            servicesConfig,
            new TestOnCompleteHandler(onCompleteListener.ref)
          )

        onCompleteListener.expectMsg(TestOnCompleteHandler.Completed)
      }
    }
  }
}
