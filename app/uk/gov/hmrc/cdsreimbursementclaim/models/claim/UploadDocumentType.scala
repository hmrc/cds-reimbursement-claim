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

sealed trait UploadDocumentType {
  def toTPI05Key: String
}

object UploadDocumentType extends EnumerationFormat[UploadDocumentType] {

  case object CommercialInvoice extends UploadDocumentType {
    override def toTPI05Key: String = "Commercial Invoice"
  }
  case object PackingList extends UploadDocumentType {
    override def toTPI05Key: String = "Packing List"
  }
  case object AirWayBill extends UploadDocumentType {
    override def toTPI05Key: String = "Air Waybill"
  }
  case object BillOfLading extends UploadDocumentType {
    override def toTPI05Key: String = "Bill of Lading"
  }
  case object SubstituteEntry extends UploadDocumentType {
    override def toTPI05Key: String = "Substitute Entry"
  }
  case object ScheduleOfMRNs extends UploadDocumentType {
    override def toTPI05Key: String = "Schedule of MRNs"
  }
  case object ProofOfAuthority extends UploadDocumentType {
    override def toTPI05Key: String = "Proof of Authority (to be repaid)"
  }
  case object CorrespondenceTrader extends UploadDocumentType {
    override def toTPI05Key: String = "Correspondence Trader"
  }
  case object AdditionalSupportingDocuments extends UploadDocumentType {
    override def toTPI05Key: String = "Additional Supporting Documentation"
  }
  case object ImportAndExportDeclaration extends UploadDocumentType {
    override def toTPI05Key: String = "Import and Export Declaration"
  }
  case object Other extends UploadDocumentType {
    override def toTPI05Key: String = "Other"
  }

  val values: Set[UploadDocumentType] = Set(
    CommercialInvoice,
    PackingList,
    AirWayBill,
    BillOfLading,
    SubstituteEntry,
    ScheduleOfMRNs,
    ProofOfAuthority,
    CorrespondenceTrader,
    AdditionalSupportingDocuments,
    ImportAndExportDeclaration,
    Other
  )
}
