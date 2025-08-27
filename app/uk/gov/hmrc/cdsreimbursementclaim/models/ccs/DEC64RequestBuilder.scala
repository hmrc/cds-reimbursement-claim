/*
 * Copyright 2025 HM Revenue & Customs
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

object DEC64RequestBuilder {

  def apply(
    correlationID: String,
    batchID: String,
    batchCount: Int,
    batchSize: Int,
    checksum: String,
    sourceLocation: String,
    sourceFileName: String,
    sourceFileMimeType: String,
    fileSize: Long,
    properties: List[(String, Any)]
  ) =
    s"""<?xml version='1.0' encoding='UTF-8'?>
       |<ans1:Envelope xmlns:ans1="http://schemas.xmlsoap.org/soap/envelope/">
       |    <ans1:Body>
       |        <ans2:BatchFileInterfaceMetadata
       |            xmlns:ans2="http://www.hmrc.gsi.gov.uk/mdg/batchFileInterfaceMetadataSchema">
       |            <ans2:sourceSystem>TPI</ans2:sourceSystem>
       |            <ans2:sourceSystemType>AWS</ans2:sourceSystemType>
       |            <ans2:interfaceName>DEC64</ans2:interfaceName>
       |            <ans2:interfaceVersion>1.0.0</ans2:interfaceVersion>
       |            <ans2:correlationID>$correlationID</ans2:correlationID>
       |            <ans2:batchID>$batchID</ans2:batchID>
       |            <ans2:batchSize>$batchSize</ans2:batchSize>
       |            <ans2:batchCount>$batchCount</ans2:batchCount>
       |            <ans2:checksum>$checksum</ans2:checksum>
       |            <ans2:checksumAlgorithm>SHA-256</ans2:checksumAlgorithm>
       |            <ans2:fileSize>$fileSize</ans2:fileSize>
       |            <ans2:compressed>false</ans2:compressed>
       |            <ans2:properties>
       |${properties.map(property).mkString("\n")}
       |            </ans2:properties>
       |            <ans2:sourceLocation>$sourceLocation</ans2:sourceLocation>
       |            <ans2:sourceFileName>$sourceFileName</ans2:sourceFileName>
       |            <ans2:sourceFileMimeType>$sourceFileMimeType</ans2:sourceFileMimeType>
       |            <ans2:destinations>
       |                <ans2:destination>
       |                    <ans2:destinationSystem>CDFPay</ans2:destinationSystem>
       |                </ans2:destination>
       |            </ans2:destinations>
       |        </ans2:BatchFileInterfaceMetadata>
       |    </ans1:Body>
       |</ans1:Envelope>""".stripMargin

  def property(key: String, value: Any) =
    s"""              <ans2:property>
       |                 <ans2:name>$key</ans2:name>
       |                 <ans2:value>$value</ans2:value>
       |              </ans2:property>""".stripMargin
}
