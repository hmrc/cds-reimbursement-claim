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

import cats.data.NonEmptyList
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN

package object answers {

  type AssociatedMrn = MRN

  type AssociatedMRNsAnswer = NonEmptyList[AssociatedMrn]

  type SupportingEvidencesAnswer = NonEmptyList[UploadDocument]

  object SupportingEvidencesAnswer {
    def apply(evidence: UploadDocument): SupportingEvidencesAnswer =
      NonEmptyList.one(evidence)
  }

  type ClaimsAnswer = NonEmptyList[Claim]

  object ClaimsAnswer {
    def apply(head: Claim, tail: Claim*): NonEmptyList[Claim] = NonEmptyList.of(head, tail: _*)
    def apply(l: List[Claim]): Option[NonEmptyList[Claim]]    = NonEmptyList.fromList(l)
  }

}
