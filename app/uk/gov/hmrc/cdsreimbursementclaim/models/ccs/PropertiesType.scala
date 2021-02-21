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
import ru.tinkoff.phobos.derivation.semiauto._
import uk.gov.hmrc.cdsreimbursementclaim.models.ccs.Namespaces.mdg

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}

final case class PropertiesType(@xmlns(mdg) property: Seq[PropertyType] = Nil)

object PropertiesType {
  def generateMandatoryList(
    caseReference: String,
    eori: String,
    declarationId: String,
    documentType: String,
    localDateTime: LocalDateTime = LocalDateTime.now()
  ): PropertiesType = {
    val receivedDateFormatter = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
      .withZone(ZoneId.systemDefault()) ///TODO: fix use the Timeeutils
    PropertiesType(
      List(
        PropertyType("CaseReference", caseReference),
        PropertyType("Eori", eori),
        PropertyType("DeclarationId", declarationId),
        PropertyType("DeclarationType", documentType),
        PropertyType("ApplicationName", "NDRC"), //Possible values are NDRC & Securities //TODO
        PropertyType("DocumentType", "MRN"), //can be either EntryNumber or MRN //TODO
        PropertyType("DocumentReceivedDate", receivedDateFormatter.format(localDateTime))
      )
    )
  }
  implicit val propertiesTypeEnc: ElementEncoder[PropertiesType] = deriveElementEncoder[PropertiesType]

}
