package uk.gov.hmrc.cdsreimbursementclaim.services.tpi05

import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.EisSubmitClaimRequest

trait ClaimToTPI05Mapper[A] {
  def mapToEisSubmitClaimRequest(claim: A): Either[Error, EisSubmitClaimRequest]
}
