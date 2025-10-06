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
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}

class MapFormatSpec extends AnyWordSpec with Matchers {

  "MapFormat" should {
    "serialize a map with a key that serializes to string" in {
      val map  = Map("a" -> 1, "b" -> 2)
      val json = Json.toJson(map)(MapFormat[String, Int])

      json shouldBe
        Json.obj(
          "a" -> 1,
          "b" -> 2
        )
    }

    "serialize a map with a key that doesn't serialize to a string" in {
      val map  = Map(1 -> "a", 2 -> "b")
      val json = Json.toJson(map)(MapFormat[Int, String])

      json shouldBe
        Json.obj(
          "entry__0" -> Json.obj("k" -> 1, "v" -> "a"),
          "entry__1" -> Json.obj("k" -> 2, "v" -> "b")
        )
    }

    "deserialize a Json object with keys that deserialize to string" in {
      val json   = Json.obj(
        "a" -> 1,
        "b" -> 2
      )
      val result = Json.fromJson[Map[String, Int]](json)(MapFormat[String, Int])

      result shouldBe JsSuccess(Map("a" -> 1, "b" -> 2))
    }

    "deserialize a Json object with entry__ prefixed keys correctly" in {
      val json   = Json.obj(
        "entry__0" -> Json.obj("k" -> 1, "v" -> "a"),
        "entry__1" -> Json.obj("k" -> 2, "v" -> "b")
      )
      val result = Json.fromJson[Map[Int, String]](json)(MapFormat[Int, String])

      result shouldBe JsSuccess(Map(1 -> "a", 2 -> "b"))
    }

    "fail to deserialize a Json object with invalid structure" in {
      val json   = Json.obj(
        "entry__0" -> Json.obj("k" -> 1)
      )
      val result = Json.fromJson[Map[Int, String]](json)(MapFormat[Int, String])

      result shouldBe a[JsError]
    }

    "fail to deserialize a non-Json object" in {
      val json   = Json.arr(1, 2)
      val result = Json.fromJson[Map[String, Int]](json)(MapFormat[String, Int])

      result shouldBe a[JsError]
    }
  }
}
