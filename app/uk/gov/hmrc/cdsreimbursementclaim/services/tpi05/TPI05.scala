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

package uk.gov.hmrc.cdsreimbursementclaim.services.tpi05

import cats.data.Validated
import cats.data.Validated.Valid
import cats.implicits.catsSyntaxOptionId
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.{ISO8601DateTime, IsoLocalDate}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{CDFPayservice, ClaimType, CustomDeclarationType}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.UUIDGenerator.compactCorrelationId

object TPI05 {

  def request: Builder = Builder(
    Valid(
      RequestDetail(
        RequestDetailA(
          CDFPayService = CDFPayservice.NDRC,
          dateReceived = IsoLocalDate.now.some,
          customDeclarationType = Some(CustomDeclarationType.MRN),
          claimDate = IsoLocalDate.now.some
        ),
        RequestDetailB()
      )
    )
  )

  final case class Builder private (validation: Validated[Error, RequestDetail]) {

    def forClaimOfType(claimType: ClaimType): Builder = ???
//      copy(validation.map(_.copy(requestDetailA = )))

    def verify: Either[Error, EisSubmitClaimRequest] =
      validation.toEither.map { requestDetail =>
        EisSubmitClaimRequest(
          PostNewClaimsRequest(
            RequestCommon(
              originatingSystem = Platform.MDTP,
              receiptDate = ISO8601DateTime.now,
              acknowledgementReference = compactCorrelationId
            ),
            requestDetail
          )
        )
      }
  }
}
