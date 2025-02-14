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

package uk.gov.hmrc.cdsreimbursementclaim.models

import cats.data.NonEmptyList
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN

package object claim {

  type AssociatedMRNsAnswer       = NonEmptyList[MRN]
  type AssociatedMRNsClaimsAnswer = NonEmptyList[ClaimedReimbursementsAnswer]

  type ClaimedReimbursementsAnswer = NonEmptyList[ClaimedReimbursement]

  object ClaimedReimbursementsAnswer {

    def apply(head: ClaimedReimbursement, tail: ClaimedReimbursement*): NonEmptyList[ClaimedReimbursement] =
      NonEmptyList.of(head, tail*)

    def apply(items: List[ClaimedReimbursement]): Option[NonEmptyList[ClaimedReimbursement]] =
      NonEmptyList.fromList(items)
  }
}
