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

import ru.tinkoff.phobos.derivation.semiauto._
import ru.tinkoff.phobos.encoding.ElementEncoder
import ru.tinkoff.phobos.syntax.xmlns
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig._
import uk.gov.hmrc.cdsreimbursementclaim.models.ccs.Namespaces.mdg

final case class BatchFileInterfaceMetadata(
  @xmlns(mdg) sourceSystem: String = Dec64.SOURCE_SYSTEM,
  @xmlns(mdg) sourceSystemType: String = Dec64.SOURCE_SYSTEM_TYPE,
  @xmlns(mdg) interfaceName: String = Dec64.INTERFACE_NAME,
  @xmlns(mdg) interfaceVersion: String = Dec64.INTERFACE_VERSION,
  @xmlns(mdg) correlationID: String,
  @xmlns(mdg) batchID: String,
  @xmlns(mdg) batchSize: Long,
  @xmlns(mdg) batchCount: Long,
  @xmlns(mdg) checksum: String,
  @xmlns(mdg) checksumAlgorithm: String = Dec64.UPSCAN_CHECKSUM_ALGORITHM,
  @xmlns(mdg) fileSize: Long,
  @xmlns(mdg) compressed: Boolean = false,
  @xmlns(mdg) properties: PropertiesType,
  @xmlns(mdg) sourceLocation: String,
  @xmlns(mdg) sourceFileName: String,
  @xmlns(mdg) sourceFileMimeType: String,
  @xmlns(mdg) destinations: Destinations = Destinations(List(Destination(Dec64.CDFPay)))
)

object BatchFileInterfaceMetadata {
  implicit val batchFileInterfaceMetadataEncoder: ElementEncoder[BatchFileInterfaceMetadata] =
    deriveElementEncoder[BatchFileInterfaceMetadata]
}
