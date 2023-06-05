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
import play.api.test.Helpers._
import uk.gov.hmrc.cdsreimbursementclaim.connectors.DeclarationConnector
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.GetDeclarationError
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.request.DeclarationRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Acc14DeclarationGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext"))
class DeclarationServiceSpec extends AnyWordSpec with Matchers with MockFactory with ScalaCheckPropertyChecks {

  val declarationConnectorMock: DeclarationConnector                   = mock[DeclarationConnector]
  val declarationTransformerServiceMock: DeclarationTransformerService = mock[DeclarationTransformerService]

  val declarationService = new DefaultDeclarationService(
    declarationConnectorMock,
    declarationTransformerServiceMock
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 1)

  def mockDeclarationConnector(
    response: Either[Error, HttpResponse]
  ): CallHandler2[DeclarationRequest, HeaderCarrier, EitherT[Future, Error, HttpResponse]] =
    (declarationConnectorMock
      .getDeclaration(_: DeclarationRequest)(_: HeaderCarrier))
      .expects(*, *)
      .returning(EitherT.fromEither[Future](response))

  def mockTransformDeclarationResponse(declarationResponse: DeclarationResponse)(
    response: Either[Error, Option[DisplayDeclaration]]
  ): CallHandler1[DeclarationResponse, Either[Error, Option[DisplayDeclaration]]] =
    (declarationTransformerServiceMock
      .toDeclaration(_: DeclarationResponse))
      .expects(declarationResponse)
      .returning(response)

  "Declaration Service" when {

    "handling requests to get a declaration" must {

      "return a declaration" when {

        "a successful http response is received" in forAll {
          (
            mrn: MRN,
            declarationResponse: DeclarationResponse,
            declaration: DisplayDeclaration
          ) =>
            inSequence {
              mockDeclarationConnector(
                Right(
                  HttpResponse(
                    200,
                    Json.toJson(declarationResponse).toString(),
                    Map.empty[String, immutable.Seq[String]]
                  )
                )
              )
              mockTransformDeclarationResponse(declarationResponse)(Right(Some(declaration)))
            }
            await(declarationService.getDeclaration(mrn).value) shouldBe Right(Some(declaration))
        }
      }

      "return a declaration for getDeclarationWithErrorCodes" when {

        "a successful http response is received" in forAll {
          (
            mrn: MRN,
            declarationResponse: DeclarationResponse,
            declaration: DisplayDeclaration
          ) =>
            inSequence {
              mockDeclarationConnector(
                Right(
                  HttpResponse(
                    200,
                    Json.toJson(declarationResponse).toString(),
                    Map.empty[String, immutable.Seq[String]]
                  )
                )
              )
              mockTransformDeclarationResponse(declarationResponse)(Right(Some(declaration)))
            }
            await(declarationService.getDeclarationWithErrorCodes(mrn).value) shouldBe Right(declaration)
        }
      }

      "return an error" when {

        "an corrupt/invalid acc14 payload is received" in forAll { mrn: MRN =>
          inSequence {
            mockDeclarationConnector(
              Right(HttpResponse(200, "corrupt/bad payload", Map.empty[String, immutable.Seq[String]]))
            )
          }
          await(declarationService.getDeclaration(mrn).value).isLeft shouldBe true
        }

        "a http status response other than 200 OK is received" in forAll { mrn: MRN =>
          inSequence {
            mockDeclarationConnector(
              Right(HttpResponse(400, "some error", Map.empty[String, immutable.Seq[String]]))
            )
          }
          await(declarationService.getDeclaration(mrn).value).isLeft shouldBe true
        }

        "an unsuccessful http response is received" in forAll { mrn: MRN =>
          inSequence {
            mockDeclarationConnector(Left(Error("http bad request")))
          }
          await(declarationService.getDeclaration(mrn).value).isLeft shouldBe true
        }
      }
    }

    "return an error for getDeclarationWithErrorCodes" when {

      "UnexpectedError: an corrupt/invalid acc14 payload is received" in forAll { mrn: MRN =>
        inSequence {
          mockDeclarationConnector(
            Right(HttpResponse(200, "corrupt/bad payload", Map.empty[String, immutable.Seq[String]]))
          )
        }
        await(declarationService.getDeclarationWithErrorCodes(mrn).value) shouldBe Left(
          GetDeclarationError.unexpectedError
        )
      }

      "UnexpectedError: a http status response other than 200 OK is received" in forAll { mrn: MRN =>
        inSequence {
          mockDeclarationConnector(
            Right(HttpResponse(400, "some error", Map.empty[String, immutable.Seq[String]]))
          )
        }
        await(declarationService.getDeclarationWithErrorCodes(mrn).value) shouldBe Left(
          GetDeclarationError.unexpectedError
        )
      }

      "UnexpectedError: an unsuccessful http response is received" in forAll { mrn: MRN =>
        inSequence {
          mockDeclarationConnector(Left(Error("http bad request")))
        }
        await(declarationService.getDeclarationWithErrorCodes(mrn).value) shouldBe Left(
          GetDeclarationError.unexpectedError
        )
      }

      "InvalidReasonForSecurity" in forAll { mrn: MRN =>
        val declarationErrorResponse: DeclarationErrorResponse = declarationErrorResponseGenerator("072")
        inSequence {
          mockDeclarationConnector(
            Right(
              HttpResponse(
                400,
                Json.toJson(declarationErrorResponse).toString(),
                Map.empty[String, immutable.Seq[String]]
              )
            )
          )
        }
        await(declarationService.getDeclarationWithErrorCodes(mrn).value) shouldBe Left(
          GetDeclarationError.invalidReasonForSecurity
        )
      }

      "DeclarationNotFound" in forAll { mrn: MRN =>
        val declarationErrorResponse: DeclarationErrorResponse = declarationErrorResponseGenerator("086")
        inSequence {
          mockDeclarationConnector(
            Right(
              HttpResponse(
                400,
                Json.toJson(declarationErrorResponse).toString(),
                Map.empty[String, immutable.Seq[String]]
              )
            )
          )
        }
        await(declarationService.getDeclarationWithErrorCodes(mrn).value) shouldBe Left(
          GetDeclarationError.declarationNotFound
        )
      }

      def declarationErrorResponseGenerator(errorCode: String): DeclarationErrorResponse =
        DeclarationErrorResponse(ErrorDetail(None, None, None, None, None, SourceFaultDetail(List(errorCode))))
    }
  }
}
