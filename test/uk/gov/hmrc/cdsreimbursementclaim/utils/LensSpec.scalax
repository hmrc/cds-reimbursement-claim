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

package uk.gov.hmrc.cdsreimbursementclaim.utils
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class LensSpec extends AnyWordSpec with Matchers {

  case class TestCase1(foo: TestCase2, bar: Int, zoo: String, opt: Option[Int] = Some(0))
  case class TestCase2(faz: String, zaz: Int, opt2: Option[TestCase3] = None) {
    def withFaz(faz2: String) = this.copy(faz = faz2)
    def withZaz(zaz2: Int)    = this.copy(zaz = zaz2)
  }
  case class TestCase3(tas: Boolean)

  val lens  = Lens[TestCase1]
  val lens2 = lens.foo
  val lens3 = lens.foo.faz
  val lens4 = lens.foo.zaz
  val lens5 = lens.bar
  val lens6 = lens.zoo
  val lens7 = lens.opt
  val lens8 = lens.foo.opt2

  val sample = TestCase1(foo = TestCase2(faz = "Faz", zaz = 9), bar = 1, zoo = "Zoo")

  "Lens" should {
    "get value using lenses" in {
      lens2.get(sample).faz shouldBe "Faz"
      lens2.get(sample).zaz shouldBe 9
      lens3.get(sample)     shouldBe "Faz"
      lens4.get(sample)     shouldBe 9
      lens5.get(sample)     shouldBe 1
      lens6.get(sample)     shouldBe "Zoo"
      lens7.get(sample)     shouldBe Some(0)
      lens8.get(sample)     shouldBe None
    }

    "set value using lenses" in {
      lens2.set(sample, TestCase2(faz = "zaf", zaz = 8)).foo.faz   shouldBe "zaf"
      lens2.set(sample, TestCase2(faz = "zaf", zaz = 8))           shouldBe
        TestCase1(
          foo = TestCase2(faz = "zaf", zaz = 8),
          bar = 1,
          zoo = "Zoo"
        )
      lens3.set(sample, "zaf").foo.faz                             shouldBe "zaf"
      lens4.set(sample, 99).foo.zaz                                shouldBe 99
      lens5.set(sample, 2).bar                                     shouldBe 2
      lens6.set(sample, "Hello").zoo                               shouldBe "Hello"
      lens7.set(sample, None).opt                                  shouldBe None
      lens8.set(sample, Some(TestCase3(true))).foo.opt2.map(_.tas) shouldBe Some(true)
    }

    "update value using lenses" in {
      lens2.update(sample, _.withFaz("oooo")).foo.faz shouldBe "oooo"
      lens2.update(sample, _.withZaz(1000)).foo.zaz   shouldBe 1000
      lens2.update(sample, _.withZaz(1000)).foo.faz   shouldBe "Faz"
      lens3.update(sample, _.reverse).foo.faz         shouldBe "zaF"
      lens4.update(sample, _ * -1).foo.zaz            shouldBe -9
      lens5.update(sample, _ * -1).bar                shouldBe -1
      lens6.update(sample, _.reverse).zoo             shouldBe "ooZ"
    }
  }
}
