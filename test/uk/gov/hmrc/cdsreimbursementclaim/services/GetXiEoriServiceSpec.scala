/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.data.EitherT
import org.scalamock.handlers.{CallHandler1, CallHandler2}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import play.api.test.Helpers.*
import play.api.test.FakeRequest
import uk.gov.hmrc.cdsreimbursementclaim.connectors.SubscriptionConnector
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.GetDeclarationError
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.request.DeclarationRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.*
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Acc14DeclarationGen.*
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen.*
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Sub09ReponseGen
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.{Eori, MRN}
import uk.gov.hmrc.cdsreimbursementclaim.models.sub09.SubscriptionResponse
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext"))
class GetXiEoriServiceSpec extends AnyWordSpec with Matchers with MockFactory with ScalaCheckPropertyChecks {

  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]

  val service = new GetXiEoriServiceImpl(
    mockSubscriptionConnector
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val testEori = Eori("ABC-123-XYZ")

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 1)

  def mockGetSubscription(eori: Eori)(response: Either[String, Option[SubscriptionResponse]]) =
    (mockSubscriptionConnector
      .getSubscription(_: Eori)(_: HeaderCarrier))
      .expects(*, *)
      .returning(Future.successful(response))

  "GetXiEoriService" when {
    "getXIEori success" should {
      "handling success response from getSubscription" in {
        forAll(Sub09ReponseGen.genSubscriptionWithXiEori) { case (response, _, _) =>
          inSequence {
            mockGetSubscription(testEori)(Right(Some(response)))
          }

          val result = await(service.getXIEori(testEori))
          result.map(_.value) shouldBe response.subscriptionDisplayResponse.responseDetail.flatMap(
            _.XI_Subscription.map(_.XI_EORINo)
          )
        }
      }
    }
    "getXIEori fail"    should {
      "handling failure when Left(error) response is received" in {
        forAll(Sub09ReponseGen.genSubscriptionWithXiEori) { case (response, _, _) =>
          inSequence {
            mockGetSubscription(testEori)(Left("some error"))
          }

          val result = await(service.getXIEori(testEori))
          result shouldBe None
        }
      }
      "handling failure when Right(None) response is received" in {
        inSequence {
          mockGetSubscription(testEori)(Right(None))
        }

        val result = await(service.getXIEori(testEori))
        result shouldBe None

      }
    }
  }
}
