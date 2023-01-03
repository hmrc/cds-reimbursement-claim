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

package uk.gov.hmrc.cdsreimbursementclaim.services

import com.google.inject.ImplementedBy
import uk.gov.hmrc.cdsreimbursementclaim.connectors.Tpi02Connector
import uk.gov.hmrc.cdsreimbursementclaim.models.CDFPayService
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi02.{ErrorResponse, GetSpecificCaseResponse}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[GetSpecificClaimServiceImpl])
trait GetSpecificClaimService {
  def getSpecificClaim(cdfPayService: CDFPayService, cdfPayCaseNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[ErrorResponse, GetSpecificCaseResponse]]
}
@Singleton
class GetSpecificClaimServiceImpl @Inject() (tpi02Connector: Tpi02Connector)(implicit
  executionContext: ExecutionContext
) extends GetSpecificClaimService {

  def getSpecificClaim(cdfPayService: CDFPayService, cdfPayCaseNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[ErrorResponse, GetSpecificCaseResponse]] =
    tpi02Connector
      .getSpecificClaim(cdfPayService, cdfPayCaseNumber)
      .map(_.map(_.getSpecificCaseResponse))
}
