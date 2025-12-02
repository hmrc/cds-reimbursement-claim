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
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ReimbursementMethodAnswer.{BankAccountTransfer, CurrentMonthAdjustment, Subsidy}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{BasisOfClaim, ClaimantType, Country, PayeeType, Reimbursement, ReimbursementMethodAnswer, SingleOverpaymentsClaim, Street, TaxCode}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.{AcceptanceDate, ISOLocalDate}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.*
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.CaseType.{CMA, Individual}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant.{Importer, Representative}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.YesNo.{No, Yes}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{ClaimType, CustomDeclarationType, DeclarationMode, ReimbursementMethod}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.ImportDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.OverpaymentsClaimGen.{genOverpaymentsSingleClaim, genOverpaymentsSingleClaimAllTypes}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.NdrcDetails as ResponseNdrcDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.CMAEligibleGen

import java.util.UUID

class OverpaymentsSingleClaimMappingV2Spec
    extends AnyWordSpec
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with OptionValues
    with TypeCheckedTripleEquals {

  val mapper = new OverpaymentsSingleClaimToTPI05Mapper(true)

  "The OverpaymentsSingle claim mapper" should {

    "map a valid Declarant claim to TPI05 request" in forAll(genOverpaymentsSingleClaim(ClaimantType.Declarant)) {
      (singleOverpaymentsData: (SingleOverpaymentsClaim, ImportDeclaration, Option[ImportDeclaration])) =>
        val tpi05Request = mapper `map` singleOverpaymentsData

        val (claim, declaration, duplicateDeclaration) = singleOverpaymentsData

        val nrdcDetailsMap = declaration.displayResponseDetail.ndrcDetails.toList.flatten
          .groupBy(_.taxType)
          .view
          .mapValues(_.minByOption(_.taxType).value)
          .mapValues(ndrc => ndrc.copy(amount = BigDecimal(ndrc.amount).roundToTwoDecimalPlaces.toString()))

        inside(tpi05Request) { case Right(EisSubmitClaimRequest(PostNewClaimsRequest(common, details))) =>
          common.originatingSystem should be(MDTP)

          details.claimantEORI should ===(claim.claimantInformation.eori)

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
            Symbol("reimbursementMethod")(None),
            Symbol("basisOfClaim")(claim.basisOfClaim.toTPI05DisplayString.some),
            Symbol("caseType")(Some(if (claim.reimbursementMethod === CurrentMonthAdjustment) CMA else Individual)),
            Symbol("goodsDetails")(
              claim.newEoriAndDan match {
                case None                =>
                  GoodsDetails(
                    descOfGoods = claim.additionalDetails.some.map(_.take(500)),
                    isPrivateImporter = Some(if (claim.claimantType === ClaimantType.Consignee) Yes else No)
                  ).some
                case Some(newEoriAndDan) =>
                  GoodsDetails(
                    descOfGoods = (newEoriAndDan.asAdditionalDetailsText ++ claim.additionalDetails).some
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
                    street = claim.claimantInformation.establishmentAddress.street.map(_.take(70)),
                    city = claim.claimantInformation.establishmentAddress.city,
                    countryCode = claim.claimantInformation.establishmentAddress.countryCode.getOrElse(Country.uk.code),
                    postalCode = claim.claimantInformation.establishmentAddress.postalCode,
                    telephoneNumber = claim.claimantInformation.establishmentAddress.telephoneNumber,
                    emailAddress = claim.claimantInformation.establishmentAddress.emailAddress
                  ),
                  contactInformation = claim.claimantInformation.contactInformation.some
                ),
                importerEORIDetails = {
                  val maybeConsigneeDetails = Some(declaration.displayResponseDetail.effectiveConsigneeDetails)
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
              val mrn = MRN(declaration.displayResponseDetail.declarationId)
              Some(
                MrnDetail(
                  MRNNumber = mrn.some,
                  acceptanceDate = AcceptanceDate
                    .fromDisplayFormat(declaration.displayResponseDetail.acceptanceDate)
                    .flatMap(_.toTpi05DateString)
                    .toOption,
                  declarantReferenceNumber = declaration.displayResponseDetail.declarantReferenceNumber,
                  mainDeclarationReference = (claim.movementReferenceNumber.value === mrn.value).some,
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
                    val consigneeDetails   = declaration.displayResponseDetail.effectiveConsigneeDetails
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
                  bankDetails = Option(claim.movementReferenceNumber.value === mrn.value)
                    .filter(_ === true)
                    .flatMap(_ =>
                      claim.bankAccountDetails
                        .map(bd => BankDetails(BankDetail.from(bd).some, BankDetail.from(bd).some))
                        .orElse(
                          declaration.displayResponseDetail.bankDetails.map(bd =>
                            BankDetails(
                              bd.consigneeBankDetails.map(BankDetail.from),
                              bd.declarantBankDetails.map(BankDetail.from)
                            )
                          )
                        )
                    ),
                  NDRCDetails =
                    claim.reimbursements.toList.map { case Reimbursement(taxCode, reclaimAmount, reimbursementMethod) =>
                      NdrcDetails(
                        paymentMethod = nrdcDetailsMap.get(taxCode.value).value.paymentMethod,
                        paymentReference = nrdcDetailsMap.get(taxCode.value).value.paymentReference,
                        CMAEligible = nrdcDetailsMap.get(taxCode.value).value.cmaEligible,
                        taxType = taxCode,
                        amount = nrdcDetailsMap.get(taxCode.value).value.amount,
                        claimAmount = reclaimAmount.roundToTwoDecimalPlaces.toString().some,
                        Some(
                          if (reimbursementMethod === Subsidy) ReimbursementMethod.Subsidy
                          else if (reimbursementMethod === BankAccountTransfer) ReimbursementMethod.BankTransfer
                          else ReimbursementMethod.Deferment
                        )
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
                        declaration.displayResponseDetail.bankDetails.map(bd =>
                          BankDetails(
                            bd.consigneeBankDetails.map(BankDetail.from),
                            bd.declarantBankDetails.map(BankDetail.from)
                          )
                        )
                      ),
                    NDRCDetails = claim.reimbursements.toList.map {
                      case Reimbursement(taxCode, reclaimAmount, reimbursementMethod) =>
                        NdrcDetails(
                          paymentMethod = nrdcDetailsMap.get(taxCode.value).value.paymentMethod,
                          paymentReference = nrdcDetailsMap.get(taxCode.value).value.paymentReference,
                          CMAEligible = nrdcDetailsMap.get(taxCode.value).value.cmaEligible,
                          taxType = taxCode,
                          amount = nrdcDetailsMap.get(taxCode.value).value.amount,
                          claimAmount = reclaimAmount.roundToTwoDecimalPlaces.toString().some,
                          reimbursementMethod = Some(
                            if (reimbursementMethod === Subsidy) ReimbursementMethod.Subsidy
                            else if (reimbursementMethod === BankAccountTransfer) ReimbursementMethod.BankTransfer
                            else ReimbursementMethod.Deferment
                          )
                        )
                    }.some
                  )
                )
            )
          )
        }
    }

    "map a valid Consignee claim to TPI05 request" in forAll(genOverpaymentsSingleClaim(ClaimantType.Consignee)) {
      (singleOverpaymentsData: (SingleOverpaymentsClaim, ImportDeclaration, Option[ImportDeclaration])) =>
        val tpi05Request = mapper `map` singleOverpaymentsData

        val (claim, declaration, duplicateDeclaration) = singleOverpaymentsData

        val nrdcDetailsMap = declaration.displayResponseDetail.ndrcDetails.toList.flatten
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
            Symbol("reimbursementMethod")(None),
            Symbol("basisOfClaim")(claim.basisOfClaim.toTPI05DisplayString.some),
            Symbol("caseType")(Some(if (claim.reimbursementMethod === CurrentMonthAdjustment) CMA else Individual)),
            Symbol("goodsDetails")(
              claim.newEoriAndDan match {
                case None                =>
                  GoodsDetails(
                    descOfGoods = claim.additionalDetails.some.map(_.take(500)),
                    isPrivateImporter = Some(if (claim.claimantType === ClaimantType.Consignee) Yes else No)
                  ).some
                case Some(newEoriAndDan) =>
                  GoodsDetails(
                    descOfGoods = (newEoriAndDan.asAdditionalDetailsText ++ claim.additionalDetails).some
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
                    street = claim.claimantInformation.establishmentAddress.street.map(_.take(70)),
                    city = claim.claimantInformation.establishmentAddress.city,
                    countryCode = claim.claimantInformation.establishmentAddress.countryCode.getOrElse(Country.uk.code),
                    postalCode = claim.claimantInformation.establishmentAddress.postalCode,
                    telephoneNumber = claim.claimantInformation.establishmentAddress.telephoneNumber,
                    emailAddress = claim.claimantInformation.establishmentAddress.emailAddress
                  ),
                  contactInformation = claim.claimantInformation.contactInformation.some
                ),
                agentEORIDetails = {
                  val declarantDetails    = declaration.displayResponseDetail.declarantDetails
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
              val mrn = MRN(declaration.displayResponseDetail.declarationId)
              Some(
                MrnDetail(
                  MRNNumber = mrn.some,
                  acceptanceDate = AcceptanceDate
                    .fromDisplayFormat(declaration.displayResponseDetail.acceptanceDate)
                    .flatMap(_.toTpi05DateString)
                    .toOption,
                  declarantReferenceNumber = declaration.displayResponseDetail.declarantReferenceNumber,
                  mainDeclarationReference = (claim.movementReferenceNumber.value === mrn.value).some,
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
                    val consigneeDetails   = declaration.displayResponseDetail.effectiveConsigneeDetails
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
                  bankDetails = Option(claim.movementReferenceNumber.value === mrn.value)
                    .filter(_ === true)
                    .flatMap(_ =>
                      claim.bankAccountDetails
                        .map(bd => BankDetails(BankDetail.from(bd).some, BankDetail.from(bd).some))
                        .orElse(
                          declaration.displayResponseDetail.bankDetails.map(bd =>
                            BankDetails(
                              bd.consigneeBankDetails.map(BankDetail.from),
                              bd.declarantBankDetails.map(BankDetail.from)
                            )
                          )
                        )
                    ),
                  NDRCDetails =
                    claim.reimbursements.toList.map { case Reimbursement(taxCode, reclaimAmount, reimbursementMethod) =>
                      NdrcDetails(
                        paymentMethod = nrdcDetailsMap.get(taxCode.value).value.paymentMethod,
                        paymentReference = nrdcDetailsMap.get(taxCode.value).value.paymentReference,
                        CMAEligible = nrdcDetailsMap.get(taxCode.value).value.cmaEligible,
                        taxType = taxCode,
                        amount = nrdcDetailsMap.get(taxCode.value).value.amount,
                        claimAmount = reclaimAmount.roundToTwoDecimalPlaces.toString().some,
                        reimbursementMethod = Some(
                          if (reimbursementMethod === Subsidy) ReimbursementMethod.Subsidy
                          else if (reimbursementMethod === BankAccountTransfer) ReimbursementMethod.BankTransfer
                          else ReimbursementMethod.Deferment
                        )
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
                        declaration.displayResponseDetail.bankDetails.map(bd =>
                          BankDetails(
                            bd.consigneeBankDetails.map(BankDetail.from),
                            bd.declarantBankDetails.map(BankDetail.from)
                          )
                        )
                      ),
                    NDRCDetails = claim.reimbursements.toList.map {
                      case Reimbursement(taxCode, reclaimAmount, reimbursementMethod) =>
                        NdrcDetails(
                          paymentMethod = nrdcDetailsMap.get(taxCode.value).value.paymentMethod,
                          paymentReference = nrdcDetailsMap.get(taxCode.value).value.paymentReference,
                          CMAEligible = nrdcDetailsMap.get(taxCode.value).value.cmaEligible,
                          taxType = taxCode,
                          amount = nrdcDetailsMap.get(taxCode.value).value.amount,
                          claimAmount = reclaimAmount.roundToTwoDecimalPlaces.toString().some,
                          reimbursementMethod = Some(
                            if (reimbursementMethod === Subsidy) ReimbursementMethod.Subsidy
                            else if (reimbursementMethod === BankAccountTransfer) ReimbursementMethod.BankTransfer
                            else ReimbursementMethod.Deferment
                          )
                        )
                    }.some
                  )
                )
            )
          )
        }
    }

    "map a valid third-party User claim to TPI05 request" in forAll(
      genOverpaymentsSingleClaim(ClaimantType.User)
    ) { (singleOverpaymentsData: (SingleOverpaymentsClaim, ImportDeclaration, Option[ImportDeclaration])) =>
      val tpi05Request = mapper `map` singleOverpaymentsData

      val (claim, declaration, duplicateDeclaration) = singleOverpaymentsData

      val nrdcDetailsMap = declaration.displayResponseDetail.ndrcDetails.toList.flatten
        .groupBy(_.taxType)
        .view
        .mapValues(_.minByOption(_.taxType).value)
        .mapValues(ndrc => ndrc.copy(amount = BigDecimal(ndrc.amount).roundToTwoDecimalPlaces.toString()))

      inside(tpi05Request) { case Right(EisSubmitClaimRequest(PostNewClaimsRequest(common, details))) =>
        common.originatingSystem should be(MDTP)

        details.claimantEORI should ===(claim.claimantInformation.eori)

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
          Symbol("reimbursementMethod")(None),
          Symbol("basisOfClaim")(claim.basisOfClaim.toTPI05DisplayString.some),
          Symbol("caseType")(Some(if (claim.reimbursementMethod === CurrentMonthAdjustment) CMA else Individual)),
          Symbol("goodsDetails")(
            claim.newEoriAndDan match {
              case None                =>
                GoodsDetails(
                  descOfGoods = claim.additionalDetails.some.map(_.take(500)),
                  isPrivateImporter = Some(if (claim.claimantType === ClaimantType.Consignee) Yes else No)
                ).some
              case Some(newEoriAndDan) =>
                GoodsDetails(
                  descOfGoods = (newEoriAndDan.asAdditionalDetailsText ++ claim.additionalDetails).some
                    .map(_.take(500)),
                  isPrivateImporter = Some(if (claim.claimantType === ClaimantType.Consignee) Yes else No)
                ).some
            }
          ),
          Symbol("EORIDetails")(
            EoriDetails(
              importerEORIDetails = {
                val maybeConsigneeDetails = Some(declaration.displayResponseDetail.effectiveConsigneeDetails)
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
              },
              agentEORIDetails = {
                val declarantDetails    = declaration.displayResponseDetail.declarantDetails
                val maybeContactDetails = declarantDetails.contactDetails

                val maybeTelephone    = maybeContactDetails.flatMap(_.telephone)
                val maybeEmailAddress = maybeContactDetails.flatMap(_.emailAddress)

                EORIInformation(
                  EORINumber = declarantDetails.EORI,
                  CDSFullName = declarantDetails.legalName,
                  CDSEstablishmentAddress = Address(
                    contactPerson = claim.claimantInformation.establishmentAddress.contactPerson,
                    addressLine1 = claim.claimantInformation.establishmentAddress.addressLine1,
                    addressLine2 = claim.claimantInformation.establishmentAddress.addressLine2,
                    addressLine3 = claim.claimantInformation.establishmentAddress.addressLine3,
                    street = claim.claimantInformation.establishmentAddress.street.map(_.take(70)),
                    city = claim.claimantInformation.establishmentAddress.city,
                    countryCode = claim.claimantInformation.establishmentAddress.countryCode.getOrElse(Country.uk.code),
                    postalCode = claim.claimantInformation.establishmentAddress.postalCode,
                    telephoneNumber = claim.claimantInformation.establishmentAddress.telephoneNumber,
                    emailAddress = claim.claimantInformation.establishmentAddress.emailAddress
                  ),
                  contactInformation = claim.claimantInformation.contactInformation.some
                )
              }
            ).some
          ),
          Symbol("MRNDetails") {
            val mrn = MRN(declaration.displayResponseDetail.declarationId)
            Some(
              MrnDetail(
                MRNNumber = mrn.some,
                acceptanceDate = AcceptanceDate
                  .fromDisplayFormat(declaration.displayResponseDetail.acceptanceDate)
                  .flatMap(_.toTpi05DateString)
                  .toOption,
                declarantReferenceNumber = declaration.displayResponseDetail.declarantReferenceNumber,
                mainDeclarationReference = (claim.movementReferenceNumber.value === mrn.value).some,
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
                  val consigneeDetails   = declaration.displayResponseDetail.effectiveConsigneeDetails
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
                bankDetails = Option(claim.movementReferenceNumber.value === mrn.value)
                  .filter(_ === true)
                  .flatMap(_ =>
                    claim.bankAccountDetails
                      .map(bd => BankDetails(BankDetail.from(bd).some, BankDetail.from(bd).some))
                      .orElse(
                        declaration.displayResponseDetail.bankDetails.map(bd =>
                          BankDetails(
                            bd.consigneeBankDetails.map(BankDetail.from),
                            bd.declarantBankDetails.map(BankDetail.from)
                          )
                        )
                      )
                  ),
                NDRCDetails =
                  claim.reimbursements.toList.map { case Reimbursement(taxCode, reclaimAmount, reimbursementMethod) =>
                    NdrcDetails(
                      paymentMethod = nrdcDetailsMap.get(taxCode.value).value.paymentMethod,
                      paymentReference = nrdcDetailsMap.get(taxCode.value).value.paymentReference,
                      CMAEligible = nrdcDetailsMap.get(taxCode.value).value.cmaEligible,
                      taxType = taxCode,
                      amount = nrdcDetailsMap.get(taxCode.value).value.amount,
                      claimAmount = reclaimAmount.roundToTwoDecimalPlaces.toString().some,
                      reimbursementMethod = Some(
                        if (reimbursementMethod === Subsidy) ReimbursementMethod.Subsidy
                        else if (reimbursementMethod === BankAccountTransfer) ReimbursementMethod.BankTransfer
                        else ReimbursementMethod.Deferment
                      )
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
                      declaration.displayResponseDetail.bankDetails.map(bd =>
                        BankDetails(
                          bd.consigneeBankDetails.map(BankDetail.from),
                          bd.declarantBankDetails.map(BankDetail.from)
                        )
                      )
                    ),
                  NDRCDetails =
                    claim.reimbursements.toList.map { case Reimbursement(taxCode, reclaimAmount, reimbursementMethod) =>
                      NdrcDetails(
                        paymentMethod = nrdcDetailsMap.get(taxCode.value).value.paymentMethod,
                        paymentReference = nrdcDetailsMap.get(taxCode.value).value.paymentReference,
                        CMAEligible = nrdcDetailsMap.get(taxCode.value).value.cmaEligible,
                        taxType = taxCode,
                        amount = nrdcDetailsMap.get(taxCode.value).value.amount,
                        claimAmount = reclaimAmount.roundToTwoDecimalPlaces.toString().some,
                        reimbursementMethod = Some(
                          if (reimbursementMethod === Subsidy) ReimbursementMethod.Subsidy
                          else if (reimbursementMethod === BankAccountTransfer) ReimbursementMethod.BankTransfer
                          else ReimbursementMethod.Deferment
                        )
                      )
                    }.some
                )
              )
          )
        )
      }
    }
    "fail to map invalid claim amount" in {
      val singleOverpaymentsData                     = genOverpaymentsSingleClaim(ClaimantType.Declarant).sample.get
      val (claim, declaration, duplicateDeclaration) = singleOverpaymentsData

      val taxType = declaration.displayResponseDetail.ndrcDetails.get.head.taxType

      val updatedClaim = claim.copy(
        reimbursements =
          Seq(Reimbursement(TaxCode(taxType).get, BigDecimal(-1.00), ReimbursementMethodAnswer.BankAccountTransfer))
      )
      val tpi05Request = mapper.map(singleOverpaymentsData.copy(_1 = updatedClaim))

      tpi05Request match {
        case Left(error) =>
          error.value should be("Total reimbursement amount must be greater than zero")
        case Right(_)    =>
          fail("Expected a Left, but got a Right")
      }
    }

    "fail to map invalid duplicate declaration" in {

      val singleOverpaymentsData =
        genOverpaymentsSingleClaim(ClaimantType.Declarant, Some(BasisOfClaim.DuplicateEntry)).sample
          .getOrElse(fail("Failed to generate data"))

      val (
        claim: SingleOverpaymentsClaim,
        declaration: ImportDeclaration,
        duplicateDeclaration: Option[ImportDeclaration]
      ) = singleOverpaymentsData

      val value   = UUID.randomUUID().toString
      val amount  = BigDecimal(123456789123.12)
      val taxType = duplicateDeclaration.map(_.displayResponseDetail.ndrcDetails.get.head.taxType).get

      val updatedDuplicateImportDeclaration = duplicateDeclaration.map { declaration =>
        declaration.copy(displayResponseDetail =
          declaration.displayResponseDetail.copy(ndrcDetails =
            Some(
              List(
                ResponseNdrcDetails(
                  paymentMethod = value,
                  paymentReference = value,
                  cmaEligible = Some(CMAEligibleGen.CMAEligible),
                  taxType = taxType,
                  amount = amount.toString()
                )
              )
            )
          )
        )
      }

      val tpi05Request = mapper.map(
        singleOverpaymentsData.copy(_3 = updatedDuplicateImportDeclaration)
      )

      tpi05Request match {
        case Left(error) =>
          error.value should be(
            s"Failed to build Duplicate MRN detail - The payment method is expected to be 3 characters long: $value;\n" +
              s"The payment reference is blank or exceeds 18 characters: $value;\n" +
              s"Bad amount format: ${amount.toString}"
          )
        case Right(_)    =>
          fail("Expected a Left, but got a Right")
      }
    }

    "fail to map missing email" in {
      val singleOverpaymentsData                     = genOverpaymentsSingleClaimAllTypes.sample.get
      val (claim, declaration, duplicateDeclaration) = singleOverpaymentsData
      val updatedClaim                               = claim
        .copy(
          claimantInformation = claim.claimantInformation
            .copy(
              contactInformation = claim.claimantInformation.contactInformation
                .copy(emailAddress = None)
            )
        )

      val tpi05Request = mapper.map((updatedClaim, declaration, duplicateDeclaration))

      tpi05Request match {
        case Left(error) =>
          error.value should be("Email address is missing")
        case Right(_)    =>
          fail("Expected a Left, but got a Right")
      }
    }

    "fail to map missing claimant name" in {
      val singleOverpaymentsData                     = genOverpaymentsSingleClaimAllTypes.sample.get
      val (claim, declaration, duplicateDeclaration) = singleOverpaymentsData
      val updatedClaim                               = claim
        .copy(
          claimantInformation = claim.claimantInformation
            .copy(
              contactInformation = claim.claimantInformation.contactInformation
                .copy(contactPerson = None)
            )
        )

      val tpi05Request = mapper.map((updatedClaim, declaration, duplicateDeclaration))

      tpi05Request match {
        case Left(error) =>
          error.value should be("Claimant name is missing")
        case Right(_)    =>
          fail("Expected a Left, but got a Right")
      }
    }
  }
}
