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

sealed trait UploadDocumentType {
  def toDec64DisplayString: String
}

object UploadDocumentType extends EnumerationFormat[UploadDocumentType] {

  case object CommercialInvoice extends UploadDocumentType {
    override def toDec64DisplayString: String = "Commercial Invoice"
  }

  case object PackingList extends UploadDocumentType {
    override def toDec64DisplayString: String = "Packing List"
  }

  case object ExportPackingList extends UploadDocumentType {
    override def toDec64DisplayString: String = "Packing List"
  }

  case object ImportPackingList extends UploadDocumentType {
    override def toDec64DisplayString: String = "Packing List"
  }

  case object AirWayBill extends UploadDocumentType {
    override def toDec64DisplayString: String = "Air Waybill"
  }

  case object BillOfLading extends UploadDocumentType {
    override def toDec64DisplayString: String = "Bill of Lading"
  }

  case object SubstituteEntry extends UploadDocumentType {
    override def toDec64DisplayString: String = "Substitute Entry"
  }

  case object SubstituteOrDiversionEntry extends UploadDocumentType {
    override def toDec64DisplayString: String = "Substitute Entry"
  }

  case object ScheduleOfMRNs extends UploadDocumentType {
    override def toDec64DisplayString: String = "Schedule of MRNs"
  }

  case object ProofOfAuthority extends UploadDocumentType {
    override def toDec64DisplayString: String = "Proof of Authority (to be repaid)"
  }

  case object CorrespondenceTrader extends UploadDocumentType {
    override def toDec64DisplayString: String = "Correspondence Trader"
  }

  case object AdditionalSupportingDocuments extends UploadDocumentType {
    override def toDec64DisplayString: String = "Additional Supporting Documentation"
  }

  case object QuotaLicense extends UploadDocumentType {
    override def toDec64DisplayString: String = "Additional Supporting Documentation"
  }

  case object ProofOfOrigin extends UploadDocumentType {
    override def toDec64DisplayString: String = "Additional Supporting Documentation"
  }

  case object ProofOfEligibility extends UploadDocumentType {
    override def toDec64DisplayString: String = "Additional Supporting Documentation"
  }

  case object BillOfDischarge3 extends UploadDocumentType {
    override def toDec64DisplayString: String = "Additional Supporting Documentation"
  }

  case object BillOfDischarge4 extends UploadDocumentType {
    override def toDec64DisplayString: String = "Additional Supporting Documentation"
  }

  case object ImportAndExportDeclaration extends UploadDocumentType {
    override def toDec64DisplayString: String = "Import and Export Declaration"
  }

  case object ImportDeclaration extends UploadDocumentType {
    override def toDec64DisplayString: String = "Import and Export Declaration"
  }

  case object ExportDeclaration extends UploadDocumentType {
    override def toDec64DisplayString: String = "Import and Export Declaration"
  }

  case object CalculationWorksheet extends UploadDocumentType {
    override def toDec64DisplayString: String = "Calculation worksheet"
  }

  case object DocumentaryProofFaultyOrNotWhatOrdered extends UploadDocumentType {
    override def toDec64DisplayString: String = "Documentary proof that the goods are faulty or not what you ordered"
  }

  case object ProofOfExportOrDestruction extends UploadDocumentType {
    override def toDec64DisplayString: String = "Proof of export or destruction"
  }

  case object LetterOfAuthority extends UploadDocumentType {
    override def toDec64DisplayString: String = "Proof of Authority (to be repaid)"
  }

  case object Other extends UploadDocumentType {
    override def toDec64DisplayString: String = "Other"
  }

  case object SupportingEvidence extends UploadDocumentType {
    override def toDec64DisplayString: String = "Other"
  }

  case object CalculationWorksheetOrFinalSalesFigures extends UploadDocumentType {
    override def toDec64DisplayString: String = "Other"
  }

  case object ClaimWorksheet extends UploadDocumentType {
    override def toDec64DisplayString: String = "Other"
  }

  lazy val values: Set[UploadDocumentType] = Set(
    AirWayBill,
    BillOfLading,
    CommercialInvoice,
    CorrespondenceTrader,
    ImportDeclaration,
    ExportDeclaration,
    ImportAndExportDeclaration,
    PackingList,
    ExportPackingList,
    ImportPackingList,
    ProofOfAuthority,
    ProofOfEligibility,
    ProofOfOrigin,
    SubstituteEntry,
    SubstituteOrDiversionEntry,
    ScheduleOfMRNs,
    Other,
    CalculationWorksheet,
    CalculationWorksheetOrFinalSalesFigures,
    DocumentaryProofFaultyOrNotWhatOrdered,
    ProofOfExportOrDestruction,
    AdditionalSupportingDocuments,
    LetterOfAuthority,
    SupportingEvidence,
    BillOfDischarge3,
    BillOfDischarge4,
    QuotaLicense,
    ClaimWorksheet
  )
}
