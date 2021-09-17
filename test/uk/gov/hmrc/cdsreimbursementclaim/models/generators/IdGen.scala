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

package uk.gov.hmrc.cdsreimbursementclaim.models.generators

import org.scalacheck.magnolia._
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{MovementReferenceNumber, PhoneNumber}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.{EntryNumber, Eori, MRN}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample

object IdGen {

  def alphaNumGen(n: Int): String =
    Gen.listOfN(n, Gen.alphaNumChar).map(_.mkString).sample.getOrElse(sys.error(s"Could not generate instance"))

  def alphaCharGen(n: Int): String =
    Gen.listOfN(n, Gen.alphaChar).map(_.mkString).sample.getOrElse(sys.error(s"Could not generate instance"))

  def genPhoneNumber: PhoneNumber =
    Gen
      .listOfN(10, Gen.numChar)
      .map(numbers => PhoneNumber(numbers.foldLeft("0")((s, ch) => s"$s$ch")))
      .sample
      .getOrElse(sys.error(s"Could not generate instance"))

  implicit val arbitraryPhoneNumber: Typeclass[PhoneNumber] = Arbitrary(genPhoneNumber)

  def genEori: Gen[Eori] =
    for {
      c <- Gen.listOfN(2, Gen.alphaUpperChar)
      n <- Gen.listOfN(12, Gen.numChar)
      s <- Gen.const((c ++ n).mkString(""))
    } yield Eori(s)

  implicit val arbitraryEori: Typeclass[Eori] = Arbitrary(genEori)

  def genMRN: Gen[MRN] = for {
    d1      <- Gen.listOfN(2, Gen.numChar)
    letter2 <- Gen.listOfN(2, Gen.alphaUpperChar)
    word    <- Gen.listOfN(13, Gen.numChar)
    d2      <- Gen.listOfN(1, Gen.numChar)
  } yield MRN((d1 ++ letter2 ++ word ++ d2).mkString)

  implicit val arbitraryMrn: Typeclass[MRN] = Arbitrary(genMRN)

  def sampleMrnAnswer(mrn: MRN = sample[MRN]): Option[MovementReferenceNumber] =
    Some(MovementReferenceNumber(Right(mrn)))

  def genEntryNumber: Gen[EntryNumber] = for {
    prefix <- Gen.listOfN(9, Gen.numChar)
    letter <- Gen.listOfN(1, Gen.alphaUpperChar)
    suffix <- Gen.listOfN(8, Gen.numChar)
  } yield EntryNumber((prefix ++ letter ++ suffix).mkString)

  implicit val entryNumberGen: Typeclass[EntryNumber] = Arbitrary(genEntryNumber)

  def sampleEntryNumberAnswer(entryNumber: EntryNumber = sample[EntryNumber]): Option[MovementReferenceNumber] =
    Some(MovementReferenceNumber(Left(entryNumber)))

  def genMovementReferenceNumber: Gen[MovementReferenceNumber] = Gen.oneOf(
    genMRN.map(mrn => MovementReferenceNumber(Right(mrn))),
    genEntryNumber.map(entry => MovementReferenceNumber(Left(entry)))
  )

  implicit val arbitraryMovementReferenceNumber: Typeclass[MovementReferenceNumber] =
    Arbitrary(genMovementReferenceNumber)

}