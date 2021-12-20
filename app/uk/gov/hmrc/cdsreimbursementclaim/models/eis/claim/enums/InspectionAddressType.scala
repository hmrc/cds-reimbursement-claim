/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.Writes
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ClaimantType
import uk.gov.hmrc.cdsreimbursementclaim.utils.EnumerationToStringWrites

sealed trait InspectionAddressType

object InspectionAddressType {

  final case object Importer extends InspectionAddressType
  final case object Declarant extends InspectionAddressType
  final case object Other extends InspectionAddressType

  def apply(claimantType: ClaimantType): InspectionAddressType =
    claimantType match {
      case ClaimantType.Consignee => Importer
      case ClaimantType.Declarant => Declarant
      case ClaimantType.User      => Other
    }

  implicit val writes: Writes[InspectionAddressType] = EnumerationToStringWrites[InspectionAddressType]
}
