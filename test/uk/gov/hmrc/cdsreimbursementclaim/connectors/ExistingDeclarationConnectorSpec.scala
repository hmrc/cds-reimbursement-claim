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

package uk.gov.hmrc.cdsreimbursementclaim.connectors

import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json
import play.api.test.Helpers.*
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.http.CustomHeaderNames
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ExistingClaim
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.request.{RequestCommon, TPI04Request}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.{CorrelationId, MRN}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ReasonForSecurityGen.*
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen.*
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ExistingClaimGen.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.ISO8601DateTime

import scala.concurrent.ExecutionContext.Implicits.global

class ExistingDeclarationConnectorSpec
    extends AnyWordSpec
    with Matchers
    with MockFactory
    with HttpV2Support
    with ScalaCheckPropertyChecks {

  private val mockConfig = mock[ServicesConfig]
  private val baseUrl    = "http://localhost:7502"
  private val testToken  = "test token"

  private def mockBaseUrl =
    (mockConfig.baseUrl(_: String)).expects("declaration").returning(baseUrl)

  private def mockGetString =
    (mockConfig.getString(_: String)).expects("eis.bearer-token").returning(testToken)

  val connector: ExistingDeclarationConnector = new ExistingDeclarationConnector(mockHttp, mockConfig) {
    override def getExtraHeaders(implicit hc: HeaderCarrier): Seq[(String, String)] =
      Seq(
        HeaderNames.DATE                   -> "some-date",
        CustomHeaderNames.X_CORRELATION_ID -> "some-correlation-id",
        HeaderNames.X_FORWARDED_HOST       -> Platform.MDTP,
        HeaderNames.CONTENT_TYPE           -> MimeTypes.JSON,
        HeaderNames.ACCEPT                 -> MimeTypes.JSON
      )
  }

  val explicitHeaders = Seq(
    "Date"             -> "some-date",
    "X-Correlation-ID" -> "some-correlation-id",
    "X-Forwarded-Host" -> "MDTP",
    "Content-Type"     -> "application/json",
    "Accept"           -> "application/json",
    "Authorization"    -> "Bearer test-token"
  )

  "Existing Declaration Connector" should {
    val backEndUrl         = s"$baseUrl/tpi/getexistingclaim/v1"
    val acknowledgementRef = CorrelationId.compact
    val receiptDate        = ISO8601DateTime.now

    mockBaseUrl
    mockGetString

    "return an existing claim from the downstream service" in forAll {
      (
        mrn: MRN,
        reason: ReasonForSecurity,
        response: ExistingClaim
      ) =>

        val requestDetails = TPI04Request(
          RequestCommon(
            Platform.MDTP,
            receiptDate,
            acknowledgementRef
          ),
          mrn,
          reason
        )

        mockHttpPostSuccess[ExistingClaim](backEndUrl, Json.toJson(requestDetails), response)
        val actual         = await(connector.checkExistingDeclaration(mrn, reason, receiptDate, acknowledgementRef).value)
        actual shouldBe Right(response)
    }
  }
}
