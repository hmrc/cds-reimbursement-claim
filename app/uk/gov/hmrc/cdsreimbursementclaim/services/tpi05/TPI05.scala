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
import cats.implicits.catsSyntaxEq
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ReimbursementMethodAnswer.CurrentMonthAdjustment
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{DeclarantTypeAnswer, MethodOfDisposal, ReimbursementMethodAnswer}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.{ISO8601DateTime, ISOLocalDate}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums._
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

  final case class Builder private (validation: Validated[Error, RequestDetail]) extends AnyVal {

    def forClaimOfType(claimType: ClaimType): Builder =
      copy(validation.map(_.copy(claimType = Some(claimType))))

    def withClaimedAmount(claimedAmount: BigDecimal): Builder =
      copy(validation.andThen { request =>
        Validated.cond(
          claimedAmount > 0,
          request.copy(
            claimAmountTotal = Some(claimedAmount.roundToTwoDecimalPlaces)
          ),
          Error("Total reimbursement amount must be greater than zero")
        )
      })

    def withCaseType(caseType: CaseType): Builder =
      copy(validation.map(_.copy(caseType = Some(caseType))))

    def withDeclarationMode(declarationMode: DeclarationMode): Builder =
      copy(validation.map(_.copy(declarationMode = Some(declarationMode))))

    def withDisposalMethod(methodOfDisposal: MethodOfDisposal): Builder =
      copy(validation.map(_.copy(disposalMethod = Some(methodOfDisposal))))

    def withReimbursementMethod(reimbursementMethod: ReimbursementMethodAnswer): Builder =
      copy(
        validation.map(
          _.copy(reimbursementMethod =
            Some(
              if (reimbursementMethod === CurrentMonthAdjustment) ReimbursementMethod.Deferment
              else ReimbursementMethod.BankTransfer
            )
          )
        )
      )

    def withBasisOfClaim(basisOfClaim: String): Builder =
      copy(validation.map(_.copy(basisOfClaim = Some(basisOfClaim))))

    def withClaimant(declarantType: DeclarantTypeAnswer): Builder = {
      val payeeIndicator =
        Option(declarantType === DeclarantTypeAnswer.Importer)
          .filter(_ === true)
          .map(_ => "Importer")
          .orElse(Some("Declarant"))

      copy(validation.map(_.copy(
        claimant = payeeIndicator,
        payeeIndicator = payeeIndicator
      )))
    }

    def verify: Either[Error, EisSubmitClaimRequest] =
      validation.toEither.map { requestDetail =>
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
