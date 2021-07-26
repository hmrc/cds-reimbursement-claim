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

import julienrf.json.derived
import play.api.libs.json.OFormat

sealed trait UploadDocumentType extends Product with Serializable

object UploadDocumentType {
  case object C88E2 extends UploadDocumentType {
    override def toString: String = "C88E2"
  }
  case object CommercialInvoice extends UploadDocumentType {
    override def toString: String = "Commercial Invoice"
  }
  case object PackingList extends UploadDocumentType {
    override def toString: String = "Packing List"
  }
  case object AirWayBill extends UploadDocumentType {
    override def toString: String = "AirWay Bill"
  }
  case object BillOfLading extends UploadDocumentType {
    override def toString: String = "Bill Of Lading"
  }
  case object SubstituteEntry extends UploadDocumentType {
    override def toString: String = "Substitute Entry"
  }
  case object ScheduleOfMRNs extends UploadDocumentType {
    override def toString: String = "Schedule Of MRNs"
  }
  case object ProofOfAuthority extends UploadDocumentType {
    override def toString: String = "Proof Of Authority"
  }
  case object CorrespondenceTrader extends UploadDocumentType {
    override def toString: String = "Correspondence Trader"
  }
  case object AdditionalSupportingDocuments extends UploadDocumentType {
    override def toString: String = "Additional Supporting Documents"
  }
  case object ImportAndExportDeclaration extends UploadDocumentType {
    override def toString: String = "Import and Export Declaration"
  }
  case object Other extends UploadDocumentType {
    override def toString: String = "Other"
  }

  implicit val format: OFormat[UploadDocumentType] = derived.oformat[UploadDocumentType]()
}
