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

import cats.implicits.catsSyntaxOptionId
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.Inside.inside
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform.MDTP
import uk.gov.hmrc.cdsreimbursementclaim.models.CDFPayService.NDRC
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ClaimantType.Consignee
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ReimbursementMethodAnswer.{BankAccountTransfer, CurrentMonthAdjustment}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{Country, ScheduledOverpaymentsClaim, Street}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.{AcceptanceDate, ISOLocalDate}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.CaseType.{CMA, Individual}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant.{Importer, Representative}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReimbursementMethod.{BankTransfer, Deferment}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.YesNo.{No, Yes}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{ClaimType, CustomDeclarationType, DeclarationMode}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.OverpaymentsClaimGen.genOverpaymentsScheduledClaim
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps

class OverpaymentsScheduledClaimMappingSpec
    extends AnyWordSpec
    with C285ClaimSupport
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with OptionValues
    with TypeCheckedTripleEquals {

  "The OverpaymentsScheduled claim mapper" should {

    "map a valid claim to TPI05 request" in forAll(genOverpaymentsScheduledClaim) {
      scheduledOverpaymentsData: (ScheduledOverpaymentsClaim, DisplayDeclaration) =>
        val tpi05Request = overpaymentsScheduledClaimToTPI05Mapper map scheduledOverpaymentsData

        val (claim, displayDeclaration) = scheduledOverpaymentsData
        val declarant                   = displayDeclaration.displayResponseDetail.declarantDetails

        inside(tpi05Request) {
          case Right(EisSubmitClaimRequest(PostNewClaimsRequest(common, details: RequestDetail))) =>
            common.originatingSystem should be(MDTP)

            details.claimantEORI should ===(claim.claimantInformation.eori)

            details should have(
              'CDFPayService (NDRC),
              'dateReceived (ISOLocalDate.now.some),
              'customDeclarationType (CustomDeclarationType.MRN.some),
              'claimDate (ISOLocalDate.now.some),
              'claimType (ClaimType.C285.some),
              'claimant (Some(if (claim.claimantType === Consignee) Importer else Representative)),
              'payeeIndicator (Some(if (claim.claimantType === Consignee) Importer else Representative)),
              'declarationMode (Some(DeclarationMode.ParentDeclaration)),
              'claimAmountTotal (claim.totalReimbursementAmount.roundToTwoDecimalPlaces.toString.some),
              'reimbursementMethod (
                Some(if (claim.reimbursementMethod === BankAccountTransfer) BankTransfer else Deferment)
              ),
              'basisOfClaim (claim.basisOfClaim.toTPI05DisplayString.some),
              'caseType (Some(if (claim.reimbursementMethod === CurrentMonthAdjustment) CMA else Individual)),
              'goodsDetails (
                GoodsDetails(
                  descOfGoods = claim.additionalDetails.some,
                  isPrivateImporter = Some(if (claim.claimantType === Consignee) Yes else No)
                ).some
              ),
              'EORIDetails (
                EoriDetails(
                  agentEORIDetails = EORIInformation(
                    EORINumber = declarant.EORI,
                    CDSFullName = declarant.legalName,
                    CDSEstablishmentAddress = Address(
                      contactPerson = declarant.contactDetails.flatMap(_.contactName),
                      addressLine1 = declarant.contactDetails.flatMap(_.addressLine1),
                      addressLine2 = declarant.contactDetails.flatMap(_.addressLine2),
                      addressLine3 = declarant.contactDetails.flatMap(_.addressLine3),
                      street = Street.fromLines(
                        declarant.contactDetails.flatMap(_.addressLine1),
                        declarant.contactDetails.flatMap(_.addressLine2)
                      ),
                      city = declarant.contactDetails.flatMap(_.addressLine4),
                      postalCode = declarant.contactDetails.flatMap(_.postalCode),
                      countryCode = declarant.contactDetails.flatMap(_.countryCode).value,
                      emailAddress = declarant.contactDetails.flatMap(_.emailAddress),
                      telephoneNumber = None
                    ),
                    contactInformation = claim.claimantInformation.contactInformation.some
                  ),
                  importerEORIDetails = {
                    val maybeConsigneeDetails = displayDeclaration.displayResponseDetail.effectiveConsigneeDetails
                    val maybeContactDetails   = maybeConsigneeDetails.flatMap(_.contactDetails)

                    EORIInformation(
                      EORINumber = maybeConsigneeDetails.map(_.EORI).value,
                      CDSFullName = maybeConsigneeDetails.map(_.legalName).value,
                      CDSEstablishmentAddress = Address(
                        contactPerson = None,
                        addressLine1 = maybeConsigneeDetails.map(_.establishmentAddress.addressLine1),
                        addressLine2 = maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine2),
                        addressLine3 = maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine3),
                        street = Street.fromLines(
                          maybeConsigneeDetails.map(_.establishmentAddress.addressLine1),
                          maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine2)
                        ),
                        city = maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine3),
                        countryCode = maybeConsigneeDetails
                          .map(_.establishmentAddress.countryCode)
                          .getOrElse(Country.uk.code),
                        postalCode = maybeConsigneeDetails.flatMap(_.establishmentAddress.postalCode),
                        telephoneNumber = maybeContactDetails.flatMap(_.telephone),
                        emailAddress = maybeContactDetails.flatMap(_.emailAddress)
                      ),
                      contactInformation = ContactInformation(
                        contactPerson = maybeContactDetails.flatMap(_.contactName),
                        addressLine1 = maybeContactDetails.flatMap(_.addressLine1),
                        addressLine2 = maybeContactDetails.flatMap(_.addressLine2),
                        addressLine3 = maybeContactDetails.flatMap(_.addressLine3),
                        street = Street.fromLines(
                          maybeContactDetails.flatMap(_.addressLine1),
                          maybeContactDetails.flatMap(_.addressLine2)
                        ),
                        city = maybeContactDetails.flatMap(_.addressLine3),
                        countryCode = maybeContactDetails.flatMap(_.countryCode),
                        postalCode = maybeContactDetails.flatMap(_.postalCode),
                        telephoneNumber = maybeContactDetails.flatMap(_.telephone),
                        faxNumber = None,
                        emailAddress = maybeContactDetails.flatMap(_.emailAddress)
                      ).some
                    )
                  }
                ).some
              ),
              'MRNDetails {
                val mrn = MRN(displayDeclaration.displayResponseDetail.declarationId)
                Some(
                  MrnDetail(
                    MRNNumber = mrn.some,
                    acceptanceDate = AcceptanceDate
                      .fromDisplayFormat(displayDeclaration.displayResponseDetail.acceptanceDate)
                      .flatMap(_.toTpi05DateString)
                      .toOption,
                    declarantReferenceNumber = displayDeclaration.displayResponseDetail.declarantReferenceNumber,
                    mainDeclarationReference = (claim.movementReferenceNumber.value === mrn.value).some,
                    procedureCode = displayDeclaration.displayResponseDetail.procedureCode.some,
                    declarantDetails = {
                      val declarantDetails = displayDeclaration.displayResponseDetail.declarantDetails
                      val contactDetails   = declarantDetails.contactDetails.value

                      MRNInformation(
                        EORI = declarantDetails.EORI,
                        legalName = declarantDetails.legalName,
                        establishmentAddress = Address(
                          contactPerson = None,
                          addressLine1 = declarantDetails.establishmentAddress.addressLine1.some,
                          addressLine2 = declarantDetails.establishmentAddress.addressLine2,
                          addressLine3 = declarantDetails.establishmentAddress.addressLine3,
                          street = Street.fromLines(
                            declarantDetails.establishmentAddress.addressLine1.some,
                            declarantDetails.establishmentAddress.addressLine2
                          ),
                          city = declarantDetails.establishmentAddress.addressLine3,
                          countryCode = declarantDetails.establishmentAddress.countryCode,
                          postalCode = declarantDetails.establishmentAddress.postalCode,
                          telephoneNumber = None,
                          emailAddress = None
                        ),
                        contactDetails = ContactInformation(
                          contactPerson = contactDetails.contactName,
                          addressLine1 = contactDetails.addressLine1,
                          addressLine2 = contactDetails.addressLine2,
                          addressLine3 = contactDetails.addressLine3,
                          street = Street.fromLines(contactDetails.addressLine1, contactDetails.addressLine2),
                          city = contactDetails.addressLine3,
                          countryCode = contactDetails.countryCode,
                          postalCode = contactDetails.postalCode,
                          telephoneNumber = contactDetails.telephone,
                          faxNumber = None,
                          emailAddress = contactDetails.emailAddress
                        )
                      ).some
                    },
                    consigneeDetails = {
                      val consigneeDetails   = displayDeclaration.displayResponseDetail.effectiveConsigneeDetails.value
                      val contactInformation = consigneeDetails.contactDetails.value

                      MRNInformation(
                        EORI = consigneeDetails.EORI,
                        legalName = consigneeDetails.legalName,
                        establishmentAddress = Address(
                          contactPerson = None,
                          addressLine1 = consigneeDetails.establishmentAddress.addressLine1.some,
                          addressLine2 = consigneeDetails.establishmentAddress.addressLine2,
                          addressLine3 = consigneeDetails.establishmentAddress.addressLine3,
                          street = Street.fromLines(
                            consigneeDetails.establishmentAddress.addressLine1.some,
                            consigneeDetails.establishmentAddress.addressLine2
                          ),
                          city = consigneeDetails.establishmentAddress.addressLine3,
                          countryCode = consigneeDetails.establishmentAddress.countryCode,
                          postalCode = consigneeDetails.establishmentAddress.postalCode,
                          telephoneNumber = None,
                          emailAddress = None
                        ),
                        contactDetails = ContactInformation(
                          contactPerson = contactInformation.contactName,
                          addressLine1 = contactInformation.addressLine1,
                          addressLine2 = contactInformation.addressLine2,
                          addressLine3 = contactInformation.addressLine3,
                          street = Street.fromLines(contactInformation.addressLine1, contactInformation.addressLine2),
                          city = contactInformation.addressLine3,
                          countryCode = contactInformation.countryCode,
                          postalCode = contactInformation.postalCode,
                          telephoneNumber = contactInformation.telephone,
                          faxNumber = None,
                          emailAddress = contactInformation.emailAddress
                        )
                      ).some
                    },
                    accountDetails = displayDeclaration.displayResponseDetail.accountDetails.map(
                      _.map(accountDetail =>
                        AccountDetail(
                          accountType = accountDetail.accountType,
                          accountNumber = accountDetail.accountNumber,
                          EORI = accountDetail.eori,
                          legalName = accountDetail.legalName,
                          contactDetails = accountDetail.contactDetails.map { contactDetails =>
                            ContactInformation(
                              contactPerson = contactDetails.contactName,
                              addressLine1 = contactDetails.addressLine1,
                              addressLine2 = contactDetails.addressLine2,
                              addressLine3 = contactDetails.addressLine3,
                              street = contactDetails.addressLine4,
                              city = None,
                              countryCode = contactDetails.countryCode,
                              postalCode = contactDetails.postalCode,
                              telephoneNumber = contactDetails.telephone,
                              faxNumber = None,
                              emailAddress = contactDetails.emailAddress
                            )
                          }
                        )
                      )
                    ),
                    bankDetails = Option(claim.movementReferenceNumber.value === mrn.value)
                      .filter(_ === true)
                      .flatMap(_ =>
                        claim.bankAccountDetails
                          .map(bd => BankDetails(BankDetail.from(bd).some, BankDetail.from(bd).some))
                          .orElse(
                            displayDeclaration.displayResponseDetail.bankDetails.map(bd =>
                              BankDetails(
                                bd.consigneeBankDetails.map(BankDetail.from),
                                bd.declarantBankDetails.map(BankDetail.from)
                              )
                            )
                          )
                      ),
                    NDRCDetails = claim.getClaimedReimbursements.map { reimbursement =>
                      NdrcDetails(
                        paymentMethod = reimbursement.paymentMethod,
                        paymentReference = reimbursement.paymentReference,
                        CMAEligible = None,
                        taxType = reimbursement.taxCode,
                        amount = reimbursement.paidAmount.roundToTwoDecimalPlaces.toString(),
                        claimAmount = reimbursement.claimAmount.roundToTwoDecimalPlaces.toString().some
                      )
                    }.some
                  ) :: Nil
                )
              }
            )
        }
    }

  }
}
