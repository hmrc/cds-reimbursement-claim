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

package uk.gov.hmrc.cdsreimbursementclaim.services.tpi05

import cats.implicits.catsSyntaxEq
import cats.syntax.either._
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ClaimantType.{Consignee, Declarant}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.securities.{DeclarantReferenceNumber, DeclarationId, ProcedureCode}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimantType, SecuritiesClaim, SecurityDetail, TaxCode, TaxReclaimDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.{AcceptanceDate, EisBasicDate}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.{BankAccountDetails, BtaSource, SecurityDetails, TaxDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.TemporaryAdmissionMethodOfDisposal.{ExportedInMultipleShipments, ExportedInSingleShipment}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{ExportMRN, ReasonForSecurity, ReimbursementMethod, ReimbursementParty, TemporaryAdmissionMethodOfDisposal}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error => CdsError}

import java.time.LocalDate

class SecuritiesClaimToTPI05Mapper extends ClaimToTPI05Mapper[(SecuritiesClaim, DisplayDeclaration)] {

  def map(request: (SecuritiesClaim, DisplayDeclaration)): Either[CdsError, EisSubmitClaimRequest] = {
    val (claim, displayDeclaration) = request
    (for {
      email                                                        <- claim.claimantInformation.contactInformation.emailAddress.toRight(
                                                                        CdsError("claimant email address is mandatory")
                                                                      )
      claimantName                                                 <- claim.claimantInformation.contactInformation.contactPerson.toRight(
                                                                        CdsError("claimant contact name is mandatory")
                                                                      )
      claimantEmail                                                 = Email(email)
      claimedAmount                                                 = claim.securitiesReclaims.flatMap(_._2.map { case (_, value: BigDecimal) => value }).sum
      declarantDetails                                              = MRNInformation.fromDeclarantDetails(displayDeclaration.displayResponseDetail.declarantDetails)
      consigneeDetails                                              = displayDeclaration.displayResponseDetail.consigneeDetails
                                                                        .map(MRNInformation.fromConsigneeDetails(_))
                                                                        .getOrElse(declarantDetails)
      securities                                                    = displayDeclaration.displayResponseDetail.securityDetails.toList.flatten
      securityDetails                                              <- getSecurityDetails(securities, claim.securitiesReclaims)
      acceptanceDate                                               <- AcceptanceDate
                                                                        .fromDisplayFormat(displayDeclaration.displayResponseDetail.acceptanceDate)
                                                                        .toEither
                                                                        .leftMap(x => CdsError(s"acceptance date could not be parsed: $x"))
      declarantReferenceNumber                                      = displayDeclaration.displayResponseDetail.declarantReferenceNumber
      btaSource                                                     = displayDeclaration.displayResponseDetail.btaSource
      btaDueDate                                                    = displayDeclaration.displayResponseDetail.btaDueDate
                                                                        .flatMap(EisBasicDate.parse(_).toOption)
      accountDetails                                                = displayDeclaration.displayResponseDetail.accountDetails
      securityDeposits                                              = displayDeclaration.displayResponseDetail.securityDetails.toList.flatten
      methodOfDisposalDetail                                       <- (claim.temporaryAdmissionMethodOfDisposal, claim.exportMovementReferenceNumber) match {
                                                                        case (Some(ExportedInSingleShipment), None)    =>
                                                                          Left(
                                                                            CdsError("Export MRN must be provided when disposal method is single shipment")
                                                                          )
                                                                        case (Some(ExportedInMultipleShipments), None) =>
                                                                          Left(
                                                                            CdsError("Export MRN must be provided when disposal method is multiple shipments")
                                                                          )
                                                                        case (Some(exportMethod), Some(_))
                                                                            if !TemporaryAdmissionMethodOfDisposal.requiresMrn.contains(exportMethod) =>
                                                                          Left(CdsError("Unexpected export MRN supplied"))
                                                                        case (Some(_), _)
                                                                            if !ReasonForSecurity.temporaryAdmissions.contains(claim.reasonForSecurity) =>
                                                                          Left(CdsError("Unexpected disposal method for non-temporary-admission security"))
                                                                        case (None, _)
                                                                            if ReasonForSecurity.temporaryAdmissions.contains(claim.reasonForSecurity) =>
                                                                          Left(CdsError("disposal method missing"))
                                                                        case (Some(disposalMethod), None)              =>
                                                                          Right(Some(TemporaryAdmissionMethodOfDisposalDetail(disposalMethod.eisCode, None)))
                                                                        case (Some(disposalMethod), Some(exportMRN))   =>
                                                                          Right(
                                                                            Some(
                                                                              TemporaryAdmissionMethodOfDisposalDetail(
                                                                                disposalMethod.eisCode,
                                                                                Some(List(ExportMRN(exportMRN)))
                                                                              )
                                                                            )
                                                                          )
                                                                        case (None, None)                              => Right(None)
                                                                      }
      selectedSecurityDeposits                                      =
        securityDeposits.filter(deposit => claim.securitiesReclaims.exists(_._1 === deposit.securityDepositId))
      securityPaymentDetails                                       <-
        getSecurityPaymentDetails(claim.claimantType, claim.bankAccountDetails, selectedSecurityDeposits)
      (bankDetails, useExistingPaymentDetails, reimbursementMethod) = securityPaymentDetails
    } yield TPI05
      .request(
        claimantEORI = claim.claimantInformation.eori,
        claimantEmailAddress = claimantEmail,
        claimantName = claimantName
      )
      .forClaimOfType(None)
      .withClaimedAmount(claimedAmount)
      .withSecurityInfo(
        dateClaimReceived = Some(EisBasicDate(LocalDate.now)),
        reasonForSecurity = claim.reasonForSecurity,
        declarationId = DeclarationId(claim.movementReferenceNumber.value),
        procedureCode = ProcedureCode(displayDeclaration.displayResponseDetail.procedureCode),
        acceptanceDate = acceptanceDate,
        declarantReferenceNumber = declarantReferenceNumber.map(DeclarantReferenceNumber(_)),
        btaSource = btaSource.map(BtaSource(_)),
        btaDueDate = btaDueDate,
        declarantDetails = declarantDetails,
        consigneeDetails = consigneeDetails,
        accountDetails = accountDetails,
        securityDetails = securityDetails
      )
      .withReimbursementParty(claim.claimantType match {
        case ClaimantType.Consignee => ReimbursementParty.Consignee
        case ClaimantType.Declarant => ReimbursementParty.Declarant
        case ClaimantType.User      => ReimbursementParty.Declarant
      })
      .withSecurityPaymentDetails(
        reimbursementMethod = reimbursementMethod,
        useExistingPaymentMethod = useExistingPaymentDetails,
        bankDetails = bankDetails
      )
      .withTemporaryAdmissionMethodOfDisposal(methodOfDisposalDetail)).flatMap(x => x.verify)
  }

