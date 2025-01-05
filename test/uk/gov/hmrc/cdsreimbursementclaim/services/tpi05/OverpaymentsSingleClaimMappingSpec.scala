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
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ClaimantType
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ReimbursementMethodAnswer.{BankAccountTransfer, CurrentMonthAdjustment, Subsidy}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{Country, PayeeType, SingleOverpaymentsClaim, Street}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.{AcceptanceDate, ISOLocalDate}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.CDFPayService.NDRC
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.CaseType.{CMA, Individual}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant.{Importer, Representative}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.ReimbursementMethod
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.YesNo.{No, Yes}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{ClaimType, CustomDeclarationType, DeclarationMode}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.OverpaymentsClaimGen.genOverpaymentsSingleClaim
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.{BigDecimalOps, WAFRules}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.Reimbursement

class OverpaymentsSingleClaimMappingSpec
    extends AnyWordSpec
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with OptionValues
    with TypeCheckedTripleEquals {

  val mapper = new OverpaymentsSingleClaimToTPI05Mapper(false)

  "The OverpaymentsSingle claim mapper" should {

    "map a valid Declarant claim to TPI05 request" in forAll(genOverpaymentsSingleClaim(ClaimantType.Declarant)) {
      (singleOverpaymentsData: (SingleOverpaymentsClaim, DisplayDeclaration, Option[DisplayDeclaration])) =>
        val tpi05Request = mapper `map` singleOverpaymentsData

        val (claim, displayDeclaration, duplicateDeclaration) = singleOverpaymentsData

        val nrdcDetailsMap = displayDeclaration.displayResponseDetail.ndrcDetails.toList.flatten
          .groupBy(_.taxType)
          .view
          .mapValues(_.minByOption(_.taxType).value)
          .mapValues(ndrc => ndrc.copy(amount = BigDecimal(ndrc.amount).roundToTwoDecimalPlaces.toString()))

        inside(tpi05Request) { case Right(EisSubmitClaimRequest(PostNewClaimsRequest(common, details))) =>
          common.originatingSystem should be(MDTP)

          details.claimantEORI should ===(claim.claimantInformation.eori)
//          details.claimantEmailAddress should ===(claim.claimantInformation.)

          details should have(
            Symbol("CDFPayService")(NDRC),
            Symbol("newEORI")(claim.newEoriAndDan.map(_.eori)),
            Symbol("newDAN")(claim.newEoriAndDan.map(_.dan)),
            Symbol("dateReceived")(ISOLocalDate.now.some),
            Symbol("customDeclarationType")(CustomDeclarationType.MRN.some),
            Symbol("claimDate")(ISOLocalDate.now.some),
            Symbol("claimType")(ClaimType.C285.some),
            Symbol("claimant")(Some(if (claim.claimantType === ClaimantType.Consignee) Importer else Representative)),
            Symbol("payeeIndicator")(Some(if (claim.payeeType === PayeeType.Consignee) Importer else Representative)),
            Symbol("declarationMode")(Some(DeclarationMode.ParentDeclaration)),
            Symbol("claimAmountTotal")(claim.reimbursements.map(_.amount).sum.roundToTwoDecimalPlaces.toString.some),
            Symbol("reimbursementMethod")(
              Some(
                if (claim.reimbursementMethod === Subsidy) ReimbursementMethod.Subsidy
                else if (claim.reimbursementMethod === BankAccountTransfer) ReimbursementMethod.BankTransfer
                else ReimbursementMethod.Deferment
              )
            ),
            Symbol("basisOfClaim")(claim.basisOfClaim.toTPI05DisplayString.some),
            Symbol("caseType")(Some(if (claim.reimbursementMethod === CurrentMonthAdjustment) CMA else Individual)),
            Symbol("goodsDetails")(
              claim.newEoriAndDan match {
                case None                =>
                  GoodsDetails(
                    descOfGoods = claim.additionalDetails.some.map(WAFRules.asSafeText),
                    isPrivateImporter = Some(if (claim.claimantType === ClaimantType.Consignee) Yes else No)
                  ).some
                case Some(newEoriAndDan) =>
                  GoodsDetails(
                    descOfGoods = (newEoriAndDan.asAdditionalDetailsText ++ claim.additionalDetails).some
                      .map(WAFRules.asSafeText)
                      .map(_.take(500)),
                    isPrivateImporter = Some(if (claim.claimantType === ClaimantType.Consignee) Yes else No)
                  ).some
              }
            ),
            Symbol("EORIDetails")(
              EoriDetails(
                agentEORIDetails = EORIInformation(
                  EORINumber = claim.claimantInformation.eori,
                  CDSFullName = claim.claimantInformation.fullName,
                  CDSEstablishmentAddress = Address(
                    contactPerson = claim.claimantInformation.establishmentAddress.contactPerson,
                    addressLine1 = claim.claimantInformation.establishmentAddress.addressLine1,
                    addressLine2 = claim.claimantInformation.establishmentAddress.addressLine2,
                    addressLine3 = claim.claimantInformation.establishmentAddress.addressLine3,
                    street = claim.claimantInformation.establishmentAddress.street,
                    city = claim.claimantInformation.establishmentAddress.city,
                    countryCode = claim.claimantInformation.establishmentAddress.countryCode.getOrElse(Country.uk.code),
                    postalCode = claim.claimantInformation.establishmentAddress.postalCode,
                    telephoneNumber = claim.claimantInformation.establishmentAddress.telephoneNumber,
                    emailAddress = claim.claimantInformation.establishmentAddress.emailAddress
                  ),
                  contactInformation = claim.claimantInformation.contactInformation.some
                ),
                importerEORIDetails = {
                  val maybeConsigneeDetails = Some(displayDeclaration.displayResponseDetail.effectiveConsigneeDetails)
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
            Symbol("MRNDetails") {
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
                    val consigneeDetails   = displayDeclaration.displayResponseDetail.effectiveConsigneeDetails
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
                  NDRCDetails = claim.reimbursements.toList.map { case Reimbursement(taxCode, reclaimAmount, _) =>
                    NdrcDetails(
                      paymentMethod = nrdcDetailsMap.get(taxCode.value).value.paymentMethod,
                      paymentReference = nrdcDetailsMap.get(taxCode.value).value.paymentReference,
                      CMAEligible = None,
                      taxType = taxCode,
                      amount = nrdcDetailsMap.get(taxCode.value).value.amount,
                      claimAmount = reclaimAmount.roundToTwoDecimalPlaces.toString().some,
                      None
                    )
                  }.some
                ) :: Nil
              )
            },
            Symbol("duplicateMRNDetails")(
              duplicateDeclaration
                .map(_.displayResponseDetail)
                .map(details =>
                  MrnDetail(
                    MRNNumber = MRN(details.declarationId).some,
                    acceptanceDate = AcceptanceDate
                      .fromDisplayFormat(details.acceptanceDate)
                      .flatMap(_.toTpi05DateString)
                      .toOption,
                    declarantReferenceNumber = details.declarantReferenceNumber,
                    mainDeclarationReference = true.some,
                    procedureCode = details.procedureCode.some,
                    declarantDetails = {
                      val declarantDetails = details.declarantDetails
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
                      val consigneeDetails   = details.effectiveConsigneeDetails
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
                    bankDetails = claim.bankAccountDetails
                      .map(bd => BankDetails(BankDetail.from(bd).some, BankDetail.from(bd).some))
                      .orElse(
                        displayDeclaration.displayResponseDetail.bankDetails.map(bd =>
                          BankDetails(
                            bd.consigneeBankDetails.map(BankDetail.from),
                            bd.declarantBankDetails.map(BankDetail.from)
                          )
                        )
                      ),
                    NDRCDetails = claim.reimbursements.toList.map { case Reimbursement(taxCode, reclaimAmount, _) =>
                      NdrcDetails(
                        paymentMethod = nrdcDetailsMap.get(taxCode.value).value.paymentMethod,
                        paymentReference = nrdcDetailsMap.get(taxCode.value).value.paymentReference,
                        CMAEligible = None,
                        taxType = taxCode,
                        amount = nrdcDetailsMap.get(taxCode.value).value.amount,
                        claimAmount = reclaimAmount.roundToTwoDecimalPlaces.toString().some,
                        None
                      )
                    }.some
                  )
                )
            )
          )
        }
    }

    "map a valid Consignee claim to TPI05 request" in forAll(genOverpaymentsSingleClaim(ClaimantType.Consignee)) {
      (singleOverpaymentsData: (SingleOverpaymentsClaim, DisplayDeclaration, Option[DisplayDeclaration])) =>
        val tpi05Request = mapper `map` singleOverpaymentsData

        val (claim, displayDeclaration, duplicateDeclaration) = singleOverpaymentsData

        val nrdcDetailsMap = displayDeclaration.displayResponseDetail.ndrcDetails.toList.flatten
          .groupBy(_.taxType)
          .view
          .mapValues(_.minByOption(_.taxType).value)
          .mapValues(ndrc => ndrc.copy(amount = BigDecimal(ndrc.amount).roundToTwoDecimalPlaces.toString()))

        inside(tpi05Request) { case Right(EisSubmitClaimRequest(PostNewClaimsRequest(common, details))) =>
          common.originatingSystem should be(MDTP)

          details.claimantEORI should ===(claim.claimantInformation.eori)
//          details.claimantEmailAddress should ===(claim.claimantInformation.)

          details should have(
            Symbol("CDFPayService")(NDRC),
            Symbol("newEORI")(claim.newEoriAndDan.map(_.eori)),
            Symbol("newDAN")(claim.newEoriAndDan.map(_.dan)),
            Symbol("dateReceived")(ISOLocalDate.now.some),
            Symbol("customDeclarationType")(CustomDeclarationType.MRN.some),
            Symbol("claimDate")(ISOLocalDate.now.some),
            Symbol("claimType")(ClaimType.C285.some),
            Symbol("claimant")(Some(if (claim.claimantType === ClaimantType.Consignee) Importer else Representative)),
            Symbol("payeeIndicator")(Some(if (claim.payeeType === PayeeType.Consignee) Importer else Representative)),
            Symbol("declarationMode")(Some(DeclarationMode.ParentDeclaration)),
            Symbol("claimAmountTotal")(claim.reimbursements.map(_.amount).sum.roundToTwoDecimalPlaces.toString.some),
            Symbol("reimbursementMethod")(
              Some(
                if (claim.reimbursementMethod === Subsidy) ReimbursementMethod.Subsidy
                else if (claim.reimbursementMethod === BankAccountTransfer) ReimbursementMethod.BankTransfer
                else ReimbursementMethod.Deferment
              )
            ),
            Symbol("basisOfClaim")(claim.basisOfClaim.toTPI05DisplayString.some),
            Symbol("caseType")(Some(if (claim.reimbursementMethod === CurrentMonthAdjustment) CMA else Individual)),
            Symbol("goodsDetails")(
              claim.newEoriAndDan match {
                case None                =>
                  GoodsDetails(
                    descOfGoods = claim.additionalDetails.some.map(WAFRules.asSafeText),
                    isPrivateImporter = Some(if (claim.claimantType === ClaimantType.Consignee) Yes else No)
                  ).some
                case Some(newEoriAndDan) =>
                  GoodsDetails(
                    descOfGoods = (newEoriAndDan.asAdditionalDetailsText ++ claim.additionalDetails).some
                      .map(WAFRules.asSafeText)
                      .map(_.take(500)),
                    isPrivateImporter = Some(if (claim.claimantType === ClaimantType.Consignee) Yes else No)
                  ).some
              }
            ),
            Symbol("EORIDetails")(
              EoriDetails(
                importerEORIDetails = EORIInformation(
                  EORINumber = claim.claimantInformation.eori,
                  CDSFullName = claim.claimantInformation.fullName,
                  CDSEstablishmentAddress = Address(
                    contactPerson = claim.claimantInformation.establishmentAddress.contactPerson,
                    addressLine1 = claim.claimantInformation.establishmentAddress.addressLine1,
                    addressLine2 = claim.claimantInformation.establishmentAddress.addressLine2,
                    addressLine3 = claim.claimantInformation.establishmentAddress.addressLine3,
                    street = claim.claimantInformation.establishmentAddress.street,
                    city = claim.claimantInformation.establishmentAddress.city,
                    countryCode = claim.claimantInformation.establishmentAddress.countryCode.getOrElse(Country.uk.code),
                    postalCode = claim.claimantInformation.establishmentAddress.postalCode,
                    telephoneNumber = claim.claimantInformation.establishmentAddress.telephoneNumber,
                    emailAddress = claim.claimantInformation.establishmentAddress.emailAddress
                  ),
                  contactInformation = claim.claimantInformation.contactInformation.some
                ),
                agentEORIDetails = {
                  val declarantDetails    = displayDeclaration.displayResponseDetail.declarantDetails
                  val maybeContactDetails = declarantDetails.contactDetails

                  val maybeTelephone    = maybeContactDetails.flatMap(_.telephone)
                  val maybeEmailAddress = maybeContactDetails.flatMap(_.emailAddress)

                  val establishmentAddressLine1      = declarantDetails.establishmentAddress.addressLine1
                  val maybeEstablishmentAddressLine2 = declarantDetails.establishmentAddress.addressLine2
                  val maybeEstablishmentAddressLine3 = declarantDetails.establishmentAddress.addressLine3

                  EORIInformation(
                    EORINumber = declarantDetails.EORI,
                    CDSFullName = declarantDetails.legalName,
                    CDSEstablishmentAddress = Address(
                      contactPerson = None,
                      addressLine1 = Street.line1(Some(establishmentAddressLine1), maybeEstablishmentAddressLine2),
                      addressLine2 = Street.line2(Some(establishmentAddressLine1), maybeEstablishmentAddressLine2),
                      addressLine3 = maybeEstablishmentAddressLine3,
                      street = Street.fromLines(Some(establishmentAddressLine1), maybeEstablishmentAddressLine2),
                      city = maybeEstablishmentAddressLine3,
                      countryCode = declarantDetails.establishmentAddress.countryCode,
                      postalCode = declarantDetails.establishmentAddress.postalCode,
                      telephoneNumber = maybeTelephone,
                      emailAddress = maybeEmailAddress
                    ),
                    contactInformation = declarantDetails.contactDetails.map { contactDetails =>
                      val maybeAddress1 = contactDetails.addressLine1
                      val maybeAddress2 = contactDetails.addressLine2
                      val maybeAddress3 = contactDetails.addressLine3

                      ContactInformation(
                        contactPerson = contactDetails.contactName,
                        addressLine1 = Street.line1(maybeAddress1, maybeAddress2),
                        addressLine2 = Street.line2(maybeAddress1, maybeAddress2),
                        addressLine3 = maybeAddress3,
                        street = Street.fromLines(maybeAddress1, maybeAddress2),
                        city = maybeAddress3,
                        countryCode = contactDetails.countryCode,
                        postalCode = contactDetails.postalCode,
                        telephoneNumber = maybeTelephone,
                        faxNumber = None,
                        emailAddress = maybeEmailAddress
                      )
                    }
                  )
                }
              ).some
            ),
            Symbol("MRNDetails") {
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
                    val consigneeDetails   = displayDeclaration.displayResponseDetail.effectiveConsigneeDetails
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
                  NDRCDetails = claim.reimbursements.toList.map { case Reimbursement(taxCode, reclaimAmount, _) =>
                    NdrcDetails(
                      paymentMethod = nrdcDetailsMap.get(taxCode.value).value.paymentMethod,
                      paymentReference = nrdcDetailsMap.get(taxCode.value).value.paymentReference,
                      CMAEligible = None,
                      taxType = taxCode,
                      amount = nrdcDetailsMap.get(taxCode.value).value.amount,
                      claimAmount = reclaimAmount.roundToTwoDecimalPlaces.toString().some,
                      None
                    )
                  }.some
                ) :: Nil
              )
            },
            Symbol("duplicateMRNDetails")(
              duplicateDeclaration
                .map(_.displayResponseDetail)
                .map(details =>
                  MrnDetail(
                    MRNNumber = MRN(details.declarationId).some,
                    acceptanceDate = AcceptanceDate
                      .fromDisplayFormat(details.acceptanceDate)
                      .flatMap(_.toTpi05DateString)
                      .toOption,
                    declarantReferenceNumber = details.declarantReferenceNumber,
                    mainDeclarationReference = true.some,
                    procedureCode = details.procedureCode.some,
                    declarantDetails = {
                      val declarantDetails = details.declarantDetails
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
                      val consigneeDetails   = details.effectiveConsigneeDetails
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
                    bankDetails = claim.bankAccountDetails
                      .map(bd => BankDetails(BankDetail.from(bd).some, BankDetail.from(bd).some))
                      .orElse(
                        displayDeclaration.displayResponseDetail.bankDetails.map(bd =>
                          BankDetails(
                            bd.consigneeBankDetails.map(BankDetail.from),
                            bd.declarantBankDetails.map(BankDetail.from)
                          )
                        )
                      ),
                    NDRCDetails = claim.reimbursements.toList.map { case Reimbursement(taxCode, reclaimAmount, _) =>
                      NdrcDetails(
                        paymentMethod = nrdcDetailsMap.get(taxCode.value).value.paymentMethod,
                        paymentReference = nrdcDetailsMap.get(taxCode.value).value.paymentReference,
                        CMAEligible = None,
                        taxType = taxCode,
                        amount = nrdcDetailsMap.get(taxCode.value).value.amount,
                        claimAmount = reclaimAmount.roundToTwoDecimalPlaces.toString().some,
                        None
                      )
                    }.some
                  )
                )
            )
          )
        }
    }

    "map a valid third-party User claim to TPI05 request" in forAll(
      genOverpaymentsSingleClaim(ClaimantType.Consignee)
    ) { (singleOverpaymentsData: (SingleOverpaymentsClaim, DisplayDeclaration, Option[DisplayDeclaration])) =>
      val tpi05Request = mapper `map` singleOverpaymentsData

      val (claim, displayDeclaration, duplicateDeclaration) = singleOverpaymentsData

      val nrdcDetailsMap = displayDeclaration.displayResponseDetail.ndrcDetails.toList.flatten
        .groupBy(_.taxType)
        .view
        .mapValues(_.minByOption(_.taxType).value)
        .mapValues(ndrc => ndrc.copy(amount = BigDecimal(ndrc.amount).roundToTwoDecimalPlaces.toString()))

      inside(tpi05Request) { case Right(EisSubmitClaimRequest(PostNewClaimsRequest(common, details))) =>
        common.originatingSystem should be(MDTP)

        details.claimantEORI should ===(claim.claimantInformation.eori)
//          details.claimantEmailAddress should ===(claim.claimantInformation.)

        details should have(
          Symbol("CDFPayService")(NDRC),
          Symbol("newEORI")(claim.newEoriAndDan.map(_.eori)),
          Symbol("newDAN")(claim.newEoriAndDan.map(_.dan)),
          Symbol("dateReceived")(ISOLocalDate.now.some),
          Symbol("customDeclarationType")(CustomDeclarationType.MRN.some),
          Symbol("claimDate")(ISOLocalDate.now.some),
          Symbol("claimType")(ClaimType.C285.some),
          Symbol("claimant")(Some(if (claim.claimantType === ClaimantType.Consignee) Importer else Representative)),
          Symbol("payeeIndicator")(Some(if (claim.payeeType === PayeeType.Consignee) Importer else Representative)),
          Symbol("declarationMode")(Some(DeclarationMode.ParentDeclaration)),
          Symbol("claimAmountTotal")(claim.reimbursements.map(_.amount).sum.roundToTwoDecimalPlaces.toString.some),
          Symbol("reimbursementMethod")(
            Some(
              if (claim.reimbursementMethod === Subsidy) ReimbursementMethod.Subsidy
              else if (claim.reimbursementMethod === BankAccountTransfer) ReimbursementMethod.BankTransfer
              else ReimbursementMethod.Deferment
            )
          ),
          Symbol("basisOfClaim")(claim.basisOfClaim.toTPI05DisplayString.some),
          Symbol("caseType")(Some(if (claim.reimbursementMethod === CurrentMonthAdjustment) CMA else Individual)),
          Symbol("goodsDetails")(
            claim.newEoriAndDan match {
              case None                =>
                GoodsDetails(
                  descOfGoods = claim.additionalDetails.some.map(WAFRules.asSafeText),
                  isPrivateImporter = Some(if (claim.claimantType === ClaimantType.Consignee) Yes else No)
                ).some
              case Some(newEoriAndDan) =>
                GoodsDetails(
                  descOfGoods = (newEoriAndDan.asAdditionalDetailsText ++ claim.additionalDetails).some
                    .map(WAFRules.asSafeText)
                    .map(_.take(500)),
                  isPrivateImporter = Some(if (claim.claimantType === ClaimantType.Consignee) Yes else No)
                ).some
            }
          ),
          Symbol("EORIDetails")(
            EoriDetails(
              importerEORIDetails = EORIInformation(
                EORINumber = claim.claimantInformation.eori,
                CDSFullName = claim.claimantInformation.fullName,
                CDSEstablishmentAddress = Address(
                  contactPerson = claim.claimantInformation.establishmentAddress.contactPerson,
                  addressLine1 = claim.claimantInformation.establishmentAddress.addressLine1,
                  addressLine2 = claim.claimantInformation.establishmentAddress.addressLine2,
                  addressLine3 = claim.claimantInformation.establishmentAddress.addressLine3,
                  street = claim.claimantInformation.establishmentAddress.street,
                  city = claim.claimantInformation.establishmentAddress.city,
                  countryCode = claim.claimantInformation.establishmentAddress.countryCode.getOrElse(Country.uk.code),
                  postalCode = claim.claimantInformation.establishmentAddress.postalCode,
                  telephoneNumber = claim.claimantInformation.establishmentAddress.telephoneNumber,
                  emailAddress = claim.claimantInformation.establishmentAddress.emailAddress
                ),
                contactInformation = claim.claimantInformation.contactInformation.some
              ),
              agentEORIDetails = {
                val declarantDetails    = displayDeclaration.displayResponseDetail.declarantDetails
                val maybeContactDetails = declarantDetails.contactDetails

                val maybeTelephone    = maybeContactDetails.flatMap(_.telephone)
                val maybeEmailAddress = maybeContactDetails.flatMap(_.emailAddress)

                val establishmentAddressLine1      = declarantDetails.establishmentAddress.addressLine1
                val maybeEstablishmentAddressLine2 = declarantDetails.establishmentAddress.addressLine2
                val maybeEstablishmentAddressLine3 = declarantDetails.establishmentAddress.addressLine3

                EORIInformation(
                  EORINumber = declarantDetails.EORI,
                  CDSFullName = declarantDetails.legalName,
                  CDSEstablishmentAddress = Address(
                    contactPerson = None,
                    addressLine1 = Street.line1(Some(establishmentAddressLine1), maybeEstablishmentAddressLine2),
                    addressLine2 = Street.line2(Some(establishmentAddressLine1), maybeEstablishmentAddressLine2),
                    addressLine3 = maybeEstablishmentAddressLine3,
                    street = Street.fromLines(Some(establishmentAddressLine1), maybeEstablishmentAddressLine2),
                    city = maybeEstablishmentAddressLine3,
                    countryCode = declarantDetails.establishmentAddress.countryCode,
                    postalCode = declarantDetails.establishmentAddress.postalCode,
                    telephoneNumber = maybeTelephone,
                    emailAddress = maybeEmailAddress
                  ),
                  contactInformation = declarantDetails.contactDetails.map { contactDetails =>
                    val maybeAddress1 = contactDetails.addressLine1
                    val maybeAddress2 = contactDetails.addressLine2
                    val maybeAddress3 = contactDetails.addressLine3

                    ContactInformation(
                      contactPerson = contactDetails.contactName,
                      addressLine1 = Street.line1(maybeAddress1, maybeAddress2),
                      addressLine2 = Street.line2(maybeAddress1, maybeAddress2),
                      addressLine3 = maybeAddress3,
                      street = Street.fromLines(maybeAddress1, maybeAddress2),
                      city = maybeAddress3,
                      countryCode = contactDetails.countryCode,
                      postalCode = contactDetails.postalCode,
                      telephoneNumber = maybeTelephone,
                      faxNumber = None,
                      emailAddress = maybeEmailAddress
                    )
                  }
                )
              }
            ).some
          ),
          Symbol("MRNDetails") {
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
                  val consigneeDetails   = displayDeclaration.displayResponseDetail.effectiveConsigneeDetails
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
                NDRCDetails = claim.reimbursements.toList.map { case Reimbursement(taxCode, reclaimAmount, _) =>
                  NdrcDetails(
                    paymentMethod = nrdcDetailsMap.get(taxCode.value).value.paymentMethod,
                    paymentReference = nrdcDetailsMap.get(taxCode.value).value.paymentReference,
                    CMAEligible = None,
                    taxType = taxCode,
                    amount = nrdcDetailsMap.get(taxCode.value).value.amount,
                    claimAmount = reclaimAmount.roundToTwoDecimalPlaces.toString().some,
                    None
                  )
                }.some
              ) :: Nil
            )
          },
          Symbol("duplicateMRNDetails")(
            duplicateDeclaration
              .map(_.displayResponseDetail)
              .map(details =>
                MrnDetail(
                  MRNNumber = MRN(details.declarationId).some,
                  acceptanceDate = AcceptanceDate
                    .fromDisplayFormat(details.acceptanceDate)
                    .flatMap(_.toTpi05DateString)
                    .toOption,
                  declarantReferenceNumber = details.declarantReferenceNumber,
                  mainDeclarationReference = true.some,
                  procedureCode = details.procedureCode.some,
                  declarantDetails = {
                    val declarantDetails = details.declarantDetails
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
                    val consigneeDetails   = details.effectiveConsigneeDetails
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
                  bankDetails = claim.bankAccountDetails
                    .map(bd => BankDetails(BankDetail.from(bd).some, BankDetail.from(bd).some))
                    .orElse(
                      displayDeclaration.displayResponseDetail.bankDetails.map(bd =>
                        BankDetails(
                          bd.consigneeBankDetails.map(BankDetail.from),
                          bd.declarantBankDetails.map(BankDetail.from)
                        )
                      )
                    ),
                  NDRCDetails = claim.reimbursements.toList.map { case Reimbursement(taxCode, reclaimAmount, _) =>
                    NdrcDetails(
                      paymentMethod = nrdcDetailsMap.get(taxCode.value).value.paymentMethod,
                      paymentReference = nrdcDetailsMap.get(taxCode.value).value.paymentReference,
                      CMAEligible = None,
                      taxType = taxCode,
                      amount = nrdcDetailsMap.get(taxCode.value).value.amount,
                      claimAmount = reclaimAmount.roundToTwoDecimalPlaces.toString().some,
                      None
                    )
                  }.some
                )
              )
          )
        )
      }
    }

  }
}
