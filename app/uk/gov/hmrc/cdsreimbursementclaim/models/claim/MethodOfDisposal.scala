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

import cats.Eq
import julienrf.json.derived
import play.api.libs.json.OFormat

sealed trait MethodOfDisposal extends Product with Serializable

object MethodOfDisposal {

  case object Export extends MethodOfDisposal {
    override def toString: String = "Export"
  }

  case object PostalExport extends MethodOfDisposal {
    override def toString: String = "Postal Export"
  }

  case object DonationToCharity extends MethodOfDisposal {
    override def toString: String = "Donation to Charity"
  }

  case object PlacedInCustomsWarehouse extends MethodOfDisposal {
    override def toString: String = "Placed in Customs Warehouse"
  }

  case object ExportInBaggage extends MethodOfDisposal {
    override def toString: String = "Export in Baggage"
  }

  case object Destruction extends MethodOfDisposal {
    override def toString: String = "Destruction"
  }

  implicit val equality: Eq[MethodOfDisposal] =
    Eq.fromUniversalEquals[MethodOfDisposal]

  implicit val format: OFormat[MethodOfDisposal] =
    derived.oformat[MethodOfDisposal]()
}
