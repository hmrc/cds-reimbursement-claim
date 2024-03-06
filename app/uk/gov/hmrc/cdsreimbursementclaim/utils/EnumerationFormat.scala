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

import cats.Eq
import cats.syntax.eq._
import play.api.libs.json.Format
import play.api.mvc.PathBindable
import play.api.mvc.QueryStringBindable

/** Provides capabilities to the set of case objects of a sealed trait T:
  * - JOSN serialization
  * - cats Eq (equality)
  * - path binding
  * - query string binding
  */
@SuppressWarnings(Array("org.wartremover.warts.Throw"))
trait EnumerationFormat[T] {

  val values: Set[T]

  private final lazy val valueMap: Map[String, T] =
    values.map(v => (v.toString, v)).toMap

  final def parse(key: String): Option[T] =
    valueMap
      .get(key)
      .orElse(values.find(_.toString.toLowerCase() === key.toLowerCase()))

  final def keyOf(value: T): String = {
    val key = value.toString
    if (valueMap.contains(key)) key
    else
      throw new IllegalArgumentException(s"The [$key] is NOT a value of the expected enum class.")
  }

  final def tryParse(key: String): T =
    valueMap
      .get(key)
      .orElse(values.find(_.toString.toLowerCase() === key.toLowerCase()))
      .getOrElse(
        throw new IllegalArgumentException(s"The [$key] is NOT a value of the expected enum class.")
      )

  final def hasKey(key: String): Boolean =
    valueMap.contains(key) ||
      values.exists(keyOf(_).toLowerCase() === key.toLowerCase())

  implicit final val equality: Eq[T] = Eq.fromUniversalEquals[T]

  implicit final val format: Format[T] =
    SimpleStringFormat(tryParse, keyOf)

  implicit final val enumeration: EnumerationFormat[T] = this

  implicit val pathBinder: PathBindable[T] = new PathBindable[T] {
    def bind(key: String, value: String): Either[String, T] =
      parse(value).toRight(s"Path segment /$value/ cannot be parsed as a $key of an enum type")

    def unbind(key: String, value: T): String =
      keyOf(value)

  }

  implicit val queryBinder: QueryStringBindable[T] = new QueryStringBindable[T] {

    override def bind(
      key: String,
      params: Map[String, Seq[String]]
    ): Option[Either[String, T]] =
      QueryStringBindable.bindableString
        .bind(key, params)
        .map(
          _.flatMap(value => parse(value).toRight(s"Query parameter $key=[$value] cannot be parsed as an enum type"))
        )

    override def unbind(key: String, value: T): String =
      QueryStringBindable.bindableString.unbind(key, keyOf(value))
  }
}
