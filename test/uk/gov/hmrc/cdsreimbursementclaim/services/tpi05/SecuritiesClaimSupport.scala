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
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimantType, SecuritiesClaim, TaxCode, TaxReclaimDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant.{Importer, Representative}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.TaxDetails

trait SecuritiesClaimSupport {

  implicit class SecuritiesClaimOps(claimAndDeclaration: (SecuritiesClaim, DisplayDeclaration)) {
    val (claim, declaration) = claimAndDeclaration

    def claimant: Claimant =
      if (claim.claimantType === ClaimantType.Consignee) Importer else Representative
//
//    def getSecurityDetails: List[SecurityDetail] =
//      claim.securitiesReclaims.map { a =>
//        SecurityDetail(
//          a._1,
//          a._2.values.sum.toString,
//          amountPaid = "todo",
//          paymentMethod = "todo",
//          paymentReference = "todo",
//          getTaxDetails(
//            declaration.displayResponseDetail.securityDetails
//              .flatMap(_.find(_.securityDepositId == a._1))
//              .map(_.taxDetails)
//              .getOrElse(Nil),
//            a._2
//          )
//        )
//      }.toList

    def getTaxDetails(
      depositTaxes: List[TaxDetails],
      claimItems: Map[TaxCode, BigDecimal]
    ): List[TaxReclaimDetail] =
      depositTaxes
        .sortBy(_.taxType)
        .filter(dt => claimItems.keys.exists(_.value === dt.taxType))
        .zip(claimItems.toList.sortBy(_._1.value))
        .map { case (TaxDetails(taxType, amount), (_, claimAmount)) =>
          TaxReclaimDetail(taxType, amount, claimAmount.toString())
        }
  }
}
