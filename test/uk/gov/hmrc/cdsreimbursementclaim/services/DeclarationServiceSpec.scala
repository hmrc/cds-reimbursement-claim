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
//
//import cats.data.EitherT
//import org.scalamock.handlers.{CallHandler1, CallHandler2}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
//import play.api.test.Helpers._
//import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
//import uk.gov.hmrc.cdsreimbursementclaim.connectors.DeclarationConnector
//import uk.gov.hmrc.cdsreimbursementclaim.models.Error
//import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.request.{DeclarationRequest, OverpaymentDeclarationDisplayRequest, RequestCommon, RequestDetail}
//import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response._
//import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.{DisplayDeclaration, DisplayResponseDetail}
//import uk.gov.hmrc.cdsreimbursementclaim.models.generators.DeclarationGen._
//import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
//import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
//import uk.gov.hmrc.cdsreimbursementclaim.utils.{TimeUtils, toUUIDString}
//import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
//
//import java.util.UUID
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future

class DeclarationServiceSpec extends AnyWordSpec with Matchers with MockFactory {

//  val mockDeclarationConnector: DeclarationConnector                   = mock[DeclarationConnector]
//  val mockDeclarationTransformerService: DeclarationTransformerService = mock[DeclarationTransformerService]
//
//  val declarationService = new DefaultDeclarationService(
//    mockDeclarationConnector,
//    mockDeclarationTransformerService
//  )
//
//  implicit val hc: HeaderCarrier = HeaderCarrier()
//
//  def mockDeclarationConnector(declarationRequest: DeclarationRequest)(
//    response: Either[Error, HttpResponse]
//  ): CallHandler2[DeclarationRequest, HeaderCarrier, EitherT[Future, Error, HttpResponse]] =
//    (mockDeclarationConnector
//      .getDeclaration(_: DeclarationRequest)(_: HeaderCarrier))
//      .expects(declarationRequest, *)
//      .returning(EitherT.fromEither[Future](response))
//
//  def mockTransformDeclarationResponse(declarationResponse: DeclarationResponse)(
//    response: Either[Error, Option[DisplayDeclaration]]
//  ): CallHandler1[DeclarationResponse, Either[Error, Option[DisplayDeclaration]]] =
//    (mockDeclarationTransformerService
//      .toDeclaration(_: DeclarationResponse))
//      .expects(declarationResponse)
//      .returning(response)
//
//  val acc14SuccessPayload: String =
//    s"""
//         |{
//         |	"overpaymentDeclarationDisplayResponse": {
//         |		"responseCommon": {
//         |			"status": "OK",
//         |			"processingDate": "2021-02-12T11:34:54Z"
//         |		},
//         |		"responseDetail": {
//         |			"declarationId": "94LQRNVJY9FJQO_EI0",
//         |			"acceptanceDate": "2021-02-12",
//         |			"procedureCode": "2",
//         |			"declarantDetails": {
//         |				"declarantEORI": "AA12345678901234Z",
//         |				"legalName": "Automation Central LTD",
//         |				"establishmentAddress": {
//         |					"addressLine1": "10 Automation Road",
//         |					"addressLine3": "Coventry",
//         |					"postalCode": "CV3 6EA",
//         |					"countryCode": "GB"
//         |				},
//         |				"contactDetails": {
//         |					"contactName": "Automation Central LTD",
//         |					"addressLine1": "10 Automation Road",
//         |					"addressLine3": "Coventry",
//         |					"postalCode": "CV3 6EA",
//         |					"countryCode": "GB"
//         |				}
//         |			},
//         |			"consigneeDetails": {
//         |				"consigneeEORI": "AA12345678901234Z",
//         |				"legalName": "Automation Central LTD",
//         |				"establishmentAddress": {
//         |					"addressLine1": "10 Automation Road",
//         |					"addressLine3": "Coventry",
//         |					"postalCode": "CV3 6EA",
//         |					"countryCode": "GB"
//         |				},
//         |				"contactDetails": {
//         |					"contactName": "Automation Central LTD",
//         |					"addressLine1": "10 Automation Road",
//         |					"addressLine3": "Coventry",
//         |					"postalCode": "CV3 6EA",
//         |					"countryCode": "GB",
//         |          "telephone": "+4420723934397",
//         |          "emailAddress" : "automation@gmail.com"
//         |				}
//         |			},
//         |			"bankDetails": {
//         |				"consigneeBankDetails": {
//         |					"accountHolderName": "CDS E2E To E2E Bank",
//         |					"sortCode": "308844",
//         |					"accountNumber": "12345678"
//         |				},
//         |				"declarantBankDetails": {
//         |					"accountHolderName": "CDS E2E To E2E Bank",
//         |					"sortCode": "308844",
//         |					"accountNumber": "12345678"
//         |				}
//         |			},
//         |			"ndrcDetails": [
//         |				{
//         |					"taxType": "A80",
//         |					"amount": "218.00",
//         |					"paymentMethod": "001",
//         |					"paymentReference": "GB201430007000"
//         |				},
//         |				{
//         |					"taxType": "A95",
//         |					"amount": "211.00",
//         |					"paymentMethod": "001",
//         |					"paymentReference": "GB201430007000"
//         |				}
//         |			]
//         |		}
//         |	}
//         |}
//         |""".stripMargin
//
//  "Declaration Service" when {
//
//    val mrn                                  = sample[MRN]
//    val correlationId                        = UUID.randomUUID()
//    val receiptDate                          = TimeUtils.iso8601DateTimeNow
//    val requestCommon                        = sample[RequestCommon].copy(Platform.MDTP, receiptDate, correlationId)
//    val requestDetail                        = sample[RequestDetail].copy(declarationId = mrn.value, securityReason = None)
//    val overpaymentDeclarationDisplayRequest = sample[OverpaymentDeclarationDisplayRequest]
//      .copy(requestCommon = requestCommon, requestDetail = requestDetail)
//    val declarationRequest                   =
//      sample[DeclarationRequest].copy(overpaymentDeclarationDisplayRequest = overpaymentDeclarationDisplayRequest)
//
//    val responseCommon = sample[ResponseCommon].copy(
//      status = "OK",
//      statusText = None,
//      processingDate = "2021-02-12T11:34:54Z",
//      returnParameters = None
//    )
//
//    val responseDetail = sample[ResponseDetail].copy(
//      declarationId = "94LQRNVJY9FJQO_EI0",
//      acceptanceDate = "2021-02-12",
//      declarantReferenceNumber = None,
//      securityReason = None,
//      btaDueDate = None,
//      procedureCode = "2",
//      btaSource = None,
//      declarantDetails = sample[DeclarantDetails].copy(
//        EORI = "AA12345678901234Z",
//        legalName = "Automation Central LTD",
//        establishmentAddress = EstablishmentAddress(
//          addressLine1 = "10 Automation Road",
//          addressLine2 = None,
//          addressLine3 = Some("Coventry"),
//          postalCode = Some("CV3 6EA"),
//          countryCode = "GB"
//        ),
//        contactDetails = Some(
//          ContactDetails(
//            contactName = Some("Automation Central LTD"),
//            addressLine1 = Some("10 Automation Road"),
//            addressLine2 = None,
//            addressLine3 = Some("Coventry"),
//            addressLine4 = None,
//            postalCode = Some("CV3 6EA"),
//            countryCode = Some("GB"),
//            telephone = None,
//            emailAddress = None
//          )
//        )
//      ),
//      consigneeDetails = Some(
//        ConsigneeDetails(
//          EORI = "AA12345678901234Z",
//          legalName = "Automation Central LTD",
//          establishmentAddress = EstablishmentAddress(
//            addressLine1 = "10 Automation Road",
//            addressLine2 = None,
//            addressLine3 = Some("Coventry"),
//            postalCode = Some("CV3 6EA"),
//            countryCode = "GB"
//          ),
//          contactDetails = Some(
//            ContactDetails(
//              contactName = Some("Automation Central LTD"),
//              addressLine1 = Some("10 Automation Road"),
//              addressLine2 = None,
//              addressLine3 = Some("Coventry"),
//              addressLine4 = None,
//              postalCode = Some("CV3 6EA"),
//              countryCode = Some("GB"),
//              telephone = Some("+4420723934397"),
//              emailAddress = Some("automation@gmail.com")
//            )
//          )
//        )
//      ),
//      accountDetails = None,
//      bankDetails = Some(
//        BankDetails(
//          consigneeBankDetails = Some(
//            BankAccountDetails(
//              accountHolderName = "CDS E2E To E2E Bank",
//              sortCode = "308844",
//              accountNumber = "12345678"
//            )
//          ),
//          declarantBankDetails = Some(
//            BankAccountDetails(
//              accountHolderName = "CDS E2E To E2E Bank",
//              sortCode = "308844",
//              accountNumber = "12345678"
//            )
//          )
//        )
//      ),
//      ndrcDetails = Some(
//        List(
//          NdrcDetails(
//            taxType = "A80",
//            amount = "218.00",
//            paymentMethod = "001",
//            paymentReference = "GB201430007000",
//            cmaEligible = None
//          ),
//          NdrcDetails(
//            taxType = "A95",
//            amount = "211.00",
//            paymentMethod = "001",
//            paymentReference = "GB201430007000",
//            cmaEligible = None
//          )
//        )
//      )
//    )
//
//    val displayResponseDetail                 = sample[DisplayResponseDetail].copy(
//      declarationId = "94LQRNVJY9FJQO_EI0",
//      acceptanceDate = "2021-02-12",
//      declarantReferenceNumber = None,
//      securityReason = None,
//      btaDueDate = None,
//      procedureCode = "2",
//      btaSource = None,
//      declarantDetails = sample[DeclarantDetails].copy(
//        EORI = "AA12345678901234Z",
//        legalName = "Automation Central LTD",
//        establishmentAddress = EstablishmentAddress(
//          addressLine1 = "10 Automation Road",
//          addressLine2 = None,
//          addressLine3 = Some("Coventry"),
//          postalCode = Some("CV3 6EA"),
//          countryCode = "GB"
//        ),
//        contactDetails = Some(
//          ContactDetails(
//            contactName = Some("Automation Central LTD"),
//            addressLine1 = Some("10 Automation Road"),
//            addressLine2 = None,
//            addressLine3 = Some("Coventry"),
//            addressLine4 = None,
//            postalCode = Some("CV3 6EA"),
//            countryCode = Some("GB"),
//            telephone = None,
//            emailAddress = None
//          )
//        )
//      ),
//      consigneeDetails = Some(
//        ConsigneeDetails(
//          EORI = "AA12345678901234Z",
//          legalName = "Automation Central LTD",
//          establishmentAddress = EstablishmentAddress(
//            addressLine1 = "10 Automation Road",
//            addressLine2 = None,
//            addressLine3 = Some("Coventry"),
//            postalCode = Some("CV3 6EA"),
//            countryCode = "GB"
//          ),
//          contactDetails = Some(
//            ContactDetails(
//              contactName = Some("Automation Central LTD"),
//              addressLine1 = Some("10 Automation Road"),
//              addressLine2 = None,
//              addressLine3 = Some("Coventry"),
//              addressLine4 = None,
//              postalCode = Some("CV3 6EA"),
//              countryCode = Some("GB"),
//              telephone = Some("+4420723934397"),
//              emailAddress = Some("automation@gmail.com")
//            )
//          )
//        )
//      ),
//      accountDetails = None,
//      bankDetails = Some(
//        BankDetails(
//          consigneeBankDetails = Some(
//            BankAccountDetails(
//              accountHolderName = "CDS E2E To E2E Bank",
//              sortCode = "308844",
//              accountNumber = "12345678"
//            )
//          ),
//          declarantBankDetails = Some(
//            BankAccountDetails(
//              accountHolderName = "CDS E2E To E2E Bank",
//              sortCode = "308844",
//              accountNumber = "12345678"
//            )
//          )
//        )
//      ),
//      ndrcDetails = Some(
//        List(
//          NdrcDetails(
//            taxType = "A80",
//            amount = "218.00",
//            paymentMethod = "001",
//            paymentReference = "GB201430007000",
//            cmaEligible = None
//          ),
//          NdrcDetails(
//            taxType = "A95",
//            amount = "211.00",
//            paymentMethod = "001",
//            paymentReference = "GB201430007000",
//            cmaEligible = None
//          )
//        )
//      )
//    )
//    val overpaymentDeclarationDisplayResponse = sample[OverpaymentDeclarationDisplayResponse]
//      .copy(responseCommon = responseCommon, responseDetail = Some(responseDetail))
//
//    val declarationResponse =
//      sample[DeclarationResponse].copy(overpaymentDeclarationDisplayResponse = overpaymentDeclarationDisplayResponse)
//
//    val declaration = sample[DisplayDeclaration].copy(
//      displayResponseDetail = displayResponseDetail
//    )
//
//    "handling requests to get a declaration" must {
//
//      "return a declaration" when {
//
//        "a successful http response is received" in {
//
//          inSequence {
//            mockDeclarationConnector(declarationRequest)(
//              Right(HttpResponse(200, acc14SuccessPayload, Map.empty[String, Seq[String]]))
//            )
//            mockTransformDeclarationResponse(declarationResponse)(Right(Some(declaration)))
//          }
//
//          await(declarationService.getDeclaration(mrn).value) shouldBe Right(Some(declaration))
//        }
//      }
//
//      "return an error" when {
//
//        "an corrupt/invalid acc14 payload is received" in {
//          inSequence {
//            mockDeclarationConnector(declarationRequest)(
//              Right(HttpResponse(200, "corrupt/bad payload", Map.empty[String, Seq[String]]))
//            )
//          }
//          await(declarationService.getDeclaration(mrn).value).isLeft shouldBe true
//        }
//
//        "a http status response other than 200 OK is received" in {
//          inSequence {
//            mockDeclarationConnector(declarationRequest)(
//              Right(HttpResponse(400, "some error", Map.empty[String, Seq[String]]))
//            )
//          }
//          await(declarationService.getDeclaration(mrn).value).isLeft shouldBe true
//        }
//
//        "an unsuccessful http response is received" in {
//          inSequence {
//            mockDeclarationConnector(declarationRequest)(Left(Error("http bad request")))
//          }
//          await(declarationService.getDeclaration(mrn).value).isLeft shouldBe true
//        }
//      }
//    }
//  }
}
