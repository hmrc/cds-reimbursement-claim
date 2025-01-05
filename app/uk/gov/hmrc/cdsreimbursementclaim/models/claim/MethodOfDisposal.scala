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

package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import uk.gov.hmrc.cdsreimbursementclaim.utils.EnumerationFormat

sealed trait MethodOfDisposal extends Product with Serializable {
  def toTPI05DisplayString: String
}

object MethodOfDisposal extends EnumerationFormat[MethodOfDisposal] {

  case object Export extends MethodOfDisposal {
    def toTPI05DisplayString: String = "Export"
  }

  case object PostalExport extends MethodOfDisposal {
    def toTPI05DisplayString: String = "Postal Export"
  }

  case object DonationToCharity extends MethodOfDisposal {
    def toTPI05DisplayString: String = "Donation to Charity"
  }

  case object PlacedInCustomsWarehouse extends MethodOfDisposal {
    def toTPI05DisplayString: String = "Placed in Custom Warehouse"
  }

  case object ExportInBaggage extends MethodOfDisposal {
    def toTPI05DisplayString: String = "Export in Baggage"
  }

  case object Destruction extends MethodOfDisposal {
    def toTPI05DisplayString: String = "Destruction"
  }

  override lazy val values: Set[MethodOfDisposal] =
    Set(Export, PostalExport, DonationToCharity, PlacedInCustomsWarehouse, ExportInBaggage, Destruction)
}
