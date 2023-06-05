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
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{MultipleOverpaymentsClaim, Street}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.{AcceptanceDate, ISOLocalDate}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.CaseType.{CMA, Individual}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant.{Importer, Representative}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReimbursementMethod.{BankTransfer, Deferment}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.YesNo.{No, Yes}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{ClaimType, CustomDeclarationType, DeclarationMode}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.OverpaymentsClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps

class OverpaymentsMultipleClaimMappingSpec
    extends AnyWordSpec
    with OverpaymentsClaimSupport
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with OptionValues
    with TypeCheckedTripleEquals {

  "The OverpaymentsMultiple claim mapper" should {

    "map a valid claim to TPI05 request" in forAll {
      multipleOverpaymentsData: (MultipleOverpaymentsClaim, List[DisplayDeclaration]) =>
        val claim        = multipleOverpaymentsData._1
        val declarations = multipleOverpaymentsData._2
        val tpi05Request = overpaymentsMultipleClaimToTPI05Mapper.map(multipleOverpaymentsData)

        val claimsOverMrns = claim.reimbursementClaims.flatMap { case (mrn, taxesClaimed) =>
          declarations
            .filter(_.displayResponseDetail.declarationId === mrn.value)
            .map(declaration => mrn -> ((taxesClaimed, declaration)))
        }

        inside(tpi05Request) {
          case Right(EisSubmitClaimRequest(PostNewClaimsRequest(common, details: RequestDetail))) =>
            common.originatingSystem should be(MDTP)

            details.claimantEORI should ===(claim.claimantInformation.eori)

            details should have(
              Symbol("CDFPayService")(NDRC),
              Symbol("dateReceived")(ISOLocalDate.now.some),
              Symbol("customDeclarationType")(CustomDeclarationType.MRN.some),
              Symbol("claimDate")(ISOLocalDate.now.some),
              Symbol("claimType")(ClaimType.C285.some),
              Symbol("claimant")(Some(if (claim.claimantType === Consignee) Importer else Representative)),
              Symbol("payeeIndicator")(Some(if (claim.claimantType === Consignee) Importer else Representative)),
              Symbol("declarationMode")(Some(DeclarationMode.ParentDeclaration)),
              Symbol("claimAmountTotal")(claim.totalReimbursementAmount.roundToTwoDecimalPlaces.toString.some),
              Symbol("reimbursementMethod")(
                Some(if (claim.reimbursementMethod === BankAccountTransfer) BankTransfer else Deferment)
              ),
              Symbol("basisOfClaim")(claim.basisOfClaim.toTPI05DisplayString.some),
              Symbol("caseType")(Some(if (claim.reimbursementMethod === CurrentMonthAdjustment) CMA else Individual)),
              Symbol("goodsDetails")(
                GoodsDetails(
                  descOfGoods = claim.additionalDetails.some,
                  isPrivateImporter = Some(if (claim.claimantType === Consignee) Yes else No)
                ).some
              ),
              Symbol("MRNDetails") {
                claimsOverMrns.map { case (mrn, (claimedReimbursements, declaration)) =>
                  MrnDetail(
                    MRNNumber = mrn.some,
                    acceptanceDate = AcceptanceDate
                      .fromDisplayFormat(declaration.displayResponseDetail.acceptanceDate)
                      .flatMap(_.toTpi05DateString)
                      .toOption,
                    declarantReferenceNumber = declaration.displayResponseDetail.declarantReferenceNumber,
                    mainDeclarationReference = (mrn === claim.leadMrn).some,
                    procedureCode = declaration.displayResponseDetail.procedureCode.some,
                    declarantDetails = {
                      val declarantDetails = declaration.displayResponseDetail.declarantDetails
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
                        contactDetails = Some(
                          ContactInformation(
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
                        )
                      ).some
                    },
                    consigneeDetails = {
                      val consigneeDetails   = declaration.displayResponseDetail.effectiveConsigneeDetails.value
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
                        contactDetails = Some(
                          ContactInformation(
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
                        )
                      ).some
                    },
                    accountDetails = declaration.displayResponseDetail.accountDetails.map(
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
                    bankDetails = claim.firstNonEmptyBankDetails(declaration.displayResponseDetail.bankDetails),
                    NDRCDetails = {
                      val ndrcDetails = declaration.displayResponseDetail.ndrcDetails.toList.flatten

                      claimedReimbursements
                        .map { case (taxCode, claimedAmount) =>
                          ndrcDetails
                            .find(_.taxType === taxCode.value)
                            .map(details =>
                              NdrcDetails(
                                paymentMethod = details.paymentMethod,
                                paymentReference = details.paymentReference,
                                CMAEligible = None,
                                taxType = taxCode,
                                amount = BigDecimal(details.amount).roundToTwoDecimalPlaces.toString(),
                                claimAmount = claimedAmount.roundToTwoDecimalPlaces.toString().some
                              )
                            )
                        }
                        .toList
                        .flatten(Option.option2Iterable)
                    }.some
                  )
                }.some
              }
            )
        }
    }
  }
}
