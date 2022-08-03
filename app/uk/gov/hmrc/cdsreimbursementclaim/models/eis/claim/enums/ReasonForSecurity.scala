/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.cdsreimbursementclaim.utils.EnumerationFormat
import play.api.mvc.QueryStringBindable

sealed class ReasonForSecurity(val acc14Code: String)

object ReasonForSecurity extends EnumerationFormat[ReasonForSecurity] {

  case object AccountSales extends ReasonForSecurity("ACS")
  case object CommunitySystemsOfDutyRelief extends ReasonForSecurity("MDC")
  case object EndUseRelief extends ReasonForSecurity("ENU")
  case object InwardProcessingRelief extends ReasonForSecurity("IPR")
  case object ManualOverrideDeposit extends ReasonForSecurity("MOD")
  case object MissingLicenseQuota extends ReasonForSecurity("MDL")
  case object MissingPreferenceCertificate extends ReasonForSecurity("MDP")
  case object OutwardProcessingRelief extends ReasonForSecurity("OPR")
  case object RevenueDispute extends ReasonForSecurity("RED")
  case object TemporaryAdmission2Y extends ReasonForSecurity("T24")
  case object TemporaryAdmission6M extends ReasonForSecurity("TA6")
  case object TemporaryAdmission3M extends ReasonForSecurity("TA3")
  case object TemporaryAdmission2M extends ReasonForSecurity("TA2")
  case object UKAPEntryPrice extends ReasonForSecurity("CEP")
  case object UKAPSafeguardDuties extends ReasonForSecurity("CSD")

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
      UKAPSafeguardDuties
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
}
