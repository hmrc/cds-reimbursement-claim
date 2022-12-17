package uk.gov.hmrc.cdsreimbursementclaim.models.tpi01

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ClaimsResponseSpec extends AnyWordSpec with Matchers {

  case class SampleClass(id: Int, name: String)

  "ClaimResponse" should {
    "removeDuplicates" in {
      val list = Seq(SampleClass(1, "Bob"), SampleClass(1, "Bob"), SampleClass(2, "Charlie"))

      val uniqueList: Seq[SampleClass] = ClaimsResponse.removeDuplicates[SampleClass](list, item => item.name)

      uniqueList.length shouldBe 2
      uniqueList should contain only (SampleClass(2, "Charlie"), SampleClass(1, "Bob"))
    }
  }


}
