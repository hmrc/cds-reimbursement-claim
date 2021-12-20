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
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.RejectedGoodsClaim
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.ISOLocalDate
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.CE1179
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{Claimant, InspectionAddressType}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.{EisSubmitClaimRequest, GoodsDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email

class CE1779ClaimToTPI05Mapper extends ClaimToTPI05Mapper[(RejectedGoodsClaim, DisplayDeclaration)] {

  def mapToEisSubmitClaimRequest(
    data: (RejectedGoodsClaim, DisplayDeclaration)
  ): Either[Error, EisSubmitClaimRequest] = {
    val claim       = data._1
    val declaration = data._2

    TPI05.request
      .forClaimOfType(CE1179)
      .withClaimant(Claimant(claim.claimantType))
      .withClaimantEmail(claim.claimantInformation.contactInformation.emailAddress.map(Email(_)))
      .withClaimantEORI(claim.claimantInformation.eori)
      .withClaimedAmount(claim.totalReimbursementAmount)
      .withReimbursementMethod(claim.reimbursementMethod)
      .withDisposalMethod(claim.methodOfDisposal)
      .withBasisOfClaim(claim.basisOfClaim.toTPI05Key)
      .withGoodsDetails(
        GoodsDetails(
          descOfGoods = Some(claim.detailsOfRejectedGoods),
          anySpecialCircumstances = claim.basisOfClaimSpecialCircumstances,
          dateOfInspection = Some(ISOLocalDate.of(claim.inspectionDate)),
          atTheImporterOrDeclarantAddress = Some(InspectionAddressType(claim.claimantType)),
          inspectionAddress = Some(claim.inspectionAddress)
        )
      )
      //      .withEORIDetails(
      //        EoriDetails(
      //          agentEORIDetails = ???,
      //          importerEORIDetails = ??? // from Acc14
      //        )
      //      )
      .verify
  }
}
