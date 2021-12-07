package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.{Eori, MRN}

import java.time.LocalDate

final case class CE1779Claim(
  movementReferenceNumber: MRN,
  declarantType: DeclarantTypeAnswer,
  basisOfClaim: BasisOfRejectedGoodsClaim,
  methodOfDisposal: MethodOfDisposal,
  detailsOfRejectedGoods: String,
  inspectionDate: LocalDate,
  inspectionAddress: InspectionAddress,
  reimbursementClaims: Map[TaxCode, BigDecimal],
  supportingEvidences: Map[UploadDocument, DocumentTypeRejectedGoods],
  basisOfClaimSpecialCircumstances: Option[String],
  reimbursementMethod: ReimbursementMethodAnswer,
  consigneeEoriNumber: Eori,
  declarantEoriNumber: Eori,
  contactDetails: MrnContactDetails,
  contactAddress: ContactAddress,
  bankAccountDetailsAndType: Option[(BankAccountDetails, BankAccountType)]
)

object CE1779Claim {

  implicit val format: Format[CE1779Claim] = Json.format[CE1779Claim]
}
