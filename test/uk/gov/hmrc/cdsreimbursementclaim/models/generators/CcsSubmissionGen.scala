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

package uk.gov.hmrc.cdsreimbursementclaim.models.generators

import uk.gov.hmrc.cdsreimbursementclaim.models.ccs.CcsSubmissionPayload
import uk.gov.hmrc.cdsreimbursementclaim.services.ccs.CcsSubmissionRequest
import uk.gov.hmrc.mongo.workitem.WorkItem
import org.scalacheck.Arbitrary
import uk.gov.hmrc.mongo.workitem.ProcessingStatus
import org.scalacheck.Gen

object CcsSubmissionGen {

  implicit lazy val arbitraryCCSSubmissionPayload: Arbitrary[CcsSubmissionPayload] =
    GeneratorUtils.gen[CcsSubmissionPayload]

  implicit lazy val arbitraryCCSSubmissionRequest: Arbitrary[CcsSubmissionRequest] =
    GeneratorUtils.gen[CcsSubmissionRequest]

  implicit lazy val arbitraryProcessingStatus: Arbitrary[ProcessingStatus] =
    Arbitrary(Gen.const(ProcessingStatus.InProgress))

  implicit lazy val arbitraryWorkItem: Arbitrary[WorkItem[CcsSubmissionRequest]] =
    GeneratorUtils.gen[WorkItem[CcsSubmissionRequest]]

}
