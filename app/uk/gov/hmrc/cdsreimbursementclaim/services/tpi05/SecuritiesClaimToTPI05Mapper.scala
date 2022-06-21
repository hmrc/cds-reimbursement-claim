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

import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ClaimantType.{Consignee, Declarant}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.securities.{DeclarationId, ProcedureCode}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimantType, SecuritiesClaim, SecurityDetail, TaxCode, TaxDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.AcceptanceDate
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.{BankAccountDetails, ConsigneeDetails, DeclarantDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ClaimType.SECURITY
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error => CdsError}

import java.time.LocalDate

class SecuritiesClaimToTPI05Mapper extends ClaimToTPI05Mapper[SecuritiesClaim] {

  def map(request: SecuritiesClaim): Either[CdsError, EisSubmitClaimRequest] =
    (for {
      email            <- request.claimantInformation.contactInformation.emailAddress.toRight(
                            CdsError("claimant email address is mandatory")
                          )
      claimantName     <- request.claimantInformation.contactInformation.contactPerson.toRight(
                            CdsError("claimant contact name is mandatory")
                          )
      claimantEmail     = Email(email)
      claimedAmount     = request.securitiesReclaims.flatMap(_._2.map { case (_, value: BigDecimal) => value }).sum
      declarantDetails <- DeclarantDetails.fromClaimantInformation(request.claimantInformation)
      consigneeDetails <- ConsigneeDetails.fromClaimantInformation(request.claimantInformation)
    } yield (
      TPI05
        .request(
          claimantEORI = request.claimantInformation.eori,
          claimantEmailAddress = claimantEmail,
          claimantName = claimantName
        )
        .forClaimOfType(SECURITY)
        .withClaimedAmount(claimedAmount)
        .withClaimant(Claimant.basedOn(request.claimantType))
        .withSecurityInfo(
          None,
          request.reasonForSecurity,
          DeclarationId("todo"),
          ProcedureCode("todo"),
          AcceptanceDate(LocalDate.now()),
          declarantDetails,
          consigneeDetails,
          getBankDetails(request.claimantType, request.bankAccountDetails),
          getSecurityDetails(request.securitiesReclaims)
        )
    )).flatMap(_.verify)

  private def getBankDetails(claimantType: ClaimantType, bankAccountDetails: Option[BankAccountDetails]): BankDetails =
    claimantType match {
      case Consignee => BankDetails(bankAccountDetails.map(BankDetail.from), None)
      case Declarant => BankDetails(None, bankAccountDetails.map(BankDetail.from))
      case _         => BankDetails(None, None)
    }

  private def getSecurityDetails(securitiesReclaims: Map[String, Map[TaxCode, BigDecimal]]): List[SecurityDetail] =
    securitiesReclaims.map { a =>
      SecurityDetail(
        a._1,
        a._2.values.sum.toString,
        amountPaid = "todo",
        paymentMethod = "todo",
        paymentReference = "todo",
        getTaxDetails(a._2)
      )
    }.toList

  private def getTaxDetails(claimItems: Map[TaxCode, BigDecimal]): List[TaxDetail] =
    claimItems.map(a => TaxDetail(a._1.value, a._2.toString)).toList
}
