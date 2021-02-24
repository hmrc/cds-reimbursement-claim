package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.Address.NonUkAddress

final case class ClaimantDetailsAsImporterCompany(
  companyName: String,
  emailAddress: Email,
  phoneNumber: PhoneNumber,
  contactAddress: NonUkAddress
)

object ClaimantDetailsAsImporterCompany {
  implicit val format: OFormat[ClaimantDetailsAsImporterCompany] = Json.format[ClaimantDetailsAsImporterCompany]
}
