package uk.gov.hmrc.cdsreimbursementclaim.services.tpi05

import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.C285Claim
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.EisSubmitClaimRequest
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging

class C285ClaimToTPI05Mapper extends ClaimToTPI05Mapper[C285Claim] with Logging {

  def mapToEisSubmitClaimRequest(claim: C285Claim): Either[Error, EisSubmitClaimRequest] =
    TPI05.request
      .withBasisOfClaim()
      .verify
}
