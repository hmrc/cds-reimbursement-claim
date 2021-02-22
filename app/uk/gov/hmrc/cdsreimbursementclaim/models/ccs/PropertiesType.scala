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

package uk.gov.hmrc.cdsreimbursementclaim.models.ccs

import ru.tinkoff.phobos.derivation.semiauto.deriveElementEncoder
import ru.tinkoff.phobos.encoding.ElementEncoder
import ru.tinkoff.phobos.syntax.xmlns
import uk.gov.hmrc.cdsreimbursementclaim.models.ccs.Namespaces.mdg
import uk.gov.hmrc.cdsreimbursementclaim.utils.TimeUtils

import java.time.LocalDateTime

final case class PropertiesType(@xmlns(mdg) property: Seq[PropertyType] = Nil)

object PropertiesType {

  def generateMandatoryList(
    caseReference: String,
    eori: String,
    declarationId: String,
    declarantType: String,
    documentType: String,
    fileUpscanDateTime: LocalDateTime
  ): PropertiesType =
    PropertiesType(
      List(
        PropertyType("CaseReference", caseReference),
        PropertyType("Eori", eori),
        PropertyType("DeclarationId", declarationId),
        PropertyType("DeclarationType", declarantType),
        PropertyType("ApplicationName", "NDRC"),
        PropertyType("DocumentType", documentType),
        PropertyType("DocumentReceivedDate", TimeUtils.cdsDateTimeFormat.format(fileUpscanDateTime))
      )
    )

  implicit val propertiesTypeEnc: ElementEncoder[PropertiesType] = deriveElementEncoder[PropertiesType]

}
