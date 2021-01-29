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
import org.scalacheck.{Arbitrary, Gen}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.cdsreimbursementclaim.models.upscan.UpscanCallBack.UpscanSuccess
import uk.gov.hmrc.cdsreimbursementclaim.models.upscan.{UploadReference, UpscanUpload}

import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}
import scala.reflect.{ClassTag, classTag}
import org.scalacheck.ScalacheckShapeless._

object Generators extends GenUtils with UpscanGen with DeclarationGen {

  def sample[A : ClassTag](implicit gen: Gen[A]): A =
    gen.sample.getOrElse(sys.error(s"Could not generate instance of ${classTag[A].runtimeClass.getSimpleName}"))

  def sampleOptional[A : ClassTag](implicit gen: Gen[A]): Option[A] =
    Gen
      .option(gen)
      .sample
      .getOrElse(sys.error(s"Could not generate instance of ${classTag[A].runtimeClass.getSimpleName}"))

  implicit def arb[A](implicit g: Gen[A]): Arbitrary[A] = Arbitrary(g)

}

// generator helpers
sealed trait GenUtils {

  def gen[A](implicit arb: Arbitrary[A]): Gen[A] = arb.arbitrary

  // define our own Arbitrary instance for String to generate more legible strings
  implicit val stringArb: Arbitrary[String] = Arbitrary(
    for {
      n <- Gen.choose(1, 30)
      s <- Gen.listOfN(n, Gen.alphaChar).map(_.mkString(""))
    } yield s
  )

  implicit val longArb: Arbitrary[Long] = Arbitrary(Gen.choose(0L, 100L))

  implicit val bigDecimalGen: Arbitrary[BigDecimal] = Arbitrary(Gen.choose(0, 100).map(BigDecimal(_)))

  implicit val localDateTimeArb: Arbitrary[LocalDateTime] =
    Arbitrary(
      Gen
        .chooseNum(0L, 10000L)
        .map(l => LocalDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneId.systemDefault()))
    )

  implicit val jodaDateTime: Arbitrary[DateTime] =
    Arbitrary(
      Gen
        .chooseNum(0L, 10000L)
        .map(l => new DateTime(l))
    )

  implicit val localDateArb: Arbitrary[LocalDate] = Arbitrary(
    Gen.chooseNum(0, 10000L).map(LocalDate.ofEpochDay)
  )

  implicit val byteStringArb: Arbitrary[ByteString] =
    Arbitrary(
      Gen
        .choose(0L, Long.MaxValue)
        .map(s => ByteString(s))
    )

  implicit val bsonObjectId: Arbitrary[BSONObjectID] =
    Arbitrary(
      Gen
        .choose(0L, 10000L)
        .map(_ => BSONObjectID.generate())
    )

  implicit val mrn = Arbitrary(for {
    d1      <- Gen.listOfN(2, Gen.numChar)
    letter2 <- Gen.listOfN(2, Gen.alphaUpperChar)
    word    <- Gen.listOfN(13, Gen.numChar)
    d2      <- Gen.listOfN(1, Gen.numChar)
  } yield MRN(s"${d1.mkString("")}${letter2.mkString("")}${word.mkString("")}${d2.mkString("")}"))

}

trait UpscanGen { this: GenUtils =>
  implicit val upscanUploadGen: Gen[UpscanUpload]       = gen[UpscanUpload]
  implicit val uploadReferenceGen: Gen[UploadReference] = gen[UploadReference]
  implicit val upscanSuccessGen: Gen[UpscanSuccess]     = gen[UpscanSuccess]
}

trait DeclarationGen { this: GenUtils =>
  implicit val mrnGen                  = gen[MRN]
  implicit val bankDetailsGen          = gen[BankDetails]
  implicit val accountDetailsGen       = gen[AccountDetails]
  implicit val declarantDetailsGen     = gen[DeclarantDetails]
  implicit val contactDetailsGen       = gen[ContactDetails]
  implicit val consigneeDetailsGen     = gen[ConsigneeDetails]
  implicit val establishmentAddressGen = gen[EstablishmentAddress]
  implicit val consigneeBankDetailsGen = gen[ConsigneeBankDetails]
  implicit val declarantBankDetailsGen = gen[DeclarantBankDetails]
  implicit val securityDetailsGen      = gen[SecurityDetails]
  implicit val taxDetailsGen           = gen[TaxDetails]
  implicit val ndrcDetailsGen          = gen[NdrcDetails]

  // request
  implicit val requestCommonGen: Gen[RequestCommon]              = gen[RequestCommon]
  implicit val requestDetailGen: Gen[RequestDetail]              = gen[RequestDetail]
  implicit val declarationRequestGen: Gen[GetDeclarationRequest] = gen[GetDeclarationRequest]

  // response
  implicit val responseCommonGen: Gen[ResponseCommon]                                               = gen[ResponseCommon]
  implicit val responseDetailGen: Gen[ResponseDetail]                                               = gen[ResponseDetail]
  implicit val declarationInfoResponseGen: Gen[GetDeclarationResponse]                              = gen[GetDeclarationResponse]
  implicit val overpaymentDeclarationDisplayResponseGen: Gen[OverpaymentDeclarationDisplayResponse] =
    gen[OverpaymentDeclarationDisplayResponse]
}
