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

import cats.implicits.catsSyntaxOptionId
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.Inside.inside
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform.MDTP
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.SecuritiesClaimRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{ClaimType, CustomDeclarationType}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.{EisSubmitClaimRequest, PostNewClaimsRequest}
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.SecuritiesClaimGen._

class SecuritiesClaimMappingSpec
    extends AnyWordSpec
    with SecuritiesClaimSupport
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with OptionValues
    with TypeCheckedTripleEquals {

  "The Securities claim mapper" should {

    "map a valid Securities claim to TPI05 request" in forAll { (request: SecuritiesClaimRequest) =>
      import request.claim
      val tpi05Request = securitiesClaimToTPI05Mapper.map(claim)
      inside(tpi05Request) {
        case Left(_)                                                            =>
          assert(claim.claimantInformation.contactInformation.countryCode.isEmpty)
        case Right(EisSubmitClaimRequest(PostNewClaimsRequest(common, detail))) =>
          common.originatingSystem     should ===(MDTP)
          detail.customDeclarationType should ===(CustomDeclarationType.MRN.some)
          detail.claimType             should ===(Some(ClaimType.SECURITY))
          detail.claimantEORI          should ===(claim.claimantInformation.eori)
          detail.claimantEmailAddress  should ===(
            claim.claimantInformation.contactInformation.emailAddress.map(Email(_)).value
          )
          detail.claimantName          should ===(claim.claimantInformation.contactInformation.contactPerson.value)
          detail.securityDetails       should ===(claim.getSecurityDetails)
      }
    }
  }
}
