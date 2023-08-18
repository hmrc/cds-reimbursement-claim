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

import org.scalacheck.Gen
import uk.gov.hmrc.cdsreimbursementclaim.models.{EisErrorResponse, SourceFaultDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.{ErrorDetail, SourceFaultDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.CdsDateTime
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReasonForSecurity
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.CorrelationId
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi01._

import scala.collection.immutable

object Tpi01ReponseGen {

  val letterChars: Set[Char] = Set('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K')
  val digitChars: Set[Char]  = Set('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

  val genChar: Gen[Char] = Gen.oneOf(letterChars ++ digitChars)

  val genCdfPayCaseNumber: Gen[String] =
    Gen.listOfN(6, genChar).map(_.mkString("NDRC-", "", ""))

  val loremIpsum: Gen[String] = Gen.const(
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. "
  )

  val genCaseStatusNdrc: Gen[String] = Gen.oneOf(
    "Open",
    "Open-Analysis",
    "Pending-Approval",
    "Pending-Queried",
    "Resolved-Withdrawn",
    "Rejected-Failed Validation",
    "Resolved-Rejected",
    "Open-Rework",
    "Paused",
    "Resolved-No Reply",
    "RTBH-Sent",
    "Resolved-Refused",
    "Pending Payment Confirmation",
    "Resolved-Approved",
    "Resolved-Partial Refused",
    "Pending Decision Letter",
    "Approved",
    "Analysis-Rework",
    "Rework-Payment Details",
    "Reply To RTBH",
    "Pending-Compliance Recommendation",
    "Pending-Compliance Check Query",
    "Pending-Compliance Check"
  )

  val genCaseStatusScty: Gen[String] = Gen.oneOf(
    "Open",
    "Pending-Approval",
    "Pending-Payment",
    "Partial Refund",
    "Resolved-Refund",
    "Pending-Query",
    "Resolved-Manual BTA",
    "Pending-C18",
    "Closed-C18 Raised",
    "RTBH Letter Initiated",
    "Awaiting RTBH Letter Response",
    "Reminder Letter Initiated",
    "Awaiting Reminder Letter Response",
    "Decision Letter Initiated",
    "Partial BTA",
    "Partial BTA/Refund",
    "Resolved-Auto BTA",
    "Resolved-Manual BTA/Refund",
    "Open-Extension Granted",
    "Resolved-Withdrawn"
  )

  def isClosed(caseStatus: String): Boolean =
    caseStatus.startsWith("Resolved-") ||
      caseStatus.startsWith("Rejected-") ||
      caseStatus.contains("Approved")

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

  val genAmountDigits: Gen[String] =
    Gen.listOfN(6, Gen.oneOf(digitChars - '0')).map(_.mkString)

  val genAmountFraction: Gen[String] =
    Gen.listOfN(2, Gen.oneOf(digitChars)).map(_.mkString(".", "", ""))

  val genAmount: Gen[String] =
    genAmountDigits.flatMap(a => Gen.oneOf(Gen.const(""), genAmountFraction).map(a + _))

  val genAmountOpt: Gen[Option[String]] =
    Gen.oneOf(Gen.const(None), genAmount.map(Some.apply))

  val genNDRCCaseDetails: Gen[NDRCCaseDetails] =
    for {
      CDFPayCaseNumber         <- genCdfPayCaseNumber
      declarationID            <- IdGen.genMRN
      claimStartDate           <- Gen.const("20220220")
      caseStatus               <- genCaseStatusNdrc
      closedDate               <- if (isClosed(caseStatus)) Gen.const(Some("20220220"))
                                  else Gen.const(None)
      declarantEORI            <- IdGen.genEori
      importerEORI             <- IdGen.genEori
      claimantEORI             <- IdGen.genEori
      totalCustomsClaimAmount  <- genAmountOpt
      totalVATClaimAmount      <- genAmountOpt
      totalExciseClaimAmount   <- genAmountOpt
      declarantReferenceNumber <- IdGen.genMRN
      basisOfClaim             <- Gen.oneOf(
                                    C285ClaimGen.genBasisOfClaim.map(_.toTPI05DisplayString),
                                    RejectedGoodsClaimGen.genBasisOfRejectedGoodsClaim.map(_.toTPI05DisplayString)
                                  )
    } yield NDRCCaseDetails(
      CDFPayCaseNumber,
      Some(declarationID.value),
      claimStartDate,
      closedDate,
      caseStatus,
      declarantEORI.value,
      importerEORI.value,
      Some(claimantEORI.value),
      totalCustomsClaimAmount,
      totalVATClaimAmount,
      totalExciseClaimAmount,
      Some(declarantReferenceNumber.value),
      Some(basisOfClaim)
    )

  val genSCTYCaseDetails: Gen[SCTYCaseDetails] =
    for {
      CDFPayCaseNumber         <- genCdfPayCaseNumber
      declarationID            <- IdGen.genMRN
      claimStartDate           <- Gen.const("20220220")
      caseStatus               <- genCaseStatusScty
      closedDate               <- if (isClosed(caseStatus)) Gen.const(Some("20220220"))
                                  else Gen.const(None)
      declarantEORI            <- IdGen.genEori
      importerEORI             <- IdGen.genEori
      claimantEORI             <- IdGen.genEori
      totalCustomsClaimAmount  <- genAmountOpt
      totalVATClaimAmount      <- genAmountOpt
      declarantReferenceNumber <- IdGen.genMRN
      reasonForSecurity        <- Gen.oneOf(ReasonForSecurity.values)
    } yield SCTYCaseDetails(
      CDFPayCaseNumber,
      Some(declarationID.value),
      Some(claimStartDate),
      closedDate,
      reasonForSecurity.acc14Code,
      caseStatus,
      declarantEORI.value,
      Some(importerEORI.value),
      Some(claimantEORI.value),
      totalCustomsClaimAmount,
      totalVATClaimAmount,
      Some(declarantReferenceNumber.value)
    )

  def totalOf(cases: immutable.Seq[CaseDetails]): String =
    cases.map(_.total).sum.toString

  def genCdfPayCase(includeNdrcCases: Boolean, includeSctyCases: Boolean): Gen[CDFPayCase] =
    for {
      NDRCCases <- if (includeNdrcCases)
                     Gen.nonEmptyListOf(genNDRCCaseDetails).map(Some.apply)
                   else Gen.const(None)
      SCTYCases <- if (includeNdrcCases)
                     Gen.nonEmptyListOf(genSCTYCaseDetails).map(Some.apply)
                   else Gen.const(None)
    } yield CDFPayCase(
      NDRCCases.map(totalOf),
      NDRCCases,
      SCTYCases.map(totalOf),
      SCTYCases
    )

  def genResponseDetail(includeNdrcCases: Boolean, includeSctyCases: Boolean): Gen[ResponseDetail] =
    for {
      cdfPayCase <- if (includeNdrcCases || includeSctyCases)
                      genCdfPayCase(includeNdrcCases, includeSctyCases).map(Some.apply)
                    else Gen.const(None)
    } yield ResponseDetail(
      NDRCCasesFound = includeNdrcCases,
      SCTYCasesFound = includeSctyCases,
      CDFPayCase = cdfPayCase
    )

  def genGetReimbursementClaimsResponse(
    includeNdrcCases: Boolean,
    includeSctyCases: Boolean
  ): Gen[GetReimbursementClaimsResponse] =
    for {
      responseCommon <- genResponseCommonSuccess
      responseDetail <- genResponseDetail(includeNdrcCases, includeSctyCases)
    } yield GetReimbursementClaimsResponse(
      responseCommon,
      Some(responseDetail)
    )

  val tossTheCoin: Gen[Boolean] = Gen.frequency((30, Gen.const(false)), (70, Gen.const(true)))

  val genGetReimbursementClaimsResponseVariant: Gen[GetReimbursementClaimsResponse] =
    for {
      includeNdrcCases <- tossTheCoin
      includeSctyCases <- tossTheCoin
      response         <- genGetReimbursementClaimsResponse(includeNdrcCases, includeSctyCases)
    } yield response

  val genGetReimbursementClaimsResponseEmpty: Gen[GetReimbursementClaimsResponse] =
    for {
      responseCommon <- genResponseCommonError
    } yield GetReimbursementClaimsResponse(
      responseCommon,
      None
    )

  def genErrorResponse(status: Int): Gen[EisErrorResponse] =
    Gen.const(
      EisErrorResponse(
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
