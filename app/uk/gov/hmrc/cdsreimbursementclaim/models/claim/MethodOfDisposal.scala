package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import cats.Eq
import julienrf.json.derived
import play.api.libs.json.OFormat

sealed trait MethodOfDisposal extends Product with Serializable

object MethodOfDisposal {

  case object Export extends MethodOfDisposal
  case object PostalExport extends MethodOfDisposal
  case object DonationToCharity extends MethodOfDisposal
  case object PlacedInCustomsWarehouse extends MethodOfDisposal
  case object ExportInBaggage extends MethodOfDisposal
  case object Destruction extends MethodOfDisposal

  implicit val equality: Eq[MethodOfDisposal] =
    Eq.fromUniversalEquals[MethodOfDisposal]

  implicit val format: OFormat[MethodOfDisposal] =
    derived.oformat[MethodOfDisposal]()
}
