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

package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import uk.gov.hmrc.cdsreimbursementclaim.utils.EnumerationFormat

sealed trait InspectionAddressType {
  def toTPI05DisplayString: String
}

object InspectionAddressType extends EnumerationFormat[InspectionAddressType] {

  final case object Importer extends InspectionAddressType {
    override def toTPI05DisplayString: String = "Importer"
  }

  final case object Declarant extends InspectionAddressType {
    override def toTPI05DisplayString: String = "Declarant or Representative"
  }

  final case object Other extends InspectionAddressType {
    override def toTPI05DisplayString: String = "Other"
  }

  override val values: Set[InspectionAddressType] =
    Set(Importer, Declarant, Other)
}
