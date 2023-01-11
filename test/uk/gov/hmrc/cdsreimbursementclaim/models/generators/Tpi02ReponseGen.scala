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

package uk.gov.hmrc.cdsreimbursementclaim.models.generators

import cats.syntax.eq._
import org.scalacheck.Gen
import uk.gov.hmrc.cdsreimbursementclaim.models.CDFPayService
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.CdsDateTime
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.CorrelationId
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi02.ndrc.{EntryDetail, NDRCAmounts, NDRCCase, NDRCDetail, ProcedureDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi02.scty.{Goods, SCTYCase}
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi02.{ErrorDetail, ErrorResponse, GetSpecificCaseResponse, Reimbursement, ResponseCommon, ResponseDetail, ReturnParameter, SourceFaultDetail}

object Tpi02ReponseGen {

  import Tpi01ReponseGen._

  val genResponseCommonSuccess: Gen[ResponseCommon] =
    Gen.const(
      ResponseCommon(
        status = "OK",
        processingDate = CdsDateTime.now,
        correlationId = Some(CorrelationId()),
        errorMessage = None,
        returnParameters = None
      )
    )

  val genResponseCommonError: Gen[ResponseCommon] =
    Gen.const(
      ResponseCommon(
        status = "ERROR",
        processingDate = CdsDateTime.now,
        correlationId = Some(CorrelationId()),
        errorMessage = Some("error message"),
        returnParameters = Some(List(ReturnParameter("key", "value")))
      )
    )

  val genReimbursement: Gen[Reimbursement] =
    for {
      reimbursementDate   <- Gen.const("20220220")
      reimbursementAmount <- genAmount
      taxType             <- TaxCodesGen.genTaxCode.map(_.value)
      reimbursementMethod <- Gen.oneOf("PRCDMGI".iterator.toSeq).map(_.toString)
    } yield Reimbursement(
      reimbursementDate,
      reimbursementAmount,
      taxType,
      reimbursementMethod
    )

  val genGoods: Gen[Goods] =
    for {
      itemNumber       <- Gen.listOfN(12, genChar).map(_.mkString)
      goodsDescription <- loremIpsum
    } yield Goods(itemNumber, Some(goodsDescription))

  val genProcedureDetail: Gen[ProcedureDetail] =
    for {
      MRNNumber <- IdGen.genMRN.map(_.value)
    } yield ProcedureDetail(MRNNumber, false)

  val genEntryNumber: Gen[String] =
    Gen.listOfN(12, genChar).map(_.mkString)

  val genEntryDetail: Gen[EntryDetail] =
    for {
      entryNumber <- genEntryNumber
    } yield EntryDetail(entryNumber, true)

  val genNdrcDetail: Gen[NDRCDetail] = for {
    CDFPayCaseNumber         <- genCdfPayCaseNumber
    declarationID            <- IdGen.genMRN
    claimType                <- Gen.oneOf("C285", "C&E1179")
    caseType                 <-
      if (claimType === "C285") Gen.oneOf("Individual", "Bulk", "CMA", "C18")
      else Gen.oneOf("Individual", "Bulk", "CMA")
    reasonForSecurity        <- Gen.oneOf(ReasonForSecurity.values)
    procedureCode            <- Gen.const("AB")
    caseStatus               <- genCaseStatusNdrc
    descOfGoods              <- loremIpsum
    descOfRejectedGoods      <- loremIpsum
    goods                    <- Gen.listOfN(3, genGoods)
    declarantEORI            <- IdGen.genEori
    importerEORI             <- IdGen.genEori
    claimantEORI             <- IdGen.genEori.map(e => Some.apply(e.value))
    totalCustomsClaimAmount  <- genAmount
    totalVATClaimAmount      <- genAmount
    totalClaimAmount         <- genAmount
    totalReimbursementAmount <- genAmount
    claimStartDate           <- Gen.const("20220220")
    claimantName             <- loremIpsum
    claimantEmailAddress     <- loremIpsum
    closedDate               <- if (isClosed(caseStatus)) Gen.const(Some("20220220"))
                                else Gen.const(None)
    MRNDetails               <- Gen.listOfN(3, genProcedureDetail)
    entryDetails             <- Gen.listOfN(3, genEntryDetail)
    reimbursement            <- Gen.listOfN(5, genReimbursement)
    basisOfClaim             <- Gen.oneOf(
                                  C285ClaimGen.genBasisOfClaim.map(_.toTPI05DisplayString),
                                  RejectedGoodsClaimGen.genBasisOfRejectedGoodsClaim.map(_.toTPI05DisplayString)
                                )
  } yield NDRCDetail(
    CDFPayCaseNumber,
    Some(declarationID.value),
    claimType,
    caseType,
    caseStatus,
    Some(descOfGoods),
    Some(descOfRejectedGoods),
    declarantEORI.value,
    importerEORI.value,
    claimantEORI,
    Some(basisOfClaim),
    claimStartDate,
    Some(claimantName),
    Some(claimantEmailAddress),
    closedDate,
    Some(MRNDetails),
    Some(entryDetails),
    Some(reimbursement)
  )

  val genNdrcAmounts: Gen[NDRCAmounts] = for {
    totalCustomsClaimAmount  <- genAmount
    totalVATClaimAmount      <- genAmount
    totalExciseClaimAmount   <- genAmount
    totalClaimAmount         <- genAmount
    totalCustomsRefundAmount <- genAmount
    totalVATRefundAmount     <- genAmount
    totalExciseRefundAmount  <- genAmount
    totalRefundAmount        <- genAmount
    totalReimbursmentAmount  <- genAmount

  } yield NDRCAmounts(
    Some(totalCustomsClaimAmount),
    Some(totalVATClaimAmount),
    Some(totalExciseClaimAmount),
    Some(totalClaimAmount),
    Some(totalCustomsRefundAmount),
    Some(totalVATRefundAmount),
    Some(totalExciseRefundAmount),
    Some(totalRefundAmount),
    Some(totalReimbursmentAmount)
  )

  val genNdrcCase: Gen[NDRCCase] =
    for {
      NDRCDetail  <- genNdrcDetail
      NDRCAmounts <- genNdrcAmounts
    } yield NDRCCase(
      NDRCDetail,
      NDRCAmounts
    )

  val genSctyCase: Gen[SCTYCase] =
    for {
      CDFPayCaseNumber         <- genCdfPayCaseNumber
      declarationID            <- IdGen.genMRN
      reasonForSecurity        <- Gen.oneOf(ReasonForSecurity.values)
      procedureCode            <- Gen.const("AB")
      caseStatus               <- genCaseStatusScty
      goods                    <- Gen.listOfN(3, genGoods)
      declarantEORI            <- IdGen.genEori
      importerEORI             <- IdGen.genEori
      claimantEORI             <- IdGen.genEori.map(e => Some.apply(e.value))
      totalCustomsClaimAmount  <- genAmount
      totalVATClaimAmount      <- genAmount
      totalClaimAmount         <- genAmount
      totalReimbursementAmount <- genAmount
      claimStartDate           <- Gen.const("20220220")
      claimantName             <- loremIpsum
      claimantEmailAddress     <- loremIpsum
      closedDate               <- if (isClosed(caseStatus)) Gen.const(Some("20220220"))
                                  else Gen.const(None)
      reimbursement            <- Gen.listOfN(5, genReimbursement)
    } yield SCTYCase(
      CDFPayCaseNumber,
      Some(declarationID.value),
      reasonForSecurity.acc14Code,
      procedureCode,
      caseStatus,
      Some(goods),
      declarantEORI.value,
      importerEORI.value,
      claimantEORI,
      Some(totalCustomsClaimAmount),
      Some(totalVATClaimAmount),
      Some(totalClaimAmount),
      Some(totalReimbursementAmount),
      claimStartDate,
      Some(claimantName),
      Some(claimantEmailAddress),
      closedDate,
      Some(reimbursement)
    )

  val genResponseDetailNdrc: Gen[ResponseDetail] =
    for {
      NDRCCase <- genNdrcCase
    } yield ResponseDetail(
      CDFPayService.NDRC.toString,
      true,
      Some(NDRCCase),
      None
    )

  val genResponseDetailScty: Gen[ResponseDetail] =
    for {
      SctyCase <- genSctyCase
    } yield ResponseDetail(
      CDFPayService.SCTY.toString,
      true,
      None,
      Some(SctyCase)
    )

  val getGetSpecificCaseResponseNdrc: Gen[GetSpecificCaseResponse] =
    for {
      responseCommon <- genResponseCommonSuccess
      responseDetail <- genResponseDetailNdrc
    } yield GetSpecificCaseResponse(responseCommon, Some(responseDetail))

  val getGetSpecificCaseResponseScty: Gen[GetSpecificCaseResponse] =
    for {
      responseCommon <- genResponseCommonSuccess
      responseDetail <- genResponseDetailScty
    } yield GetSpecificCaseResponse(responseCommon, Some(responseDetail))

  val getGetSpecificCaseResponseEmpty: Gen[GetSpecificCaseResponse] =
    for {
      responseCommon <- genResponseCommonError
    } yield GetSpecificCaseResponse(responseCommon, None)

  def genErrorResponse(status: Int): Gen[ErrorResponse] =
    Gen.const(
      ErrorResponse(
        status,
        Some(
          ErrorDetail(
            timestamp = CdsDateTime.now,
            correlationId = CorrelationId(),
            errorCode = s"$status",
            errorMessage = "Some error message",
            source = "foo",
            sourceFaultDetail = SourceFaultDetail(Seq("source fault detail"))
          )
        )
      )
    )

}
