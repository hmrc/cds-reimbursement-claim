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

package uk.gov.hmrc.cdsreimbursementclaim.services

import com.google.inject.ImplementedBy
import uk.gov.hmrc.cdsreimbursementclaim.connectors.Tpi01Connector
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi01.{ClaimsSelector, ErrorResponse, GetReimbursementClaimsResponse}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[GetClaimsServiceImpl])
trait GetClaimsService {
  def getClaims(eori: Eori, claimsSelector: ClaimsSelector)(implicit
    hc: HeaderCarrier
  ): Future[Either[ErrorResponse, GetReimbursementClaimsResponse]]
}

@Singleton
class GetClaimsServiceImpl @Inject() (tpi01Connector: Tpi01Connector)(implicit
  executionContext: ExecutionContext
) extends GetClaimsService {

  def getClaims(eori: Eori, claimsSelector: ClaimsSelector)(implicit
    hc: HeaderCarrier
  ): Future[Either[ErrorResponse, GetReimbursementClaimsResponse]] =
    tpi01Connector
      .getClaims(eori, claimsSelector)
      .map(_.map(_.getPostClearanceCasesResponse))
}
