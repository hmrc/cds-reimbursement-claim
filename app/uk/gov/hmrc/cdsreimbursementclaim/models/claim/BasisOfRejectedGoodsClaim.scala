package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import cats.Eq
import julienrf.json.derived
import play.api.libs.json.OFormat

sealed trait BasisOfRejectedGoodsClaim extends Product with Serializable

object BasisOfRejectedGoodsClaim {

  case object DamagedBeforeClearance extends BasisOfRejectedGoodsClaim
  case object Defective extends BasisOfRejectedGoodsClaim
  case object NotInAccordanceWithContract extends BasisOfRejectedGoodsClaim
  case object SpecialCircumstances extends BasisOfRejectedGoodsClaim

  implicit val equality: Eq[BasisOfRejectedGoodsClaim] =
    Eq.fromUniversalEquals[BasisOfRejectedGoodsClaim]

  implicit val format: OFormat[BasisOfRejectedGoodsClaim] =
    derived.oformat[BasisOfRejectedGoodsClaim]()
}
