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

package uk.gov.hmrc.cdsreimbursementclaim.repositories.upscan

import com.typesafe.config.ConfigFactory
import org.scalatest.Ignore
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.test.Helpers._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.UpscanGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.upscan.{UploadReference, UpscanUpload}
import uk.gov.hmrc.cdsreimbursementclaim.repositories.MongoTestSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{FiniteDuration, SECONDS}

@Ignore
class UpscanRepositoryFailureSpec extends AnyWordSpec with Matchers with MongoTestSupport {
  val config = Configuration(
    ConfigFactory.parseString(
      """
        | mongodb.upscan.expiry-time = 7days
        |""".stripMargin
    )
  )

  @SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext"))
  val repository = new DefaultUpscanRepository(reactiveMongoComponent, config)

  @SuppressWarnings(Array("org.wartremover.warts.ThreadSleep"))
  override def beforeAll(): Unit = {
    super.beforeAll()
    Thread.sleep(1200) //allow indexing to complete
    reactiveMongoComponent.mongoConnector.helper.driver.close(FiniteDuration(10, SECONDS))
  }

  "Upscan Repository" when {
    "inserting" should {
      "return an error if there is a failure" in {
        val upscanUpload = sample[UpscanUpload]
        await(repository.insert(upscanUpload).value).isLeft shouldBe true
      }
    }

    "updating an upscan upload document" should {
      "return an error if there is a failure" in {
        await(
          repository
            .select(sample[UploadReference])
            .value
        ).isLeft shouldBe true
      }
    }

    "selecting an upscan upload document" should {
      "return an error if there is a failure" in {
        await(
          repository
            .select(sample[UploadReference])
            .value
        ).isLeft shouldBe true
      }
    }

    "selecting all upscan upload documents" should {
      "return an error if there is a failure" in {
        await(
          repository
            .selectAll(List(sample[UploadReference]))
            .value
        ).isLeft shouldBe true
      }
    }
  }

}
