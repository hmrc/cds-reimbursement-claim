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

sealed trait TemporaryAdmissionMethodOfDisposal {
  val eisCode: String
}

object TemporaryAdmissionMethodOfDisposal extends EnumerationFormat[TemporaryAdmissionMethodOfDisposal] {

  case object ExportedInSingleShipment extends TemporaryAdmissionMethodOfDisposal {
    val eisCode: String = "Export: Single Shipment"
  }
  case object ExportedInMultipleShipments extends TemporaryAdmissionMethodOfDisposal {
    val eisCode: String = "Export: Multiple Shipments"
  }
  case object DeclaredToOtherTraderUnderTemporaryAdmission extends TemporaryAdmissionMethodOfDisposal {
    val eisCode: String = "Other Temporary Admission"
  }
  case object DeclaredToFreeCirculation extends TemporaryAdmissionMethodOfDisposal {
    val eisCode: String = "Free Circulation/Home Use"
  }
  case object DeclaredToInwardProcessingRelief extends TemporaryAdmissionMethodOfDisposal {
    val eisCode: String = "Inward Processing Relief"
  }
  case object DeclaredToEndUse extends TemporaryAdmissionMethodOfDisposal { val eisCode: String = "End Use" }
  case object DeclaredToAFreeZone extends TemporaryAdmissionMethodOfDisposal {
    val eisCode: String = "Free Zone"
  }
  case object DeclaredToACustomsWarehouse extends TemporaryAdmissionMethodOfDisposal {
    val eisCode: String = "Customs Warehouse"
  }
  case object Destroyed extends TemporaryAdmissionMethodOfDisposal { val eisCode: String = "Destroyed" }
  case object MultipleDisposalMethodsWereUsed extends TemporaryAdmissionMethodOfDisposal {
    val eisCode: String = "Mixed"
  }
  case object Other extends TemporaryAdmissionMethodOfDisposal { val eisCode: String = "Other" }

  override val values: Set[TemporaryAdmissionMethodOfDisposal] =
    Set(
      ExportedInSingleShipment,
      ExportedInMultipleShipments,
      DeclaredToOtherTraderUnderTemporaryAdmission,
      DeclaredToFreeCirculation,
      DeclaredToInwardProcessingRelief,
      DeclaredToEndUse,
      DeclaredToAFreeZone,
      DeclaredToACustomsWarehouse,
      Destroyed,
      Other,
      MultipleDisposalMethodsWereUsed
    )

  val requiresMrn: Set[TemporaryAdmissionMethodOfDisposal] =
    Set(
      ExportedInSingleShipment,
      ExportedInMultipleShipments
    )
}
