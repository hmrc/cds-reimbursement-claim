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
import uk.gov.hmrc.cdsreimbursementclaim.connectors.SubscriptionConnector
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger

@ImplementedBy(classOf[GetXiEoriServiceImpl])
trait GetXiEoriService {
  def getXIEori(eori: Eori)(implicit hc: HeaderCarrier): Future[Option[Eori]]
}

@Singleton
class GetXiEoriServiceImpl @Inject() (connector: SubscriptionConnector)(implicit
  executionContext: ExecutionContext
) extends GetXiEoriService {

  def getXIEori(eori: Eori)(implicit hc: HeaderCarrier): Future[Option[Eori]] =
    connector
      .getSubscription(eori)
      .map {
        case Right(subscriptionOpt) =>
          subscriptionOpt
            .flatMap(_.subscriptionDisplayResponse.responseDetail.XI_Subscription)
            .map(_.XI_EORINo)

        case Left(error) =>
          Logger(getClass()).error(error)
          None
      }
}
