package uk.gov.hmrc.cdsreimbursementclaim.services.tpi05

import uk.gov.hmrc.cdsreimbursementclaim.models
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.CE1779Claim
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.EisSubmitClaimRequest

class CE1779ClaimToTPI05Mapper extends ClaimToTPI05Mapper[CE1779Claim] {

  def mapToEisSubmitClaimRequest(claim: CE1779Claim): Either[models.Error, EisSubmitClaimRequest] = ???
}
