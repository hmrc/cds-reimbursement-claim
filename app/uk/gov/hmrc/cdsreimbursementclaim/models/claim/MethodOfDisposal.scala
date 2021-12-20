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

package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import uk.gov.hmrc.cdsreimbursementclaim.utils.EnumerationFormat

sealed trait MethodOfDisposal extends Product with Serializable {
  def toTPI05Key: String
}

object MethodOfDisposal extends EnumerationFormat[MethodOfDisposal] {

  final case object Export extends MethodOfDisposal {
    def toTPI05Key: String = "Export"
  }

  final case object PostalExport extends MethodOfDisposal {
    def toTPI05Key: String = "Postal Export"
  }

  final case object DonationToCharity extends MethodOfDisposal {
    def toTPI05Key: String = "Donation to Charity"
  }

  final case object PlacedInCustomsWarehouse extends MethodOfDisposal {
    def toTPI05Key: String = "Placed in Customs Warehouse"
  }

  final case object ExportInBaggage extends MethodOfDisposal {
    def toTPI05Key: String = "Export in Baggage"
  }

  final case object Destruction extends MethodOfDisposal {
    def toTPI05Key: String = "Destruction"
  }

  override val values: Set[MethodOfDisposal] =
    Set(Export, PostalExport, DonationToCharity, PlacedInCustomsWarehouse, ExportInBaggage, Destruction)
}
