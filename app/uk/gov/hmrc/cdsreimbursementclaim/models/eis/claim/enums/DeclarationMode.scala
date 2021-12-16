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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums

import play.api.libs.json.{JsString, Writes}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.TypeOfClaimAnswer

sealed trait DeclarationMode extends Product with Serializable

object DeclarationMode {

  final case object ParentDeclaration extends DeclarationMode {
    override def toString: String = "Parent Declaration"
  }

  final case object AllDeclaration extends DeclarationMode {
    override def toString: String = "All Declarations"
  }

  def apply(typeOfClaim: TypeOfClaimAnswer): DeclarationMode =
    typeOfClaim match {
      case TypeOfClaimAnswer.Multiple => DeclarationMode.AllDeclaration
      case _                          => DeclarationMode.ParentDeclaration
    }

  implicit val writes: Writes[DeclarationMode] =
    Writes(declarationMode => JsString(declarationMode.toString))
}
