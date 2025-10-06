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

import uk.gov.hmrc.http.HttpResponse
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{Json, Reads}
import HttpResponseOps.HttpResponseOps

class HttpResponseOpsSpec extends AnyWordSpec with Matchers {

  case class Foo(value1: String, value2: Int)

  implicit val reads: Reads[Foo] = Json.reads[Foo]

  "HttpResponseOps" should {

    "successfully parse valid JSON" in {
      val json     = Json.obj("value1" -> "a", "value2" -> 1)
      val response = HttpResponse(200, json.toString())

      val result = response.parseJSON[Foo]()

      result shouldBe Right(Foo("a", 1))
    }

    "return left if JSON path does not exist" in {
      val json     = Json.obj("value1" -> "a", "value1" -> 1)
      val response = HttpResponse(200, json.toString())

      val result = response.parseJSON[Foo](Some("path"))

      result shouldBe Left("no JSON found in body of http response")
    }

    "return left if JSON is missing expected data" in {
      val json     = Json.obj("value1" -> "a")
      val response = HttpResponse(200, json.toString())

      val result = response.parseJSON[Foo]()

      result shouldBe Left("could not parse http response JSON: /value2: [error.path.missing]")
    }

    "return left if JSON is invalid" in {
      val json     = "{foo}"
      val response = HttpResponse(200, json)

      val result = response.parseJSON[Foo]()

      result.isLeft shouldBe true
    }
  }
}
