package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import cats.Eq
import play.api.libs.json.Format
import uk.gov.hmrc.cdsreimbursementclaim.utils.EnumerationFormat

sealed trait ClaimantType

object ClaimantType {

  case object Consignee extends ClaimantType
  case object Declarant extends ClaimantType
  case object User extends ClaimantType

  private val mappings: Map[String, ClaimantType] =
    Seq(Consignee, Declarant, User)
      .map(item => (item.toString, item))
      .toMap

  implicit val format: Format[ClaimantType] = EnumerationFormat(mappings)

  implicit val equality: Eq[ClaimantType] = Eq.fromUniversalEquals[ClaimantType]
}
