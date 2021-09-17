/*
 * Copyright 2021 HM Revenue & Customs
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

import akka.util.ByteString
import org.joda.time.DateTime
import org.scalacheck.magnolia.Typeclass
import org.scalacheck.{Arbitrary, Gen}
import reactivemongo.bson.BSONObjectID

import java.net.URL
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}
import java.util.UUID

package object generators {

  def genStringWithMaxSizeOfN(max: Int): Gen[String] =
    Gen
      .choose(1, max)
      .flatMap(Gen.listOfN(_, Gen.alphaChar))
      .map(_.mkString(""))

  def genLocalDateTime: Gen[LocalDateTime] =
    Gen
      .chooseNum(0L, 10000L)
      .map(millis => LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()))

  implicit val arbitraryBoolean: Typeclass[Boolean] = Arbitrary(
    Gen.oneOf(true, false)
  )

  implicit val arbitraryString: Typeclass[String] = Arbitrary(
    Gen.nonEmptyListOf(Gen.alphaUpperChar).map(_.mkString(""))
  )

  implicit val arbitraryLong: Arbitrary[Long] = Arbitrary(
    Gen.choose(-5e13.toLong, 5e13.toLong)
  )

  implicit val arbitraryLocalDate: Arbitrary[LocalDate] = Arbitrary(
    Gen.chooseNum(0L, 10000L).map(LocalDate.ofEpochDay)
  )

  implicit val arbitraryLocalDateTime: Arbitrary[LocalDateTime] =
    Arbitrary(genLocalDateTime)

  implicit val arbitraryInstant: Arbitrary[Instant] =
    Arbitrary(
      Gen
        .chooseNum(0L, 10000L)
        .map(Instant.ofEpochMilli)
    )

  implicit val arbitraryUuid: Arbitrary[UUID] = Arbitrary(UUID.randomUUID())

  implicit val arbitraryUrl: Arbitrary[URL] = Arbitrary(
    for {
      protocol <- Gen.oneOf("http", "https")
      hostname <- genStringWithMaxSizeOfN(7)
      domain   <- Gen.oneOf("com", "co.uk", "lv")
    } yield new URL(s"$protocol://$hostname.$domain")
  )

  implicit val bsonObjectId: Arbitrary[BSONObjectID] =
    Arbitrary(
      Gen
        .choose(0L, 10000L)
        .map(_ => BSONObjectID.generate())
    )

  implicit val bigDecimalGen: Arbitrary[BigDecimal] = Arbitrary(Gen.choose(0, 100).map(BigDecimal(_)))

  implicit val jodaDateTime: Arbitrary[DateTime] =
    Arbitrary(
      Gen
        .chooseNum(0L, 10000L)
        .map(l => new DateTime(l))
    )

  implicit val byteStringArb: Arbitrary[ByteString] =
    Arbitrary(
      Gen
        .choose(0L, Long.MaxValue)
        .map(s => ByteString(s))
    )

}
