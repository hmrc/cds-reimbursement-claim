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
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.cdsreimbursementclaim.connectors.DeclarationConnector
import uk.gov.hmrc.cdsreimbursementclaim.models.GenerateDeclaration._
import uk.gov.hmrc.cdsreimbursementclaim.models.Ids.UUIDGenerator
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.DateGenerator
import uk.gov.hmrc.cdsreimbursementclaim.models.declaration.Declaration
import uk.gov.hmrc.cdsreimbursementclaim.models.declaration.request.{DeclarationRequest, OverpaymentDeclarationDisplayRequest, RequestCommon, RequestDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.declaration.response._
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error, MRN}
import uk.gov.hmrc.cdsreimbursementclaim.utils.TimeUtils
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeclarationServiceSpec extends AnyWordSpec with Matchers with MockFactory {

  val declarationConnector = mock[DeclarationConnector]
  val mockUUIDGenerator    = mock[UUIDGenerator]
  val mockTransformer      = mock[DeclarationTransformerService]
  val mockDateGenerator    = mock[DateGenerator]

  val declarationService =
    new DeclarationServiceImpl(declarationConnector, mockUUIDGenerator, mockDateGenerator, mockTransformer)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  def mockGenerateUUID(uuid: UUID) =
    (mockUUIDGenerator.compactCorrelationId _: () => String).expects().returning(uuid.toString)

  def mockGenerateAcknowledgementDate(acknowledgmentDate: String) =
    (mockDateGenerator.nextAcknowledgementDate _: () => String).expects().returning(acknowledgmentDate)

  def mockDeclarationConnector(declarationRequest: DeclarationRequest)(
    response: Either[Error, HttpResponse]
  ) =
    (declarationConnector
      .getDeclaration(_: DeclarationRequest)(_: HeaderCarrier))
      .expects(declarationRequest, *)
      .returning(EitherT.fromEither[Future](response))

  def mockTransformDeclarationResponse(declarationResponse: DeclarationResponse)(
    response: Either[Error, Declaration]
  ) =
    (mockTransformer
      .toDeclaration(_: DeclarationResponse))
      .expects(declarationResponse)
      .returning(response)

  "Declaration Service" when {
    "handling requests to get a declaration" should {
      "get a declaration" in {
        val mrn                                  = sample[MRN]
        val correlationId                        = UUID.randomUUID()
        val acknowledgementDate                  = TimeUtils.eisDateTimeNow
        val requestCommon                        = sample[RequestCommon].copy("MDTP", acknowledgementDate, correlationId.toString)
        val requestDetail                        = sample[RequestDetail].copy(declarationId = mrn.value, securityReason = None)
        val overpaymentDeclarationDisplayRequest = sample[OverpaymentDeclarationDisplayRequest]
          .copy(requestCommon = requestCommon, requestDetail = requestDetail)
        val declarationRequest                   =
          sample[DeclarationRequest].copy(overpaymentDeclarationDisplayRequest = overpaymentDeclarationDisplayRequest)

        val declarationResponse = sample[DeclarationResponse]
        val declaration         = sample[Declaration]

        inSequence {
          mockGenerateAcknowledgementDate(acknowledgementDate)
          mockGenerateUUID(correlationId)
          mockDeclarationConnector(declarationRequest)(
            Right(HttpResponse(200, Json.toJson(declarationResponse).toString(), Map.empty[String, Seq[String]]))
          )
          mockTransformDeclarationResponse(declarationResponse)(Right(declaration))
        }

        await(declarationService.getDeclaration(mrn).value) shouldBe Right(declaration)
      }
    }
  }

}
