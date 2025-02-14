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

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen
import org.scalatest.compatible.Assertion
import org.scalatest.matchers.should.Matchers

trait ForSampledValueCheck {
  self: Matchers & ScalaCheckPropertyChecks =>

  final def forSampledValue[A](gen: Gen[A])(block: A => Assertion) = {
    val value: A =
      gen.sample.getOrElse(fail("Cannot sample test value"))

    block(value)
  }

  final def forSampledValue[A, B](gen1: Gen[A], gen2: Gen[B])(block: (A, B) => Assertion) = {
    val value1: A =
      gen1.sample.getOrElse(fail("Cannot sample test value"))
    val value2: B =
      gen2.sample.getOrElse(fail("Cannot sample test value"))

    block(value1, value2)
  }

  final def forSampledValue[A : Gen](block: A => Assertion) = {
    val value: A =
      implicitly[Gen[A]].sample.getOrElse(fail("Cannot sample test value"))

    block(value)
  }

}
