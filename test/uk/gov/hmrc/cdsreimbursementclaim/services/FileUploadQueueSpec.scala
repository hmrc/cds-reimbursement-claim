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

import java.util.UUID

import cats.data.EitherT
import com.typesafe.config.ConfigFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec
import play.api.{Configuration, Environment}
import play.api.inject.DefaultApplicationLifecycle
import play.api.test.Helpers._
import play.modules.reactivemongo.ReactiveMongoComponentImpl
import uk.gov.hmrc.cdsreimbursementclaim.config.AppConfig
import uk.gov.hmrc.cdsreimbursementclaim.models.WorkItemResult
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
//import ru.tinkoff.phobos.akka_http.HeadlessEnvelope
//import uk.gov.hmrc.cdsreimbursementclaim.models.GenerateUploadFiles._
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error}
import uk.gov.hmrc.cdsreimbursementclaim.repositories.FileUploadsRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging._
import uk.gov.hmrc.workitem._
import scala.concurrent.{ExecutionContext, Future}
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.time.{Millis, Seconds, Span}

class FileUploadQueueSpec extends AnyWordSpec with Matchers with MockFactory with BeforeAndAfterEach with Eventually {

  val env          = Environment.simple()
  val configChange = Configuration(ConfigFactory.parseString("queue.retry-after = 50 milliseconds"))
  val config       = Configuration.load(env) ++ configChange

  val servicesConfig     = new ServicesConfig(config)
  implicit val appConfig = new AppConfig(config, servicesConfig)

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier    =
    HeaderCarrier(requestId = Some(RequestId("RequestId")), sessionId = Some(SessionId("SessionId")))

  val lifeCycle         = new DefaultApplicationLifecycle()
  val mongoComponent    = new ReactiveMongoComponentImpl(config, env, lifeCycle)
  val repository        = new FileUploadsRepository(config, mongoComponent)
  val fileUploadService = mock[FileUploadService]
  val queue             = new FileUploadQueue(repository, fileUploadService)

  override def beforeEach(): Unit = {
    await(repository.drop)
    ()
  }

  def mockUploadService(result: Either[Error, Unit]) =
    (fileUploadService
      .upload(_: String)(_: HeaderCarrier))
      .expects(*, *)
      .returning(EitherT.fromEither[Future](result))
      .once()

  //def generateData: String = Dec64Body.soapEncoder.encode(HeadlessEnvelope(sample[Dec64Body]))
  def generateData: String = UUID.randomUUID().toString.replaceAll("-", "").take(5)

  "queue" should {
    "enqueue and dequeue a request" in {
      val payload  = generateData
      val workItem = await(queue.queueRequest(payload))
      mockUploadService(Right(()))
      val result   = await(queue.processAllRequests()).head
      result.payload.headers.requestId shouldBe Some("RequestId")
      result.payload.headers.sessionId shouldBe Some("SessionId")
      result.payload.soapRequest       shouldBe payload
      result.response                  shouldBe Right(())
      val dataAfterSucess = await(repository.findById(workItem.id))
      dataAfterSucess shouldBe None
    }

    "Retry if the call to http POST fails once" in {
      val payload  = generateData
      val workItem = await(queue.queueRequest(payload))
      val error    = Left(Error(s"Something terrible happened"))
      mockUploadService(error)
      await(queue.processAllRequests()).head.response shouldBe error
      val dataAfterFailure = await(repository.findById(workItem.id))
      dataAfterFailure.getOrElse(fail).failureCount shouldBe 1
      dataAfterFailure.getOrElse(fail).status       shouldBe Failed
    }

    "Retry unitil permamently fails" in {
      val timeout: Timeout   = Timeout(Span(5, Seconds))
      val interval: Interval = Interval(Span(100, Millis))

      def tryProcessing(): Seq[WorkItemResult] = eventually(timeout, interval) {
        val retriedRequests = await(queue.processAllRequests())
        retriedRequests.size shouldBe 1
        retriedRequests
      }

      val payload  = generateData
      val workItem = await(queue.queueRequest(payload))
      (1 to appConfig.queueMaxRetries).foreach { i =>
        val error = Left(Error(s"Failed $i times"))
        mockUploadService(error)
        tryProcessing().head.response shouldBe error
        val dataAfterFailure = await(repository.findById(workItem.id))
        dataAfterFailure.getOrElse(fail).failureCount shouldBe i
        dataAfterFailure.getOrElse(fail).status       shouldBe Failed
      }
      val error    = Left(Error("PermanentlyFailed"))
      mockUploadService(error)
      tryProcessing().head.response shouldBe error
      val dataAfterFailure = await(repository.findById(workItem.id))
      dataAfterFailure.getOrElse(fail).failureCount shouldBe appConfig.queueMaxRetries
      dataAfterFailure.getOrElse(fail).status       shouldBe PermanentlyFailed
    }

  }

}
