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
import org.scalacheck.ScalacheckShapeless._
import org.scalacheck.{Arbitrary, Gen}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.cdsreimbursementclaim.models.declaration.request.{DeclarationRequest, OverpaymentDeclarationDisplayRequest, RequestCommon, RequestDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.declaration.response._
import uk.gov.hmrc.cdsreimbursementclaim.models.declaration.{Declaration, MaskedBankDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.upscan.UpscanCallBack.{UploadDetails, UpscanSuccess}
import uk.gov.hmrc.cdsreimbursementclaim.models.upscan.{UploadReference, UpscanUpload}
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}

import scala.reflect.{ClassTag, classTag}

object GenerateUpscan extends Generators with UpscanGen
object GenerateDeclaration extends Generators with DeclarationGen
object GenerateSubmitClaim extends Generators with SubmitClaimGen
object GenerateFrontendSubmitClaim extends Generators with FrontendSumbitClaimGen
object GenerateUploadFiles extends Generators with UploadFileGen
object GenerateWorkItem extends Generators with WorkItemGen

sealed trait Generators extends GenUtils {

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

  implicit val instantArb: Arbitrary[Instant] =
    Arbitrary(
      Gen
        .chooseNum(0L, 10000L)
        .map(Instant.ofEpochMilli)
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
  implicit val uploadDetailsGen: Gen[UploadDetails]     = gen[UploadDetails]
}

trait DeclarationGen { this: GenUtils =>
  implicit val declarationGen          = gen[Declaration]
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
  implicit val requestCommonGen: Gen[RequestCommon]           = gen[RequestCommon]
  implicit val requestDetailGen: Gen[RequestDetail]           = gen[RequestDetail]
  implicit val declarationRequestGen: Gen[DeclarationRequest] = gen[DeclarationRequest]

  // response
  implicit val responseCommonGen: Gen[ResponseCommon]                                               = gen[ResponseCommon]
  implicit val responseDetailGen: Gen[ResponseDetail]                                               = gen[ResponseDetail]
  implicit val declarationInfoResponseGen: Gen[DeclarationResponse]                                 = gen[DeclarationResponse]
  implicit val overpaymentDeclarationDisplayResponseGen: Gen[OverpaymentDeclarationDisplayResponse] =
    gen[OverpaymentDeclarationDisplayResponse]

  implicit val overpaymentDeclarationDisplayRequestGen: Gen[OverpaymentDeclarationDisplayRequest] =
    gen[OverpaymentDeclarationDisplayRequest]

  implicit val maskedBankDetails: Gen[MaskedBankDetails] = gen[MaskedBankDetails]

}

trait SubmitClaimGen { this: GenUtils =>
  //Request
  import uk.gov.hmrc.cdsreimbursementclaim.models.SubmitClaimRequest._
  implicit val establishmentAddressGen    = gen[EstablishmentAddress]
  implicit val cdsEstablishmentAddressGen = gen[CdsEstablishmentAddress]
  implicit val contactInformationGen      = gen[ContactInformation]
  implicit val vatDetailsGen              = gen[VatDetails]
  implicit val agentEoriDetailsGen        = gen[AgentEoriDetails]
  implicit val goodsDetailsGen            = gen[GoodsDetails]
  implicit val importerEoriDetailsGen     = gen[ImporterEoriDetails]
  implicit val eoriDetailsGen             = gen[EoriDetails]
  implicit val contactDetailsGen          = gen[ContactDetails]
  implicit val declarantDetailsGen        = gen[DeclarantDetails]
  implicit val accountDetailsGen          = gen[AccountDetails]
  implicit val consigneeBankDetailsGen    = gen[BankDetails]
  implicit val bankInfoGen                = gen[BankInfo]
  implicit val ndrcDetailsGen             = gen[NdrcDetails]
  implicit val mrnDetailsGen              = gen[MrnDetails]
  implicit val entryDetailsGen            = gen[EntryDetails]
  implicit val requestCommonGen           = gen[RequestCommon]
  implicit val requestDetailGen           = gen[RequestDetail]
  implicit val postNewClaimsRequestGen    = gen[PostNewClaimsRequest]
  implicit val submitClaimRequestGen      = gen[SubmitClaimRequest]

  //Response
  import uk.gov.hmrc.cdsreimbursementclaim.models.SubmitClaimResponse._
  implicit val returnParametersGen      = gen[ReturnParameters]
  implicit val responseCommonGen        = gen[ResponseCommon]
  implicit val postNewClaimsResponseGen = gen[PostNewClaimsResponse]
  implicit val submitClaimResponseGen   = gen[SubmitClaimResponse]

}

trait FrontendSumbitClaimGen { this: GenUtils =>
  import uk.gov.hmrc.cdsreimbursementclaim.models.FrontendSubmitClaim._
  implicit val frontendSumbitClaimGen = gen[FrontendSubmitClaim]
  implicit val fileInformationGen     = gen[FileInformation]

}

trait UploadFileGen { self: GenUtils =>
  implicit val dec64BodyGen = gen[Dec64Body]
}

trait WorkItemGen { self: GenUtils =>
  import uk.gov.hmrc.workitem._
  implicit val workItemHeadersGen = gen[WorkItemHeaders]
  implicit val workItemPayloadGen = gen[WorkItemPayload]
  implicit val workItemGen        = gen[WorkItem[WorkItemPayload]]
}
