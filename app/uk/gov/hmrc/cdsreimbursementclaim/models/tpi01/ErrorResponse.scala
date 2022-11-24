package uk.gov.hmrc.cdsreimbursementclaim.models.tpi01

import play.api.libs.json.{Json, OFormat}

final case class ErrorResponse(status: Int, errorDetail: Option[ErrorDetail])

object ErrorResponse {
  implicit val format: OFormat[ErrorResponse] = Json.format[ErrorResponse]
}

final case class ErrorDetail(
  timestamp: String,
  correlationId: String,
  errorCode: String,
  errorMessage: String,
  source: String,
  sourceFaultDetail: SourceFaultDetail
)

object ErrorDetail {
  implicit val format: OFormat[ErrorDetail] = Json.format[ErrorDetail]
}

final case class SourceFaultDetail(detail: Seq[String])

object SourceFaultDetail {
  implicit val format: OFormat[SourceFaultDetail] = Json.format[SourceFaultDetail]
}
