/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.libs.json._

class EnumerationFormatSpec extends AnyWordSpec with Matchers {

  import EnumerationFormatSpec._

  "EnumerationFormatSpec" should {
    "serialize an enum" in {
      Foo.format.writes(Foo.A)   shouldBe JsString("A")
      Foo.format.writes(Foo.B)   shouldBe JsString("B")
      Foo.format.writes(Foo.ABC) shouldBe JsString("ABC")

      an[Exception] shouldBe thrownBy(Foo.format.writes(Foo.C))
    }

    "de-serialize an enum" in {
      Foo.format.reads(JsString("A"))   shouldBe JsSuccess(Foo.A)
      Foo.format.reads(JsString("B"))   shouldBe JsSuccess(Foo.B)
      Foo.format.reads(JsString("ABC")) shouldBe JsSuccess(Foo.ABC)

      Foo.format.reads(JsString("a"))   shouldBe JsSuccess(Foo.A)
      Foo.format.reads(JsString("b"))   shouldBe JsSuccess(Foo.B)
      Foo.format.reads(JsString("abc")) shouldBe JsSuccess(Foo.ABC)
      Foo.format.reads(JsString("aBc")) shouldBe JsSuccess(Foo.ABC)
      Foo.format.reads(JsString("aBC")) shouldBe JsSuccess(Foo.ABC)
      Foo.format.reads(JsString("Abc")) shouldBe JsSuccess(Foo.ABC)
      Foo.format.reads(JsString("ABc")) shouldBe JsSuccess(Foo.ABC)

      Foo.format.reads(JsNull)          shouldBe a[JsError]
      Foo.format.reads(Json.obj("A" -> JsBoolean(true))) shouldBe a[JsError]
      Foo.format.reads(Json.obj("value" -> JsString("A"))) shouldBe a[JsError]
      Foo.format.reads(JsNumber(1))     shouldBe a[JsError]
      Foo.format.reads(JsBoolean(true)) shouldBe a[JsError]
    }

    "bind an enum from path parameter" in {
      Foo.pathBinder.bind("foo", "a")          shouldBe Right(Foo.A)
      Foo.pathBinder.bind("foo", "A")          shouldBe Right(Foo.A)
      Foo.pathBinder.bind("foo", "B")          shouldBe Right(Foo.B)
      Foo.pathBinder.bind("foo", "b")          shouldBe Right(Foo.B)
      Foo.pathBinder.bind("foo", "abc")        shouldBe Right(Foo.ABC)
      Foo.pathBinder.bind("foo", "ABC")        shouldBe Right(Foo.ABC)
      Foo.pathBinder.bind("foo", "c").isLeft   shouldBe true
      Foo.pathBinder.bind("foo", "C").isLeft   shouldBe true
      Foo.pathBinder.bind("foo", "d").isLeft   shouldBe true
      Foo.pathBinder.bind("foo", "D").isLeft   shouldBe true
      Foo.pathBinder.bind("foo", "bar").isLeft shouldBe true
    }

    "unbind an enum as a path parameter" in {
      Foo.pathBinder.unbind("foo", Foo.A)   shouldBe "A"
      Foo.pathBinder.unbind("foo", Foo.ABC) shouldBe "ABC"
      Foo.pathBinder.unbind("foo", Foo.B)   shouldBe "B"

      an[Exception] shouldBe thrownBy(Foo.pathBinder.unbind("foo", Foo.C))
    }

    "bind an enum from query parameter" in {
      val query: Map[String, Seq[String]] =
        Seq("a", "b", "c", "abc", "d", "bar").flatMap { i =>
          Seq(i.toLowerCase -> Seq(i.toLowerCase()), i.toUpperCase() -> Seq(i.toUpperCase()))
        }.toMap

      Foo.queryBinder.bind("a", query)   shouldBe Some(Right(Foo.A))
      Foo.queryBinder.bind("A", query)   shouldBe Some(Right(Foo.A))
      Foo.queryBinder.bind("b", query)   shouldBe Some(Right(Foo.B))
      Foo.queryBinder.bind("B", query)   shouldBe Some(Right(Foo.B))
      Foo.queryBinder.bind("ABC", query) shouldBe Some(Right(Foo.ABC))
      Foo.queryBinder.bind("abc", query) shouldBe Some(Right(Foo.ABC))
      Foo.queryBinder.bind("bar", query)   should matchPattern { case Some(Left(_)) => }
      Foo.queryBinder.bind("c", query)     should matchPattern { case Some(Left(_)) => }
      Foo.queryBinder.bind("C", query)     should matchPattern { case Some(Left(_)) => }
      Foo.queryBinder.bind("BAR", query)   should matchPattern { case Some(Left(_)) => }
      Foo.queryBinder.bind("zoo", query)   should matchPattern { case None => }
    }

    "unbind an enum as a query parameter" in {
      Foo.queryBinder.unbind("foo", Foo.A)   shouldBe "foo=A"
      Foo.queryBinder.unbind("foo", Foo.B)   shouldBe "foo=B"
      Foo.queryBinder.unbind("foo", Foo.ABC) shouldBe "foo=ABC"

      an[Exception] shouldBe thrownBy(Foo.queryBinder.unbind("foo", Foo.C))
    }
  }

}

object EnumerationFormatSpec {

  sealed trait Foo
  object Foo extends EnumerationFormat[Foo] {
    case object A extends Foo
    case object B extends Foo
    case object C extends Foo // not included in the value list
    case object ABC extends Foo

    override val values: Set[Foo] = Set(A, B, ABC)
  }

}
