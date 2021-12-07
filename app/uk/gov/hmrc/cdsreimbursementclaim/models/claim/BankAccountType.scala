package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import cats.Eq
import julienrf.json.derived
import play.api.libs.json.OFormat

sealed trait BankAccountType extends Product with Serializable

object BankAccountType {

  case object BusinessBankAccount extends BankAccountType
  case object PersonalBankAccount extends BankAccountType

  implicit val equality: Eq[BankAccountType] =
    Eq.fromUniversalEquals[BankAccountType]

  implicit val format: OFormat[BankAccountType] =
    derived.oformat[BankAccountType]()
}
