/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.cdsreimbursementclaim.controllers

import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.cdsreimbursementclaim.connectors.DeclarationConnector
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.request.DeclarationRequest
import cats.data.EitherT
import uk.gov.hmrc.http.HttpResponse
import cats.instances.future.*
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.cdsreimbursementclaim.connectors.ClaimConnector
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.EisSubmitClaimRequest
import uk.gov.hmrc.auth.core.retrieve.Credentials
import play.api.libs.json.Json

trait ConnectorStubs {

  class AuthConnectorStub() extends AuthConnector {

    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext
    ): Future[A] =
      println(predicate)
      println(retrieval)
      Future.successful(Some(Credentials("foo", "GovernmentGateway")).asInstanceOf[A])

  }

  class DeclarationConnectorStub(expectedDeclaration: String) extends DeclarationConnector {

    override def getDeclaration(declarationRequest: DeclarationRequest)(implicit
      hc: HeaderCarrier
    ): EitherT[Future, uk.gov.hmrc.cdsreimbursementclaim.models.Error, HttpResponse] =
      val acc14 = Json.prettyPrint(Json.parse(expectedDeclaration))
      println("ACC14 response:")
      println(acc14)
      EitherT.fromEither(Right(HttpResponse(200, expectedDeclaration)))
  }

  class ClaimConnectorStub extends ClaimConnector {

    override def submitClaim(submitClaimRequest: EisSubmitClaimRequest)(implicit
      hc: HeaderCarrier
    ): EitherT[Future, uk.gov.hmrc.cdsreimbursementclaim.models.Error, HttpResponse] =
      val tpi05 = Json.prettyPrint(Json.toJson(submitClaimRequest))
      println("TPI05 request:")
      println(tpi05)
      EitherT.fromEither(
        Right(
          HttpResponse(
            200,
            s"""{
                    |    "postNewClaimsResponse": {
                    |        "responseCommon": {
                    |            "status": "OK",
                    |            "processingDate": "2024-06-22T15:36:21.189",
                    |            "CDFPayService": "NDRC",
                    |            "CDFPayCaseNumber": "CASENUMBER",
                    |            "correlationID": "correlationID"
                    |        }
                    |    }
                    |}
                    |""".stripMargin
          )
        )
      )

  }

}
