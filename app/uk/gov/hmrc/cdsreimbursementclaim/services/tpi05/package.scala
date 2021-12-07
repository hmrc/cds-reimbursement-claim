package uk.gov.hmrc.cdsreimbursementclaim.services

import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.EisSubmitClaimRequest

import scala.annotation.implicitNotFound

package object tpi05 {

  implicit val c285ClaimToTPI05Mapper: C285ClaimToTPI05Mapper     = new C285ClaimToTPI05Mapper
  implicit val ce1779ClaimToTPI05Mapper: CE1779ClaimToTPI05Mapper = new CE1779ClaimToTPI05Mapper

  implicit class ClaimOps[A](val claim: A) extends AnyVal {
    @implicitNotFound("No implicit TPI05 mapper found for request object")
    def toEisSubmitClaimRequest(a: A)(implicit mapper: ClaimToTPI05Mapper[A]): Either[Error, EisSubmitClaimRequest] =
      mapper mapToEisSubmitClaimRequest a
  }
}
