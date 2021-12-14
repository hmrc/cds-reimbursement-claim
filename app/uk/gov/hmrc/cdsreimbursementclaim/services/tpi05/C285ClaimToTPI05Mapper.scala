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

import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{C285ClaimRequest, DeclarantTypeAnswer}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.C285
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{CaseType, Claimant, DeclarationMode, YesNo}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.{EisSubmitClaimRequest, GoodsDetails}
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging

class C285ClaimToTPI05Mapper extends ClaimToTPI05Mapper[C285ClaimRequest] with Logging {

  def mapToEisSubmitClaimRequest(request: C285ClaimRequest): Either[Error, EisSubmitClaimRequest] =
    TPI05.request
      .forClaimOfType(C285)
      .withClaimant(Claimant(request.claim.declarantTypeAnswer))
      .withClaimantEORI(request.signedInUserDetails.eori)
      .withClaimantEmail(request.signedInUserDetails.email)
      .withClaimedAmount(request.claim.totalReimbursementAmount)
      .withReimbursementMethod(request.claim.reimbursementMethodAnswer)
      .withCaseType(CaseType(request.claim.typeOfClaim, request.claim.reimbursementMethodAnswer))
      .withDeclarationMode(DeclarationMode(request.claim.typeOfClaim))
      .withBasisOfClaim(request.claim.basisOfClaimAnswer.value)
      .withEORIDetails(???)
      .withGoodsDetails(
        GoodsDetails(
          descOfGoods = Some(request.claim.commodityDetailsAnswer.value),
          isPrivateImporter = Some(YesNo(request.claim.declarantTypeAnswer))
        )
      )
      .verify
}
