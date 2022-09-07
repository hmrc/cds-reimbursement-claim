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
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimantType, SecuritiesClaim, SecurityDetail, TaxCode, TaxDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.{AcceptanceDate, EisBasicDate}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.{BankAccountDetails, BtaSource, ConsigneeDetails, DeclarantDetails, SecurityDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.SECURITY
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error => CdsError}

import java.time.LocalDate

class SecuritiesClaimToTPI05Mapper extends ClaimToTPI05Mapper[(SecuritiesClaim, DisplayDeclaration)] {

  def map(request: (SecuritiesClaim, DisplayDeclaration)): Either[CdsError, EisSubmitClaimRequest] = {
    val (claim, displayDeclaration) = request
    (for {
      email                    <- claim.claimantInformation.contactInformation.emailAddress.toRight(
                                    CdsError("claimant email address is mandatory")
                                  )
      claimantName             <- claim.claimantInformation.contactInformation.contactPerson.toRight(
                                    CdsError("claimant contact name is mandatory")
                                  )
      claimantEmail             = Email(email)
      claimedAmount             = claim.securitiesReclaims.flatMap(_._2.map { case (_, value: BigDecimal) => value }).sum
      declarantDetails         <- DeclarantDetails.fromClaimantInformation(claim.claimantInformation)
      consigneeDetails         <- ConsigneeDetails.fromClaimantInformation(claim.claimantInformation)
      securities                = displayDeclaration.displayResponseDetail.securityDetails.toList.flatten
      securityDetails          <- getSecurityDetails(securities, claim.securitiesReclaims)
      acceptanceDate           <- AcceptanceDate
                                    .fromDisplayFormat(displayDeclaration.displayResponseDetail.acceptanceDate)
                                    .toEither
                                    .leftMap(x => CdsError(s"acceptance date could not be parsed: $x"))
      declarantReferenceNumber <- displayDeclaration.displayResponseDetail.declarantReferenceNumber.toRight(
                                    CdsError("declarant reference number is mandatory")
                                  )
      btaSource                <- displayDeclaration.displayResponseDetail.btaSource.toRight(
                                    CdsError("bta source is mandatory")
                                  )
      btaDueDate               <- displayDeclaration.displayResponseDetail.btaDueDate
                                    .map(LocalDate.parse)
                                    .toRight(
                                      CdsError("bta due date is mandatory")
                                    )
      accountDetails           <- displayDeclaration.displayResponseDetail.accountDetails.toRight(
                                    CdsError("account details are mandatory is mandatory")
                                  )
    } yield TPI05
      .request(
        claimantEORI = claim.claimantInformation.eori,
        claimantEmailAddress = claimantEmail
//          claimantName = claimantName
      )
      .forClaimOfType(SECURITY)
      .withClaimedAmount(claimedAmount)
      .withClaimant(Claimant.basedOn(claim.claimantType))
      .withSecurityInfo(
        Some(EisBasicDate(LocalDate.now)),
        claim.reasonForSecurity,
        DeclarationId(claim.movementReferenceNumber.value),
        ProcedureCode(displayDeclaration.displayResponseDetail.procedureCode),
        acceptanceDate,
        DeclarantReferenceNumber(declarantReferenceNumber),
        BtaSource(btaSource),
        EisBasicDate(btaDueDate),
        declarantDetails,
        consigneeDetails,
        accountDetails.map(acc =>
          AccountDetail(
            accountType = acc.accountType,
            accountNumber = acc.accountNumber,
            EORI = acc.eori,
            legalName = acc.legalName,
            contactDetails = acc.contactDetails.map(contact =>
              ContactInformation(
                contactPerson = contact.contactName, // is this mapping correct?
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
        ),
        getBankDetails(claim.claimantType, claim.bankAccountDetails),
        securityDetails
      )).flatMap(x => x.verify)
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
      securities.find(_.securityDepositId === depositId).map { security =>
        SecurityDetail(
          depositId,
          totalAmount = security.totalAmount,
          amountPaid = security.amountPaid,
          paymentMethod = security.paymentMethod,
          paymentReference = security.paymentReference,
          getTaxDetails(reclaims)
        )
      }
    }.toList

    if (securityDetails.contains(None))
      Left(CdsError("[STRANGE] security reclaim for a deposit not present in the declaration"))
    else
      Right(securityDetails.flatMap(_.toList))
  }

  private def getTaxDetails(claimItems: Map[TaxCode, BigDecimal]): List[TaxDetail] =
    claimItems.map { case (taxType, amount) => TaxDetail(taxType.value, amount.toString) }.toList
}
