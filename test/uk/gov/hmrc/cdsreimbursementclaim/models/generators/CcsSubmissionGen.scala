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

import org.scalacheck.magnolia._
import uk.gov.hmrc.cdsreimbursementclaim.models.ccs.CcsSubmissionPayload
import uk.gov.hmrc.cdsreimbursementclaim.services.ccs.CcsSubmissionRequest
import uk.gov.hmrc.mongo.workitem.WorkItem

object CcsSubmissionGen {

  implicit lazy val arbitraryCCSSubmissionPayload: Typeclass[CcsSubmissionPayload] = gen[CcsSubmissionPayload]
  implicit lazy val arbitraryCCSSubmissionRequest: Typeclass[CcsSubmissionRequest] = gen[CcsSubmissionRequest]
  implicit lazy val arbitraryWorkItem: Typeclass[WorkItem[CcsSubmissionRequest]]   = gen[WorkItem[CcsSubmissionRequest]]

}
