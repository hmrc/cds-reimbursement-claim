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

import org.scalacheck.Gen
import org.scalacheck.ScalacheckShapeless._
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.SupportingEvidence
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.SupportingEvidenceAnswer.CompleteSupportingEvidenceAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.upscan.UpscanCallBack.{UploadDetails, UpscanSuccess}
import uk.gov.hmrc.cdsreimbursementclaim.models.upscan.{UploadReference, UpscanUpload}

object UpscanGen extends GenUtils {
  implicit val upscanUploadGen: Gen[UpscanUpload]                                         = gen[UpscanUpload]
  implicit val uploadReferenceGen: Gen[UploadReference]                                   = gen[UploadReference]
  implicit val upscanSuccessGen: Gen[UpscanSuccess]                                       = gen[UpscanSuccess]
  implicit val uploadDetailsGen: Gen[UploadDetails]                                       = gen[UploadDetails]
  implicit val supportingEvidenceGen: Gen[SupportingEvidence]                             = gen[SupportingEvidence]
  implicit val completeSupportingEvidenceAnswerGen: Gen[CompleteSupportingEvidenceAnswer] =
    gen[CompleteSupportingEvidenceAnswer]
}
