package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import play.api.libs.json.{Json, OFormat}

final case class CompleteClaimantDetailsAsImporterCompanyAnswer(
  claimantDetailsAsImporterCompany: ClaimantDetailsAsImporterCompany
)

object CompleteClaimantDetailsAsImporterCompanyAnswer{
  implicit val format: OFormat[CompleteClaimantDetailsAsImporterCompanyAnswer] = Json.format[CompleteClaimantDetailsAsImporterCompanyAnswer]
}