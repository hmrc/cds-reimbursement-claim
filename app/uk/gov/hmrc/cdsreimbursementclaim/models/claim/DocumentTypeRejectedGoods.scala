package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import cats.Eq
import julienrf.json.derived
import play.api.libs.json.OFormat

sealed trait DocumentTypeRejectedGoods extends Product with Serializable

object DocumentTypeRejectedGoods {

  case object Foo extends DocumentTypeRejectedGoods

  implicit val equality: Eq[DocumentTypeRejectedGoods] =
    Eq.fromUniversalEquals[DocumentTypeRejectedGoods]

  implicit val format: OFormat[DocumentTypeRejectedGoods] =
    derived.oformat[DocumentTypeRejectedGoods]()
}
