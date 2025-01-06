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

package uk.gov.hmrc.cdsreimbursementclaim.services.tpi05

import cats.data.Validated
import cats.data.Validated.Valid
import cats.implicits.toTraverseOps
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.securities.{DeclarantReferenceNumber, DeclarationId, ProcedureCode}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{MethodOfDisposal, NewEoriAndDan, ReimbursementMethodAnswer, SecurityDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.{AcceptanceDate, EisBasicDate, ISO8601DateTime, ISOLocalDate}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.*
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.*
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant.PayeeIndicator
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.{AccountDetails, BtaSource}
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.{CorrelationId, Eori}
import uk.gov.hmrc.cdsreimbursementclaim.models.{CDFPayService, Error as CdsError}
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps

object TPI05 {

  def request(
    claimantEORI: Eori,
    claimantEmailAddress: Email,
    claimantName: String,
    useExistingPaymentMethod: Option[Boolean] = None
  ): Builder = Builder(
    Valid(
      RequestDetail(
        CDFPayService = CDFPayService.NDRC,
        dateReceived = Some(ISOLocalDate.now),
        customDeclarationType = Some(CustomDeclarationType.MRN),
        claimDate = Some(ISOLocalDate.now),
        claimantEORI = claimantEORI,
        claimantEmailAddress = claimantEmailAddress,
        claimantName = Some(claimantName),
        useExistingPaymentMethod = useExistingPaymentMethod
      )
    )
  )

  final case class Builder(validatedRequest: Validated[CdsError, RequestDetail]) {

    def forClaimOfType(claimType: Option[ClaimType]): Builder =
      copy(validatedRequest.map(_.copy(claimType = claimType)))

    def withClaimedAmount(claimedAmount: BigDecimal): Builder =
      copy(validatedRequest.andThen { request =>
        Validated.cond(
          claimedAmount > 0,
          request.copy(
            claimAmountTotal = Some(claimedAmount.roundToTwoDecimalPlaces.toString())
          ),
          CdsError("Total reimbursement amount must be greater than zero")
        )
      })

    def withCaseType(caseType: CaseType): Builder =
      copy(validatedRequest.map(_.copy(caseType = Some(caseType))))

    def withDeclarationMode(declarationMode: DeclarationMode): Builder =
      copy(validatedRequest.map(_.copy(declarationMode = Some(declarationMode))))

    def withDisposalMethod(methodOfDisposal: MethodOfDisposal): Builder =
      copy(validatedRequest.map(_.copy(disposalMethod = Some(methodOfDisposal.toTPI05DisplayString))))

    def withTemporaryAdmissionMethodOfDisposal(
      temporaryAdmissionMethodOfDisposalDetail: Option[TemporaryAdmissionMethodOfDisposalDetail]
    ): Builder =
      copy(validatedRequest.map(_.copy(methodOfDisposals = temporaryAdmissionMethodOfDisposalDetail.map(List(_)))))

    def withReimbursementMethod(reimbursementMethod: ReimbursementMethodAnswer): Builder =
      copy(
        validatedRequest.map(
          _.copy(reimbursementMethod = Some(ReimbursementMethod.from(reimbursementMethod)))
        )
      )

    def withReimbursementMethod(reimbursementMethod: ReimbursementMethodAnswer, enabled: Boolean): Builder =
      if (enabled) {
        copy(
          validatedRequest.map(
            _.copy(reimbursementMethod = Some(ReimbursementMethod.from(reimbursementMethod)))
          )
        )
      } else this

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

    def withClaimant(claimant: Claimant, payeeIndicator: Claimant): Builder =
      copy(
        validatedRequest.map(
          _.copy(
            claimant = Some(claimant),
            payeeIndicator = Some(payeeIndicator)
          )
        )
      )

    def withEORIDetails(eoriDetails: EoriDetails): Builder =
      copy(validatedRequest.map(_.copy(EORIDetails = Some(eoriDetails))))

    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    def withMrnDetails(mrnDetails: List[MrnDetail.Builder]): Builder =
      copy(validatedRequest.andThen { request =>
        mrnDetails
          .ensuring(_.nonEmpty)
          .map(_.validated)
          .sequence
          .map { details =>
            request.copy(MRNDetails = Some(details))
          }
          .leftMap { errors =>
            CdsError(s"Failed to build MRN detail - ${errors.map(_.value).toList.distinct.mkString(";\n")}")
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
            CdsError(s"Failed to build Duplicate MRN detail - ${errors.map(_.value).toList.distinct.mkString(";\n")}")
          }
      })

    def withGoodsDetails(goodsDetails: GoodsDetails): Builder =
      copy(validatedRequest.map(_.copy(goodsDetails = Some(goodsDetails))))

    def withAdditionalDetails(additionalDetails: Option[String]): Builder =
      additionalDetails
        .map(a => copy(validatedRequest.map(_.copy(goodsDetails = Some(GoodsDetails(descOfGoods = Some(a)))))))
        .getOrElse(this)

    def withSecurityInfo(
      dateClaimReceived: Option[EisBasicDate],
      reasonForSecurity: ReasonForSecurity,
      declarationId: DeclarationId,
      procedureCode: ProcedureCode,
      acceptanceDate: AcceptanceDate,
      declarantReferenceNumber: Option[DeclarantReferenceNumber],
      btaSource: Option[BtaSource],
      btaDueDate: Option[EisBasicDate],
      declarantDetails: MRNInformation,
      consigneeDetails: MRNInformation,
      accountDetails: Option[List[AccountDetails]],
      securityDetails: List[SecurityDetail]
    ): Builder =
      copy(
        validatedRequest.map(
          _.copy(
            CDFPayService = CDFPayService.SCTY,
            security = Some(
              SecurityInfo(
                dateClaimReceived = dateClaimReceived,
                reasonForSecurity = Some(reasonForSecurity.acc14Code),
                declarationID = Some(declarationId),
                procedureCode = Some(procedureCode),
                acceptanceDate = Some(EisBasicDate(acceptanceDate.value)),
                declarantReferenceNumber = declarantReferenceNumber,
                BTASource = btaSource,
                BTADueDate = btaDueDate,
                declarantDetails = Some(declarantDetails),
                consigneeDetails = Some(consigneeDetails),
                accountDetails = accountDetails.map(
                  _.map(acc =>
                    AccountDetail(
                      accountType = acc.accountType,
                      accountNumber = acc.accountNumber,
                      EORI = acc.eori,
                      legalName = acc.legalName,
                      contactDetails = acc.contactDetails.map(contact =>
                        ContactInformation(
                          contactPerson = contact.contactName,
                          addressLine1 = contact.addressLine1,
                          addressLine2 = contact.addressLine2,
                          addressLine3 = contact.addressLine3,
                          street = None,
                          city = None,
                          countryCode = contact.countryCode,
                          postalCode = contact.postalCode,
                          telephoneNumber = contact.telephone,
                          faxNumber = None,
                          emailAddress = contact.emailAddress
                        )
                      )
                    )
                  )
                ),
                securityDetails = Some(securityDetails)
              )
            )
          )
        )
      )

    def withSecurityPaymentDetails(
      bankDetails: Option[BankDetails],
      reimbursementMethod: Option[ReimbursementMethod],
      useExistingPaymentMethod: Option[Boolean]
    ): Builder =
      copy(
        validatedRequest.map(requestDetail =>
          requestDetail.copy(
            reimbursementMethod = reimbursementMethod,
            useExistingPaymentMethod = useExistingPaymentMethod,
            security = requestDetail.security.map(
              _.copy(
                bankDetails = bankDetails
              )
            )
          )
        )
      )

    def withReimbursementParty(reimbursementParty: ReimbursementParty, payeeIndicator: Claimant): Builder =
      copy(
        validatedRequest.map(
          _.copy(reimbursementParty = Some(reimbursementParty), payeeIndicator = Some(payeeIndicator))
        )
      )

    def withClaimantAddress(claimantAddress: Address): Builder =
      copy(validatedRequest.map(_.copy(claimantAddress = Some(claimantAddress))))

    def withMaybeNewEORIAndDAN(newEoriAndDan: Option[NewEoriAndDan]): Builder =
      newEoriAndDan match {
        case None                           => this
        case Some(NewEoriAndDan(eori, dan)) =>
          copy(validatedRequest.map(_.copy(newEORI = Some(eori), newDAN = Some(dan))))
      }

    def verify: Either[CdsError, EisSubmitClaimRequest] =
      validatedRequest.toEither.map { requestDetail =>
        EisSubmitClaimRequest(
          PostNewClaimsRequest(
            RequestCommon(
              originatingSystem = Platform.MDTP,
              receiptDate = ISO8601DateTime.now,
              acknowledgementReference = CorrelationId.compact
            ),
            requestDetail
          )
        )
      }
  }
}
