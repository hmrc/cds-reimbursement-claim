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

package uk.gov.hmrc.cdsreimbursementclaim.models.dec64

import java.time.LocalDateTime

import ru.tinkoff.phobos.Namespace
import ru.tinkoff.phobos.derivation.semiauto._
import ru.tinkoff.phobos.encoding.{ElementEncoder, XmlEncoder}
import ru.tinkoff.phobos.syntax.xmlns
import uk.gov.hmrc.cdsreimbursementclaim.models.HeadlessEnvelope
import uk.gov.hmrc.cdsreimbursementclaim.utils.{EisTime, TimeUtils}

//This class implements the DEC64 API, and the xml serialization. There are no response classes as the ok response is 204 No Content
final case class BifNS()
object BifNS {
  implicit val batchFileInterfaceMetadataNs: Namespace[BifNS.type] =
    Namespace.mkInstance("http://www.hmrc.gsi.gov.uk/mdg/batchFileInterfaceMetadataSchema")
  implicit val batchFileInterfaceMetadataNss: Namespace[BifNS]     =
    Namespace.mkInstance("http://www.hmrc.gsi.gov.uk/mdg/batchFileInterfaceMetadataSchema")
}

sealed trait ChecksumAlgorithmType
case object SHA256 extends ChecksumAlgorithmType { override def toString = "SHA-256" }

final case class DestinationType(
  @xmlns(BifNS) destinationSystem: String = "CDS"
)

final case class DestinationsType(@xmlns(BifNS) destination: Seq[DestinationType] = Nil)

final case class PropertyType(@xmlns(BifNS) name: String, @xmlns(BifNS) value: String)

final case class PropertiesType(@xmlns(BifNS) property: Seq[PropertyType] = Nil)

object PropertiesType {
  def generateMandatoryList(
    caseReference: String,
    eori: String,
    declarationId: String,
    documentType: String,
    localDateTime: LocalDateTime = LocalDateTime.now()
  ): PropertiesType =
    PropertiesType(
      List(
        PropertyType("CaseReference", caseReference),
        PropertyType("Eori", eori),
        PropertyType("DeclarationId", declarationId),
        PropertyType("DeclarationType", documentType),
        PropertyType("ApplicationName", "NDRC"), //Possible values are NDRC & Securities
        PropertyType("DocumentType", "MRN"), //can be either EntryNumber or MRN
        PropertyType("DocumentReceivedDate", TimeUtils.eisDateTimeFormat.format(localDateTime))
      )
    )
}

final case class BatchFileInterfaceMetadata(
  @xmlns(BifNS) sourceSystem: String = "MDTP",
  @xmlns(BifNS) sourceSystemType: String = "AWS",
  @xmlns(BifNS) sourceSystemOS: Option[String] = None,
  @xmlns(BifNS) interfaceName: String = "DEC64",
  @xmlns(BifNS) interfaceVersion: String = "1.0.0",
  @xmlns(BifNS) correlationID: String,
  @xmlns(BifNS) sequenceNumber: Option[BigInt] = None,
  @xmlns(BifNS) batchID: String,
  @xmlns(BifNS) batchSize: BigInt,
  @xmlns(BifNS) batchCount: BigInt,
  @xmlns(BifNS) extractEndDateTime: EisTime,
  @xmlns(BifNS) checksum: String,
  @xmlns(BifNS) checksumAlgorithm: ChecksumAlgorithmType = SHA256,
  @xmlns(BifNS) fileSize: Long,
  @xmlns(BifNS) compressed: Boolean = false,
  @xmlns(BifNS) encrypted: Boolean = false,
  @xmlns(BifNS) properties: PropertiesType,
  @xmlns(BifNS) sourceLocation: String,
  @xmlns(BifNS) sourceFileName: String,
  @xmlns(BifNS) sourceFileMimeType: String,
  @xmlns(BifNS) destinations: DestinationsType
)

final case class Dec64Body(@xmlns(BifNS) BatchFileInterfaceMetadata: BatchFileInterfaceMetadata)

object Dec64Body {

  implicit val eisTimeEnc: ElementEncoder[EisTime]                             = deriveElementEncoder
  implicit val checksumAlgorithmTypeEnc: ElementEncoder[ChecksumAlgorithmType] =
    ElementEncoder.stringEncoder.contramap(_.toString)

  implicit val propertiesTypeEnc: ElementEncoder[PropertiesType]                         = deriveElementEncoder
  implicit val propertyTypeEnc: ElementEncoder[PropertyType]                             = deriveElementEncoder
  implicit val destinationsTypeEnc: ElementEncoder[DestinationsType]                     = deriveElementEncoder
  implicit val destinationTypeEnc: ElementEncoder[DestinationType]                       = deriveElementEncoder
  implicit val batchFileInterfaceMetadataEnc: ElementEncoder[BatchFileInterfaceMetadata] = deriveElementEncoder
  implicit val dec64BodyEnc: ElementEncoder[Dec64Body]                                   = deriveElementEncoder

  implicit val soapEncoder: XmlEncoder[HeadlessEnvelope[Dec64Body]] = HeadlessEnvelope.deriveEnvelopeEncoder[Dec64Body]

}
