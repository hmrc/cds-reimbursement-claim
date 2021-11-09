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
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.CompleteClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.IdGen.{genAssociatedMRNs, genClaims}

@SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
class CompleteClaimSpec extends AnyWordSpec with Matchers with MockFactory {
  "Complete Claim" should {
    "calculate the total amount to be reimbursed" when {
      "Single Journey" in {
        val claims                      = genClaims.sample.get.map(
          _.copy(
            paidAmount = BigDecimal(15),
            claimAmount = BigDecimal(10)
          )
        )
        val claimedReimbursementAnswers = ClaimedReimbursementsAnswer(claims).get
        val completeClaim               = sample[CompleteClaim].copy(
          typeOfClaim = Individual,
          claimedReimbursementsAnswer = claimedReimbursementAnswers,
          associatedMRNsAnswer = None,
          associatedMRNsClaimsAnswer = None
        )

        val expectedClaimAmount = claims.size * 10
        completeClaim.totalReimbursementAmount shouldBe expectedClaimAmount
      }

      "Bulk Multiple Journey" in {
        val claims                          = genClaims.sample.get.map(
          _.copy(
            paidAmount = BigDecimal(15),
            claimAmount = BigDecimal(10)
          )
        )
        val claimedReimbursementAnswers     = ClaimedReimbursementsAnswer(claims).get
        val associatedMrns                  = genAssociatedMRNs.sample
        val maybeAssociatedMRNsClaimsAnswer = associatedMrns.map(_.map { _ =>
          val associatedClaims = genClaims.sample.get.map(
            _.copy(
              paidAmount = BigDecimal(15),
              claimAmount = BigDecimal(10)
            )
          )
          ClaimedReimbursementsAnswer(associatedClaims).get
        })

        val completeClaim = sample[CompleteClaim].copy(
          typeOfClaim = Multiple,
          claimedReimbursementsAnswer = claimedReimbursementAnswers,
          associatedMRNsAnswer = associatedMrns.flatMap(NonEmptyList.fromList),
          associatedMRNsClaimsAnswer = maybeAssociatedMRNsClaimsAnswer.flatMap(NonEmptyList.fromList)
        )

        val expectedClaimAmount = maybeAssociatedMRNsClaimsAnswer.get.foldLeft(claims.size) { (a, b) =>
          a + b.size
        } * 10

        completeClaim.totalReimbursementAmount shouldBe expectedClaimAmount
      }
    }
  }
}
