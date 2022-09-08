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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums

import uk.gov.hmrc.cdsreimbursementclaim.utils.EnumerationFormat

sealed trait ClaimType extends Product with Serializable

object ClaimType extends EnumerationFormat[ClaimType] {

  final case object C285 extends ClaimType {
    override def toString: String = "C285"
  }

  final case object CE1179 extends ClaimType {
    override def toString: String = "C&E1179"
  }

  lazy val values: Set[ClaimType] = Set(C285, CE1179)
}
