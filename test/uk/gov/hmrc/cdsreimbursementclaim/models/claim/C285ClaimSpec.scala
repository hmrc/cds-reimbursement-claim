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

package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import cats.data.NonEmptyList
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.TypeOfClaimAnswer._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.C285ClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample

class C285ClaimSpec extends AnyWordSpec with Matchers with MockFactory {

  "C285 Claim" should {
    "calculate the total amount to be reimbursed" when {
      "Single Journey" in {
        val claims = sample(genClaims).map(
          _.copy(
            paidAmount = BigDecimal(15),
            claimAmount = BigDecimal(10)
          )
        )

        val completeClaim = sample[C285Claim].copy(
          typeOfClaim = Individual,
          claimedReimbursementsAnswer = NonEmptyList.fromListUnsafe(claims),
          associatedMRNsAnswer = None,
          associatedMRNsClaimsAnswer = None
        )

        val expectedClaimAmount = claims.size * 10
        completeClaim.totalReimbursementAmount shouldBe expectedClaimAmount
      }

      "Bulk Multiple Journey" in {

        val claims = sample(genClaims).map(
          _.copy(
            paidAmount = BigDecimal(15),
            claimAmount = BigDecimal(10)
          )
        )

        val claimedReimbursementAnswers = NonEmptyList.fromListUnsafe(claims)

        val associatedMrns = NonEmptyList.fromList(sample(genAssociatedMRNs))

        val maybeAssociatedMRNsClaimsAnswer = associatedMrns.map(_.map { _ =>
          val associatedClaims = sample(genClaims).map(
            _.copy(
              paidAmount = BigDecimal(15),
              claimAmount = BigDecimal(10)
            )
          )
          NonEmptyList.fromListUnsafe(associatedClaims)
        })

        val c285Claim = sample[C285Claim].copy(
          typeOfClaim = Multiple,
          claimedReimbursementsAnswer = claimedReimbursementAnswers,
          associatedMRNsAnswer = associatedMrns,
          associatedMRNsClaimsAnswer = maybeAssociatedMRNsClaimsAnswer
        )

        val expectedClaimAmount = maybeAssociatedMRNsClaimsAnswer.fold(0)(_.foldLeft(claims.size) { (a, b) =>
          a + b.size
        } * 10)

        c285Claim.totalReimbursementAmount shouldBe expectedClaimAmount
      }
    }
  }
}
