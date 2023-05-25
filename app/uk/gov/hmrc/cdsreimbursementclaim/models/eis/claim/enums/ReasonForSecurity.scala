/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums

import cats.syntax.eq._
import uk.gov.hmrc.cdsreimbursementclaim.utils.EnumerationFormat
import play.api.mvc.QueryStringBindable
import collection.immutable.Seq

sealed class ReasonForSecurity(val acc14Code: String, val dec64DisplayString: String)

object ReasonForSecurity extends EnumerationFormat[ReasonForSecurity] {

  case object AccountSales extends ReasonForSecurity("ACS", "CAP Account Sales")
  case object CommunitySystemsOfDutyRelief extends ReasonForSecurity("MDC", "Missing Document: CSDR")
  case object EndUseRelief extends ReasonForSecurity("ENU", "End Use")
  case object InwardProcessingRelief extends ReasonForSecurity("IPR", "Inward Processing Relief")
  case object ManualOverrideDeposit extends ReasonForSecurity("MOD", "Manual Override Deposit")
  case object MissingLicenseQuota extends ReasonForSecurity("MDL", "Missing Document Licence Quota")
  case object MissingPreferenceCertificate extends ReasonForSecurity("MDP", "Missing Document Preference")
  case object OutwardProcessingRelief extends ReasonForSecurity("OPR", "Outward Processing Relief")
  case object RevenueDispute extends ReasonForSecurity("RED", "Revenue Dispute")
  case object TemporaryAdmission2Y extends ReasonForSecurity("T24", "Temporary Admission (2 years Expiration)")
  case object TemporaryAdmission6M extends ReasonForSecurity("TA6", "Temporary Admission (6 months Expiration)")
  case object TemporaryAdmission3M extends ReasonForSecurity("TA3", "Temporary Admission (3 months Expiration)")
  case object TemporaryAdmission2M extends ReasonForSecurity("TA2", "Temporary Admission (2 months Expiration)")
  case object UKAPEntryPrice extends ReasonForSecurity("CEP", "CAP Entry Price")
  case object UKAPSafeguardDuties extends ReasonForSecurity("CSD", "CAP Safeguard Duties")
  case object ProvisionalDuty extends ReasonForSecurity("PDD", "Provisional Duty")
  case object Quota extends ReasonForSecurity("CRQ", "Quota")

  override val values: Set[ReasonForSecurity] =
    Set(
      AccountSales,
      CommunitySystemsOfDutyRelief,
      EndUseRelief,
      InwardProcessingRelief,
      ManualOverrideDeposit,
      MissingLicenseQuota,
      MissingPreferenceCertificate,
      OutwardProcessingRelief,
      RevenueDispute,
      TemporaryAdmission2Y,
      TemporaryAdmission6M,
      TemporaryAdmission3M,
      TemporaryAdmission2M,
      UKAPEntryPrice,
      UKAPSafeguardDuties,
      ProvisionalDuty,
      Quota
    )

  val temporaryAdmissions: Set[ReasonForSecurity] =
    Set(
      TemporaryAdmission2Y,
      TemporaryAdmission6M,
      TemporaryAdmission3M,
      TemporaryAdmission2M
    )

  implicit val binder: QueryStringBindable[ReasonForSecurity] = new QueryStringBindable[ReasonForSecurity] {
    private val strBinder: QueryStringBindable[String] = implicitly[QueryStringBindable[String]]

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ReasonForSecurity]] =
      params
        .get(key)
        .flatMap(_.headOption)
        .flatMap(reason => ReasonForSecurity.parse(reason))
        .map(Right(_))

    override def unbind(key: String, value: ReasonForSecurity): String = strBinder.unbind(key, value.toString)
  }

  def parseACC14Code(code: String): Option[ReasonForSecurity] =
    values.find(_.acc14Code === code)
}
