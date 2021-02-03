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

package uk.gov.hmrc.cdsreimbursementclaim.models

import java.util.UUID

import ru.tinkoff.phobos.Namespace
import ru.tinkoff.phobos.akka_http.{HeadlessEnvelope}
import ru.tinkoff.phobos.derivation.semiauto.deriveElementEncoder
import ru.tinkoff.phobos.encoding.{ElementEncoder, XmlEncoder}
import ru.tinkoff.phobos.syntax.xmlns
import uk.gov.hmrc.cdsreimbursementclaim.utils.EisTime


case class BifNS()
object BifNS {
  implicit val batchFileInterfaceMetadataNs: Namespace[BifNS.type] = Namespace.mkInstance("http://www.hmrc.gsi.gov.uk/mdg/batchFileInterfaceMetadataSchema")
  implicit val batchFileInterfaceMetadataNss: Namespace[BifNS] = Namespace.mkInstance("http://www.hmrc.gsi.gov.uk/mdg/batchFileInterfaceMetadataSchema")
}

sealed trait CompressionAlgorithmSimpleType
case object GZIP extends CompressionAlgorithmSimpleType { override def toString = "GZIP" }
case object ZIP extends CompressionAlgorithmSimpleType { override def toString = "ZIP" }

sealed trait ChecksumAlgorithmType
case object CHIEF extends ChecksumAlgorithmType { override def toString = "CHIEF" }
case object MD2 extends ChecksumAlgorithmType { override def toString = "MD2" }
case object MD5 extends ChecksumAlgorithmType { override def toString = "MD5" }
case object SHA1 extends ChecksumAlgorithmType { override def toString = "SHA-1" }
case object SHA256 extends ChecksumAlgorithmType { override def toString = "SHA-256" }
case object SHA384 extends ChecksumAlgorithmType { override def toString = "SHA-384" }
case object SHA512 extends ChecksumAlgorithmType { override def toString = "SHA-512" }

final case class DestinationType(
                                  @xmlns(BifNS) destinationSystem: Option[String] = None,
                                  @xmlns(BifNS) destinationLocation: Option[String] = None,
                                  @xmlns(BifNS) destinationFileName: Option[String] = None,
                                  @xmlns(BifNS) destinationFileEncoding: Option[String] = None,
                                  @xmlns(BifNS) destinationFileMimeType: Option[String] = None
                                )

final case class DestinationsType(@xmlns(BifNS) destination: Seq[DestinationType] = Nil)

final case class ExecutionType(@xmlns(BifNS) performed: Option[EisTime] = None, @xmlns(BifNS) result: Result, @xmlns(BifNS) tool: String, @xmlns(BifNS) report: String)

final case class VirusScanType(@xmlns(BifNS) execution: Seq[ExecutionType] = Nil)

final case class PropertyType(@xmlns(BifNS) name: String, @xmlns(BifNS) value: String)

final case class PropertiesType(@xmlns(BifNS) property: Seq[PropertyType] = Nil)

sealed trait Result
case object Passed extends Result { override def toString = "Passed" }
case object Failed extends Result { override def toString = "Failed" }

final case class MemberType(
                             @xmlns(BifNS) fileName: String,
  @xmlns(BifNS) fileSize: Option[Long] = None,
  @xmlns(BifNS) checksumAlgorithm: ChecksumAlgorithmType,
  @xmlns(BifNS) checksum: String
)

final case class Manifest(member: Seq[MemberType] = Nil)



