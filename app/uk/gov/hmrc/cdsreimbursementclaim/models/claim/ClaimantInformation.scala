package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.ContactInformation
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori

final case class ClaimantInformation(
  eori: Eori,
  fullName: Option[String],
  establishmentAddress: ContactInformation,
  contactInformation: ContactInformation
)
