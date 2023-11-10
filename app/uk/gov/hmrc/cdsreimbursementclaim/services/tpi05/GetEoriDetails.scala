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

package uk.gov.hmrc.cdsreimbursementclaim.services.tpi05

import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.ConsigneeDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.EoriDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.EORIInformation
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.Address
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{Country, HasClaimantInformation, Street}

trait GetEoriDetails[Claim <: HasClaimantInformation] {

  final def getEoriDetails(
    consigneeDetails: ConsigneeDetails,
    claim: Claim
  ): EoriDetails =
    EoriDetails(
      importerEORIDetails = EORIInformation.forConsignee(consigneeDetails),
      agentEORIDetails = EORIInformation(
        EORINumber = claim.claimantInformation.eori,
        CDSFullName = claim.claimantInformation.fullName,
        CDSEstablishmentAddress = Address(
          contactPerson = claim.claimantInformation.establishmentAddress.contactPerson,
          addressLine1 = Street.line1(
            claim.claimantInformation.establishmentAddress.addressLine1,
            claim.claimantInformation.establishmentAddress.addressLine2
          ),
          addressLine2 = Street.line2(
            claim.claimantInformation.establishmentAddress.addressLine1,
            claim.claimantInformation.establishmentAddress.addressLine2
          ),
          addressLine3 = claim.claimantInformation.establishmentAddress.addressLine3,
          street = claim.claimantInformation.establishmentAddress.street,
          city = claim.claimantInformation.establishmentAddress.city,
          countryCode = claim.claimantInformation.establishmentAddress.countryCode.getOrElse(Country.uk.code),
          postalCode = claim.claimantInformation.establishmentAddress.postalCode,
          telephoneNumber = claim.claimantInformation.establishmentAddress.telephoneNumber,
          emailAddress = claim.claimantInformation.establishmentAddress.emailAddress
        ),
        contactInformation = Some(claim.claimantInformation.contactInformation.withoutDuplicateAddressLines)
      )
    )
}
