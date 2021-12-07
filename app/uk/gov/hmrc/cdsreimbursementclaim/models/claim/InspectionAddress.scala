package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import cats.Eq
import play.api.libs.json.{Format, Json}

final case class InspectionAddress(
  addressLine1: String,
  addressLine2: Option[String] = None,
  addressLine3: Option[String] = None,
  townOrCity: Option[String] = None,
  postalCode: Postcode
)

object InspectionAddress {

  implicit val equality: Eq[InspectionAddress] =
    Eq.fromUniversalEquals[InspectionAddress]

  implicit val format: Format[InspectionAddress] =
    Json.format[InspectionAddress]
}
