/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.cdsreimbursementclaim.models.tpi01

import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.immutable

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

      val uniqueList: immutable.Seq[SampleClass] = Transformer.removeDuplicates(list, item => item.name)

      uniqueList.length shouldBe 2
      uniqueList          should contain only (SampleClass(1, "Bob", "", None), SampleClass(2, "Charlie", "", None))
    }
  }

}
