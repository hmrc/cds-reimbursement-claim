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

import org.scalatest.matchers.should._
import org.scalatest.wordspec.AnyWordSpec
import scala.xml.XML

class BatchFileInterfaceMetadataSpec extends AnyWordSpec with Matchers {

  "the encoders" should {

    "serialize case classes to xml" in {
      val properties = PropertiesType(
        Seq(
          PropertyType("DeclarationId", "20GB5J7MBW7E4FGVA2"),
          PropertyType("Eori", "GB239355053000")
        )
      )
      val bfim       = Dec64Body(
        BatchFileInterfaceMetadata(
          correlationID = "123456789",
          checksum = "123123123",
          fileSize = Some(1000L),
          sourceLocation = "https://somewhere.on.aws",
          sourceFileName = "filename.pdf",
          properties = Some(properties)
        )
      )

      val xmlString = Dec64Body.soapEncoder.encode(HeadlessEnvelope(bfim))
      val xml       = XML.loadString(xmlString)
      val body      = xml \ "Body" \ "BatchFileInterfaceMetadata"
      (body \ "sourceSystem").text     shouldBe "TPI"
      (body \ "sourceSystemType").text shouldBe "AWS"
      (body \ "interfaceName").text    shouldBe "DEC64"
      (body \ "interfaceVersion").text shouldBe "1.0.0"
      (body \ "checksum").text         shouldBe "123123123"
      (body \ "fileSize").text         shouldBe "1000"
      (body \ "sourceLocation").text   shouldBe "https://somewhere.on.aws"
      (body \ "sourceFileName").text   shouldBe "filename.pdf"
      val props = body \ "properties" \ "property"
      (props \ "name").map(_.text)  shouldBe Seq("DeclarationId", "Eori")
      (props \ "value").map(_.text) shouldBe Seq("20GB5J7MBW7E4FGVA2", "GB239355053000")
    }

  }
}
