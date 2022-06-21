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

package uk.gov.hmrc.cdsreimbursementclaim.services.tpi05

import cats.implicits.catsSyntaxEq
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimantType, SecuritiesClaim, SecurityDetail, TaxCode, TaxDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant.{Importer, Representative}

trait SecuritiesClaimSupport {

  implicit class SecuritiesClaimOps(claim: SecuritiesClaim) {

    def claimant: Claimant =
      if (claim.claimantType === ClaimantType.Consignee) Importer else Representative

    def getSecurityDetails: List[SecurityDetail] =
      claim.securitiesReclaims.map { a =>
        SecurityDetail(
          a._1,
          a._2.values.sum.toString,
          amountPaid = "todo",
          paymentMethod = "todo",
          paymentReference = "todo",
          getTaxDetails(a._2)
        )
      }.toList

    private def getTaxDetails(claimItems: Map[TaxCode, BigDecimal]): List[TaxDetail] =
      claimItems.map(a => TaxDetail(a._1.value, a._2.toString)).toList
  }
}
