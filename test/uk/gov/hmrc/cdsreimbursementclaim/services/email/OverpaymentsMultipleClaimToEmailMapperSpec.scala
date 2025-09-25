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

package uk.gov.hmrc.cdsreimbursementclaim.services.email

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.Inside.inside
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.*
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.email.EmailRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.OverpaymentsClaimGen.genOverpaymentsMultipleClaim

class OverpaymentsMultipleClaimToEmailMapperSpec
    extends AnyWordSpec
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with OptionValues
    with TypeCheckedTripleEquals {

  val mapper = new OverpaymentsMultipleClaimToEmailMapper

  "The OverpaymentsMultiple claim mapper" should {

    "map a valid claim to email request" in forAll(genOverpaymentsMultipleClaim) {
      (multipleOverpaymentsData: (MultipleOverpaymentsClaim, List[DisplayDeclaration])) =>
        val (claim, displayDeclarations) = multipleOverpaymentsData
        val emailRequest                 = mapper `map` multipleOverpaymentsData

        inside(emailRequest) { case Right(EmailRequest(email, contactName, claimAmount)) =>
          email.value should ===(claim.claimantInformation.contactInformation.emailAddress.get)
          contactName should ===(claim.claimantInformation.contactInformation.contactPerson.get)
          claimAmount should ===(claim.totalReimbursementAmount)
        }
    }

    "fail to map an invalid email to email request" in {
      val multipleOverpaymentsData     = genOverpaymentsMultipleClaim.sample.get
      val (claim, displayDeclarations) = multipleOverpaymentsData
      val updatedClaim                 = claim.copy(
        claimantInformation = claim.claimantInformation.copy(
          contactInformation = claim.claimantInformation.contactInformation.copy(emailAddress = None)
        )
      )
      val emailRequest                 = mapper.map(multipleOverpaymentsData.copy(_1 = updatedClaim))

      emailRequest.left.map(_.value should be("no email address provided with claim"))
    }

    "fail to map an invalid contact name to email request" in {
      val multipleOverpaymentsData     = genOverpaymentsMultipleClaim.sample.get
      val (claim, displayDeclarations) = multipleOverpaymentsData
      val updatedClaim                 = claim
        .copy(
          claimantInformation = claim.claimantInformation
            .copy(
              contactInformation = claim.claimantInformation.contactInformation
                .copy(contactPerson = None)
            )
        )
      val emailRequest                 = mapper `map` multipleOverpaymentsData.copy(_1 = updatedClaim)

      emailRequest.left.map(_.value should be("no contact name provided with claim"))
    }
  }
}