  private def getSecurityPaymentDetails(
    claimantType: ClaimantType,
    bankAccountDetails: Option[BankAccountDetails],
    securityDeposits: List[SecurityDetails]
  ): Either[CdsError, (BankDetails, Option[Boolean], Option[ReimbursementMethod])] = {
    val bankDetails = getBankDetails(claimantType, bankAccountDetails)
    securityDeposits.map(_.paymentMethod).toSet.toList match {
      case "001" :: Nil => Right((bankDetails, Some(true), None))
      case "004" :: Nil => Right((BankDetails(None, None), Some(false), Some(ReimbursementMethod.GeneralGuarantee)))
      case "005" :: Nil => Right((BankDetails(None, None), Some(false), Some(ReimbursementMethod.IndividualGuarantee)))
      case Nil          => Left(CdsError("No security deposits"))
      case _            =>
        bankAccountDetails match {
          case Some(_) => Right((bankDetails, Some(true), None))
          case None    => Left(CdsError("Could not determine payment method"))
        }
    }
  }

  private def getBankDetails(claimantType: ClaimantType, bankAccountDetails: Option[BankAccountDetails]): BankDetails =
    claimantType match {
      case Consignee => BankDetails(bankAccountDetails.map(BankDetail.from), None)
      case Declarant => BankDetails(None, bankAccountDetails.map(BankDetail.from))
      case _         => BankDetails(None, None)
    }

  private def getSecurityDetails(
    securities: List[SecurityDetails],
    securitiesReclaims: Map[String, Map[TaxCode, BigDecimal]]
  ): Either[CdsError, List[SecurityDetail]] = {
    val securityDetails = securitiesReclaims.map { case (depositId, reclaims) =>
      securities.find(_.securityDepositId === depositId).flatMap { security =>
        getTaxDetails(security.taxDetails, reclaims).map(taxDetails =>
          SecurityDetail(
            depositId,
            totalAmount = security.totalAmount,
            amountPaid = security.amountPaid,
            paymentMethod = security.paymentMethod,
            paymentReference = security.paymentReference,
            taxDetails = taxDetails
          )
        )
      }
    }.toList

    if (securityDetails.contains(None))
      Left(
        CdsError(
          "[STRANGE] security reclaim for a deposit not present in the declaration, or reclaim for tax code not present in declaration"
        )
      )
    else
      Right(securityDetails.flatMap(_.toList))
  }

  private def getTaxDetails(
    depositTaxes: List[TaxDetails],
    claimItems: Map[TaxCode, BigDecimal]
  ): Option[List[TaxReclaimDetail]] = {
    val reclaims = claimItems.toList
      .sortBy { case (taxType, _) => taxType.value }
      .map { case (taxType, reclaimAmount) =>
        ((taxType, reclaimAmount), depositTaxes.find(_.taxType === taxType.value))
      }
      .map {
        case ((_, claimAmount), Some(TaxDetails(taxType, amount))) =>
          Some(TaxReclaimDetail(taxType, amount, claimAmount.toString()))
        case ((_, _), None)                                        => None
      }

    if (reclaims.contains(None))
      None
    else
      Some(reclaims.flatMap(_.toList))
  }
}
