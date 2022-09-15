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

sealed trait TemporaryAdmissionMethodOfDisposal

object TemporaryAdmissionMethodOfDisposal extends EnumerationFormat[TemporaryAdmissionMethodOfDisposal] {

  case object ExportedInSingleShipment extends TemporaryAdmissionMethodOfDisposal {
    override def toString: String = "Export: Single Shipment"
  }
  case object ExportedInMultipleShipments extends TemporaryAdmissionMethodOfDisposal {
    override def toString: String = "Export: Multiple Shipments"
  }
  case object DeclaredToOtherTraderUnderTemporaryAdmission extends TemporaryAdmissionMethodOfDisposal {
    override def toString: String = "Other Temporary Admission"
  }
  case object DeclaredToFreeCirculation extends TemporaryAdmissionMethodOfDisposal {
    override def toString: String = "Free Circulation/Home Use"
  }
  case object DeclaredToInwardProcessingRelief extends TemporaryAdmissionMethodOfDisposal {
    override def toString: String = "Inward Processing Relief"
  }
  case object DeclaredToEndUse extends TemporaryAdmissionMethodOfDisposal { override def toString: String = "End Use" }
  case object DeclaredToAFreeZone extends TemporaryAdmissionMethodOfDisposal {
    override def toString: String = "Free Zone"
  }
  case object DeclaredToACustomsWarehouse extends TemporaryAdmissionMethodOfDisposal {
    override def toString: String = "Customs Warehouse"
  }
  case object Destroyed extends TemporaryAdmissionMethodOfDisposal { override def toString: String = "Destroyed" }
  case object MultipleDisposalMethodsWereUsed extends TemporaryAdmissionMethodOfDisposal {
    override def toString: String = "Mixed"
  }
  case object Other extends TemporaryAdmissionMethodOfDisposal { override def toString: String = "Other" }

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
