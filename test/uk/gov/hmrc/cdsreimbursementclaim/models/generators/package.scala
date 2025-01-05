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

package uk.gov.hmrc.cdsreimbursementclaim.models

import org.apache.pekko.http.scaladsl.model.DateTime
import org.apache.pekko.util.ByteString

import org.scalacheck.{Arbitrary, Gen}
import org.bson.types.ObjectId

import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}
import java.util.UUID

package object generators {

  def genRandomString: Gen[String] =
    genStringWithMaxSizeOfN(15)

  def genStringWithMaxSizeOfN(max: Int): Gen[String] =
    Gen
      .choose(5, max)
      .flatMap(Gen.listOfN(_, Gen.alphaChar))
      .map(_.mkString(""))

  lazy val genLocalDateTime: Gen[LocalDateTime] =
    Gen
      .chooseNum(0L, 10000L)
      .map(millis => LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()))

  implicit lazy val arbitraryLocalDateTime: Arbitrary[LocalDateTime] =
    Arbitrary(genLocalDateTime)

  lazy val genLocalDate: Gen[LocalDate] =
    Gen.chooseNum(0L, 10000L).map(LocalDate.ofEpochDay)

  implicit lazy val arbitraryLocalDate: Arbitrary[LocalDate] =
    Arbitrary(genLocalDate)

  lazy val genBoolean: Gen[Boolean] = Gen.oneOf(true, false)

  implicit lazy val arbitraryBoolean: Arbitrary[Boolean] =
    Arbitrary(genBoolean)

  implicit lazy val arbitraryString: Arbitrary[String] = Arbitrary(
    Gen.nonEmptyListOf(Gen.alphaUpperChar).map(_.mkString(""))
  )

  implicit lazy val arbitraryLong: Arbitrary[Long] = Arbitrary(
    Gen.choose(-5e13.toLong, 5e13.toLong)
  )

  implicit lazy val arbitraryInstant: Arbitrary[Instant] =
    Arbitrary(
      Gen
        .chooseNum(0L, 10000L)
        .map(Instant.ofEpochMilli)
    )

  lazy val genUUID: Gen[UUID] = Gen.const(UUID.randomUUID())

  implicit lazy val arbitraryUUID: Arbitrary[UUID] =
    Arbitrary(genUUID)

  implicit lazy val ObjectId: Arbitrary[ObjectId] =
    Arbitrary(
      Gen
        .choose(0L, 10000L)
        .map(_ => new ObjectId())
    )

  lazy val genBigDecimal: Gen[BigDecimal] =
    Gen.choose(0, 100).map(BigDecimal(_))

  implicit lazy val bigDecimalGen: Arbitrary[BigDecimal] =
    Arbitrary(genBigDecimal)

  implicit lazy val jodaDateTime: Arbitrary[DateTime] =
    Arbitrary(
      Gen
        .chooseNum(0L, 10000L)
        .map(l => DateTime(l))
    )

  implicit lazy val byteStringArb: Arbitrary[ByteString] =
    Arbitrary(
      Gen
        .choose(0L, Long.MaxValue)
        .map(s => ByteString(s))
    )

}
