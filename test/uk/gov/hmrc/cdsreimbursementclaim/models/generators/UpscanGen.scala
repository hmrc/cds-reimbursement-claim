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

package uk.gov.hmrc.cdsreimbursementclaim.models.generators

import org.scalacheck.magnolia._
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.UploadDocument
import uk.gov.hmrc.cdsreimbursementclaim.models.upscan.UpscanCallBack.{UploadDetails, UpscanSuccess}
import uk.gov.hmrc.cdsreimbursementclaim.models.upscan.{UploadReference, UpscanUpload}

object UpscanGen {
  implicit lazy val uploadReferenceGen: Typeclass[UploadReference] = gen[UploadReference]
  implicit lazy val uploadDetailsGen: Typeclass[UploadDetails]     = gen[UploadDetails]
  implicit lazy val upscanSuccessGen: Typeclass[UpscanSuccess]     = gen[UpscanSuccess]
  implicit lazy val uploadDocumentGen: Typeclass[UploadDocument]   = gen[UploadDocument]
  implicit lazy val upscanUploadGen: Typeclass[UpscanUpload]       = gen[UpscanUpload]
}
