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

import uk.gov.hmrc.cdsreimbursementclaim.models.claim.TypeOfClaimAnswer
import uk.gov.hmrc.cdsreimbursementclaim.utils.EnumerationFormat

sealed trait DeclarationMode extends Product with Serializable

object DeclarationMode extends EnumerationFormat[DeclarationMode] {

  final case object ParentDeclaration extends DeclarationMode {
    override def toString: String = "Parent Declaration"
  }

  final case object AllDeclaration extends DeclarationMode {
    override def toString: String = "All Declarations"
  }

  def basedOn(typeOfClaim: TypeOfClaimAnswer): DeclarationMode =
    typeOfClaim match {
      case TypeOfClaimAnswer.Multiple => DeclarationMode.AllDeclaration
      case _                          => DeclarationMode.ParentDeclaration
    }

  lazy val values: Set[DeclarationMode] = Set(ParentDeclaration, AllDeclaration)
}
