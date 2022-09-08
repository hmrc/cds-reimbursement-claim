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

package uk.gov.hmrc.cdsreimbursementclaim.repositories.ccs

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.test.Helpers._
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.CcsSubmissionGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
import uk.gov.hmrc.cdsreimbursementclaim.repositories.MongoTestSupport
import uk.gov.hmrc.cdsreimbursementclaim.services.ccs.CcsSubmissionRequest
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.workitem.{Failed, InProgress, PermanentlyFailed, WorkItem}

import scala.concurrent.ExecutionContext.Implicits.global

@SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext"))
class CcsSubmissionRepoSpec extends AnyWordSpec with Matchers with MongoTestSupport {

  val config: Configuration = Configuration(
    ConfigFactory.parseString(
      """
        |ccs {
        |    submission-poller {
        |        jitter-period = 5 seconds
        |        initial-delay = 10 hours
        |        interval = 10 hours
        |        failure-count-limit = 50
        |        in-progress-retry-after = 5000 # milliseconds
        |        mongo {
        |            ttl = 10 days
        |        }
        |    }
        |}
        |""".stripMargin
    )
  )

  val repository: CcsSubmissionRepo = ???
  /** new DefaultCcsSubmissionRepo(
    reactiveMongoComponent,
    config,
    new ServicesConfig(config)
  )*/

  "CcsSubmission Repo" when {

    "set" should {
      "insert a ccs submission request" in {
        val ccsSubmissionRequest = sample[CcsSubmissionRequest]
        //await(repository.set(ccsSubmissionRequest).value).map(item => item.item) shouldBe Right(ccsSubmissionRequest)
        assert(false)
      }
    }

    "get" should {
      "return some work item if one exists" in {

        val ccsSubmissionRequest = sample[CcsSubmissionRequest]
        //await(repository.set(ccsSubmissionRequest).value).map(item => item.item) shouldBe Right(ccsSubmissionRequest)
        assert(false)

        /*await(repository.get.value).map(maybeWorkItem => maybeWorkItem.map(workItem => workItem.item)) shouldBe Right(
          Some(ccsSubmissionRequest)
        )*/
        assert(false)


      }
      "return none if no work item exists " in {
        assert(false)

        /*await(
          repository.get.value
        ).map(mw => mw.map(s => s.item)) shouldBe Right(None)*/
      }
    }

    "set processing status" should {

      "update the work item status" in {
        val ccsSubmissionRequest                                    = sample[CcsSubmissionRequest]
        /*val workItem: Either[Error, WorkItem[CcsSubmissionRequest]] =
          await(repository.set(ccsSubmissionRequest).value)
        workItem.map(wi => await(repository.setProcessingStatus(wi.id, Failed).value) shouldBe Right(true))*/
        assert(false)

      }

    }

    "set result status" should {
      "update the work item status" in {
        val ccsSubmissionRequest                                    = sample[CcsSubmissionRequest]
        /*val workItem: Either[Error, WorkItem[CcsSubmissionRequest]] =
          await(repository.set(ccsSubmissionRequest).value)
        val _                                                       =
          workItem.map(wi => await(repository.setProcessingStatus(wi.id, InProgress).value))
        workItem.map(wi => await(repository.setResultStatus(wi.id, PermanentlyFailed).value) shouldBe Right(true))

         */
        assert(false)

      }
    }
  }

}
