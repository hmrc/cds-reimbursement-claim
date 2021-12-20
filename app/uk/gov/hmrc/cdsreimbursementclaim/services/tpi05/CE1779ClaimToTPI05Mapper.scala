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
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.RejectedGoodsClaimRequest
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.ISOLocalDate
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.CE1179
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{Claimant, InspectionAddressType}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.{EisSubmitClaimRequest, GoodsDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging

import javax.inject.{Inject, Singleton}

@Singleton
class CE1779ClaimToTPI05Mapper @Inject() () extends ClaimToTPI05Mapper[RejectedGoodsClaimRequest] with Logging {

  def mapToEisSubmitClaimRequest(request: RejectedGoodsClaimRequest): Either[Error, EisSubmitClaimRequest] =
    TPI05.request
      .forClaimOfType(CE1179)
      .withClaimant(Claimant(request.claim.claimantType))
      .withClaimantEmail(request.claim.claimantInformation.contactInformation.emailAddress.map(Email(_)))
      .withClaimantEORI(request.claim.claimantInformation.eori)
      .withClaimedAmount(request.claim.totalReimbursementAmount)
      .withReimbursementMethod(request.claim.reimbursementMethod)
      .withDisposalMethod(request.claim.methodOfDisposal)
      .withBasisOfClaim(request.claim.basisOfClaim.toTPI05Key)
      .withGoodsDetails(
        GoodsDetails(
          descOfGoods = Some(request.claim.detailsOfRejectedGoods),
          anySpecialCircumstances = request.claim.basisOfClaimSpecialCircumstances,
          dateOfInspection = Some(ISOLocalDate.of(request.claim.inspectionDate)),
          atTheImporterOrDeclarantAddress = Some(InspectionAddressType(request.claim.claimantType)),
          inspectionAddress = Some(request.claim.inspectionAddress)
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
