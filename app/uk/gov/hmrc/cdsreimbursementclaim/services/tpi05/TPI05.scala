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

package uk.gov.hmrc.cdsreimbursementclaim.services.tpi05

import cats.data.Validated
import cats.data.Validated.Valid
import cats.implicits.{catsSyntaxEq, toTraverseOps}
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ReimbursementMethodAnswer.CurrentMonthAdjustment
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{MethodOfDisposal, ReimbursementMethodAnswer}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.{ISO8601DateTime, ISOLocalDate}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums._
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.Eori
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.UUIDGenerator.compactCorrelationId
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps

object TPI05 {

  def request: Builder = Builder(
    Valid(
      RequestDetail(
        CDFPayService = CDFPayService.NDRC,
        dateReceived = Some(ISOLocalDate.now),
        customDeclarationType = Some(CustomDeclarationType.MRN),
        claimDate = Some(ISOLocalDate.now)
      )
    )
  )

  final case class Builder private (validatedRequest: Validated[Error, RequestDetail]) extends AnyVal {

    def forClaimOfType(claimType: ClaimType): Builder =
      copy(validatedRequest.map(_.copy(claimType = Some(claimType))))

    def withClaimedAmount(claimedAmount: BigDecimal): Builder =
      copy(validatedRequest.andThen { request =>
        Validated.cond(
          claimedAmount > 0,
          request.copy(
            claimAmountTotal = Some(claimedAmount.roundToTwoDecimalPlaces.toString())
          ),
          Error("Total reimbursement amount must be greater than zero")
        )
      })

    def withCaseType(caseType: CaseType): Builder =
      copy(validatedRequest.map(_.copy(caseType = Some(caseType))))

    def withDeclarationMode(declarationMode: DeclarationMode): Builder =
      copy(validatedRequest.map(_.copy(declarationMode = Some(declarationMode))))

    def withDisposalMethod(methodOfDisposal: MethodOfDisposal): Builder =
      copy(validatedRequest.map(_.copy(disposalMethod = Some(methodOfDisposal.toTPI05Key))))

    def withReimbursementMethod(reimbursementMethod: ReimbursementMethodAnswer): Builder =
      copy(
        validatedRequest.map(
          _.copy(reimbursementMethod =
            Some(
              if (reimbursementMethod === CurrentMonthAdjustment) ReimbursementMethod.Deferment
              else ReimbursementMethod.BankTransfer
            )
          )
        )
      )

    def withBasisOfClaim(basisOfClaim: String): Builder =
      copy(validatedRequest.map(_.copy(basisOfClaim = Some(basisOfClaim))))

    def withClaimant(claimant: Claimant): Builder =
      copy(
        validatedRequest.map(
          _.copy(
            claimant = Some(claimant),
            payeeIndicator = Some(claimant)
          )
        )
      )

    def withClaimantEmail(claimantEmailAddress: Option[Email]): Builder =
      copy(validatedRequest.andThen { request =>
        Validated.cond(
          claimantEmailAddress.nonEmpty,
          request.copy(claimantEmailAddress = claimantEmailAddress),
          Error("Email address is missing")
        )
      })

    def withClaimantEORI(eori: Eori): Builder =
      copy(validatedRequest.map(_.copy(claimantEORI = Some(eori))))

    def withEORIDetails(eoriDetails: EoriDetails): Builder =
      copy(validatedRequest.map(_.copy(EORIDetails = Some(eoriDetails))))

    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    def withMrnDetails(mrnDetails: MrnDetail.Builder*): Builder =
      copy(validatedRequest.andThen { request =>
        mrnDetails.toList
          .ensuring(_.nonEmpty)
          .map(_.validated)
          .sequence
          .map { details =>
            request.copy(MRNDetails = Some(details))
          }
          .leftMap { errors =>
            Error(s"Cannot build MRN detail due to validation errors: ${errors.toList.mkString("|")}")
          }
      })

    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    def withMaybeDuplicateMrnDetails(maybeDuplicateMrnDetails: Option[MrnDetail.Builder]): Builder =
      copy(validatedRequest.andThen { request =>
        maybeDuplicateMrnDetails
          .map(_.validated)
          .sequence
          .map(maybeDetails => request.copy(duplicateMRNDetails = maybeDetails))
          .leftMap { errors =>
            Error(s"Cannot build Duplicate MRN detail due to validation errors: ${errors.toList.mkString("|")}")
          }
      })

    def withGoodsDetails(goodsDetails: GoodsDetails): Builder =
      copy(validatedRequest.map(_.copy(goodsDetails = Some(goodsDetails))))

    def verify: Either[Error, EisSubmitClaimRequest] =
      validatedRequest.toEither.map { requestDetail =>
        EisSubmitClaimRequest(
          PostNewClaimsRequest(
            RequestCommon(
              originatingSystem = Platform.MDTP,
              receiptDate = ISO8601DateTime.now,
              acknowledgementReference = compactCorrelationId
            ),
            requestDetail
          )
        )
      }
  }
}
