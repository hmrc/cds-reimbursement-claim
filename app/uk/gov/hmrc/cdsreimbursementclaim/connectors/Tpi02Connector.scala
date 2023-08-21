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

import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig
import uk.gov.hmrc.cdsreimbursementclaim.connectors.eis.{EisConnector, JsonHeaders}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.ISO8601DateTime
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.CorrelationId
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi02.{GetSpecificCaseRequest, Request, RequestCommon, RequestDetail, Response}
import uk.gov.hmrc.cdsreimbursementclaim.models.{CDFPayService, EisErrorResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class Tpi02Connector @Inject() (
  http: HttpClient,
  val config: ServicesConfig
)(implicit executionContext: ExecutionContext)
    extends EisConnector
    with JsonHeaders {

  import Tpi02Connector._

  private val getSpecificClaimUrl: String =
    s"${config.baseUrl("claim")}/tpi/getspecificcase/v1"

  def getSpecificClaim(cdfPayService: CDFPayService, cdfPayCaseNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[EisErrorResponse, Response]] = {

    val requestCommon = RequestCommon(
      receiptDate = ISO8601DateTime.now,
      acknowledgementReference = CorrelationId.compact,
      originatingSystem = MetaConfig.Platform.MDTP
    )

    val request = Request(
      GetSpecificCaseRequest(
        requestCommon,
        RequestDetail(cdfPayService.toString, cdfPayCaseNumber)
      )
    )

    http.POST[Request, Either[EisErrorResponse, Response]](getSpecificClaimUrl, request, getEISRequiredHeaders)
  }
}

object Tpi02Connector {

  final implicit val reads: HttpReads[Either[EisErrorResponse, Response]] =
    EisErrorResponse.readsErrorDetailOr[Response]

}
