package uk.gov.hmrc.cdsreimbursementclaim.models

import org.scalatest.matchers.should._
import org.scalatest.wordspec.AnyWordSpec
import ru.tinkoff.phobos.akka_http._

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
          checksum = "123123123",
          fileSize = 1000L,
          sourceLocation = "https://somewhere.on.aws",
          sourceFileName = "filename.pdf",
          properties = Some(properties)
        )
      )

      val xmlString  = Dec64Body.soapEncoder.encode(HeadlessEnvelope(bfim))
      val xml = XML.loadString(xmlString)
      val body = xml \ "Body" \ "BatchFileInterfaceMetadata"
      (body \ "sourceSystem").text shouldBe "MDTP"
      (body \ "sourceSystemType").text shouldBe "AWS"
      (body \ "interfaceName").text shouldBe "DEC64"
      (body \ "interfaceVersion").text shouldBe "1.0.0"
      (body \ "checksum").text shouldBe "123123123"
      (body \ "fileSize").text shouldBe "1000"
      (body \ "sourceLocation").text shouldBe "https://somewhere.on.aws"
      (body \ "sourceFileName").text shouldBe "filename.pdf"
      val props = body \ "properties" \ "property"
      (props \ "name").map(_.text) shouldBe Seq("DeclarationId", "Eori")
      (props \ "value").map(_.text) shouldBe Seq("20GB5J7MBW7E4FGVA2", "GB239355053000")
    }

  }
}