final case class BatchFileInterfaceMetadata(
                                             @xmlns(BifNS) sourceSystem: String = "MDTP",
                                             @xmlns(BifNS) sourceSystemType: Option[String] = Some("AWS"),
                                             @xmlns(BifNS) sourceSystemOS: Option[String] = None,
                                             @xmlns(BifNS) interfaceName: String = "DEC64",
                                             @xmlns(BifNS) interfaceVersion: String = "1.0.0",
                                             @xmlns(BifNS) correlationID: String = UUID.randomUUID().toString.replaceAll("-", "").take(31),
                                             @xmlns(BifNS) conversationID: Option[String] = None,
                                             @xmlns(BifNS) transactionID: Option[String] = None,
                                             @xmlns(BifNS) messageID: Option[String] = None,
                                             @xmlns(BifNS) sequenceNumber: Option[BigInt] = None,
                                             @xmlns(BifNS) batchID: Option[String] = None,
                                             @xmlns(BifNS) batchSize: Option[BigInt] = None,
                                             @xmlns(BifNS) batchCount: Option[BigInt] = None,
                                             @xmlns(BifNS) extractStartDateTime: Option[EisTime] = None,
                                             @xmlns(BifNS) extractEndDateTime: Option[EisTime] = None,
                                             @xmlns(BifNS) extractDatabaseDateTime: Option[EisTime] = None,
                                             @xmlns(BifNS) checksum: String,
                                             @xmlns(BifNS) checksumAlgorithm: ChecksumAlgorithmType = SHA256,
                                             @xmlns(BifNS) signature: Option[String] = None,
                                             @xmlns(BifNS) fileSize: Long,
                                             @xmlns(BifNS) compressed: Boolean = false,
                                             @xmlns(BifNS) compressionAlgorithm: Option[CompressionAlgorithmSimpleType] = None,
                                             @xmlns(BifNS) compressedChecksum: Option[String] = None,
                                             @xmlns(BifNS) compressedChecksumAlgorithm: Option[ChecksumAlgorithmType] = None,
                                             @xmlns(BifNS) compressedSignature: Option[String] = None,
                                             @xmlns(BifNS) manifest: Option[Manifest] = None,
                                             @xmlns(BifNS) encrypted: Option[Boolean] = None,
                                             @xmlns(BifNS) encryptedChecksum: Option[ChecksumAlgorithmType] = None,
                                             @xmlns(BifNS) encryptedSignature: Option[String] = None,
                                             @xmlns(BifNS) properties: Option[PropertiesType] = None,
                                             @xmlns(BifNS) sourceLocation: String,
                                             @xmlns(BifNS) sourceFileName: String,
                                             @xmlns(BifNS) sourceFileEncoding: Option[String] = None,
                                             @xmlns(BifNS) sourceFileMimeType: Option[String] = None,
                                             @xmlns(BifNS) virusScan: Option[VirusScanType] = None,
                                             @xmlns(BifNS) destinations: Option[DestinationsType] = None
)

case class Dec64Body(@xmlns(BifNS) BatchFileInterfaceMetadata:BatchFileInterfaceMetadata)


object Dec64Body{

  implicit val eisTimeEnc:ElementEncoder[EisTime] = deriveElementEncoder
  implicit val compressionAlgorithmSimpleTypeEnc:ElementEncoder[CompressionAlgorithmSimpleType] = ElementEncoder.stringEncoder.contramap(_.toString)
  implicit val checksumAlgorithmTypeEnc:ElementEncoder[ChecksumAlgorithmType] = ElementEncoder.stringEncoder.contramap(_.toString)
  implicit val resultEnc:ElementEncoder[Result] = ElementEncoder.stringEncoder.contramap(_.toString)
  implicit val memberTypeEnc:ElementEncoder[MemberType] = deriveElementEncoder
  implicit val manifestEnc:ElementEncoder[Manifest] = deriveElementEncoder

  implicit val propertiesTypeEnc:ElementEncoder[PropertiesType] = deriveElementEncoder
  implicit val propertyTypeEnc:ElementEncoder[PropertyType] = deriveElementEncoder
  implicit val virusScanTypeEnc:ElementEncoder[VirusScanType] = deriveElementEncoder
  implicit val executionTypeEnc:ElementEncoder[ExecutionType] = deriveElementEncoder
  implicit val destinationsTypeEnc:ElementEncoder[DestinationsType] = deriveElementEncoder
  implicit val destinationTypeEnc:ElementEncoder[DestinationType] = deriveElementEncoder
  implicit val batchFileInterfaceMetadataEnc:ElementEncoder[BatchFileInterfaceMetadata] = deriveElementEncoder
  implicit val dec64BodyEnc:ElementEncoder[Dec64Body] = deriveElementEncoder


  implicit val soapEncoder = implicitly[XmlEncoder[HeadlessEnvelope[Dec64Body]]]

}
