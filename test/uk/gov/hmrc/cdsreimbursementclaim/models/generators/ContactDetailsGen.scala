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

package uk.gov.hmrc.cdsreimbursementclaim.models.generators

import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.magnolia.{Typeclass, gen}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.PhoneNumber
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.ContactDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.AddressGen.{genCountry, genPostcode}

import java.util.Locale

object ContactDetailsGen {

  lazy val genEmail: Gen[Email] =
    for {
      name   <- genStringWithMaxSizeOfN(max = 15).map(_.toLowerCase(Locale.UK))
      at      = "@"
      domain <- genStringWithMaxSizeOfN(max = 10).map(_.toLowerCase(Locale.UK))
      dotCom  = ".com"
    } yield Email(Seq(name, at, domain, dotCom).mkString)

  implicit lazy val arbitraryEmail: Typeclass[Email] =
    Arbitrary(genEmail)

  lazy val genUkPhoneNumber: Gen[PhoneNumber] =
    Gen.listOfN(10, Gen.numChar).map(numbers => PhoneNumber(numbers.foldLeft("0")((s, ch) => s"$s$ch")))

  implicit lazy val arbitraryUkPhoneNumber: Typeclass[PhoneNumber] =
    Arbitrary(genUkPhoneNumber)

  lazy val genContactDetails: Gen[ContactDetails] =
    for {
      contactName  <- Gen.option(genStringWithMaxSizeOfN(7))
      addressLine1 <- Gen.option(Gen.posNum[Int].map(num => s"$num ${genStringWithMaxSizeOfN(7)}"))
      addressLine2 <- Gen.option(genStringWithMaxSizeOfN(10))
      addressLine3 <- Gen.option(genStringWithMaxSizeOfN(10))
      addressLine4 <- Gen.option(genStringWithMaxSizeOfN(10))
      postalCode   <- Gen.option(genPostcode)
      countryCode  <- Gen.option(genCountry.map(_.code))
      telephone    <- Gen.option(genUkPhoneNumber.map(_.value))
      emailAddress <- Gen.option(genEmail.map(_.value))
    } yield ContactDetails(
      contactName,
      addressLine1,
      addressLine2,
      addressLine3,
      addressLine4,
      postalCode.map(_.value),
      countryCode,
      telephone,
      emailAddress
    )

  implicit lazy val arbitraryContactDetails: Typeclass[ContactDetails] =
    gen[ContactDetails]
}
