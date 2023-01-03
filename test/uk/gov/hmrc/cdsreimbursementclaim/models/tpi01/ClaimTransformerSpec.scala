package uk.gov.hmrc.cdsreimbursementclaim.models.tpi01

import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ClaimTransformerSpec extends AnyWordSpec with Matchers with MockFactory {

  object Transformer extends ClaimTransformer[CaseDetails, SampleClass] {
    override def fromTpi01Response(caseDetails: CaseDetails): SampleClass = mock[SampleClass]
  }
  case class SampleClass(id: Int, name: String, CDFPayCaseNumber: String, declarationID: Option[String])
      extends ClaimItem

  "ClaimTransformer" should {
    "removeDuplicates" in {
      val list =
        Seq(SampleClass(1, "Bob", "", None), SampleClass(1, "Bob", "", None), SampleClass(2, "Charlie", "", None))

      val uniqueList: Seq[SampleClass] = Transformer.removeDuplicates(list, item => item.name)

      uniqueList.length shouldBe 2
      uniqueList          should contain only (SampleClass(1, "Bob", "", None), SampleClass(2, "Charlie", "", None))
    }
  }

}
