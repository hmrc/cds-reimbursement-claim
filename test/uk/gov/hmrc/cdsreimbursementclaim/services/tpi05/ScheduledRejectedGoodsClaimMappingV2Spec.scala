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
import shapeless.lens
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform.MDTP
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{AmountPaidWithRefund, ClaimantType, Country, PayeeType, ScheduledRejectedGoodsClaim, Street, TaxCode}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.{AcceptanceDate, ISOLocalDate, TemporalAccessorOps}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.CDFPayService.NDRC
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.CaseType.Bulk
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.DeclarationMode.ParentDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{ClaimType, CustomDeclarationType}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.RejectedGoodsClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TaxCodesGen._
import uk.gov.hmrc.cdsreimbursementclaim.utils.{BigDecimalOps, WAFRules}

import java.util.UUID
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant.{Importer, Representative}

class ScheduledRejectedGoodsClaimMappingV2Spec
    extends AnyWordSpec
    with RejectedGoodsClaimSupport
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with OptionValues
    with TypeCheckedTripleEquals {

  val mapper = new ScheduledRejectedGoodsClaimToTPI05Mapper(true)

  "The Reject Goods claim mapper" should {

    "map a valid Scheduled claim to TPI05 request" in forAll {
      details: (ScheduledRejectedGoodsClaim, DisplayDeclaration) =>
        val claim       = details._1
        val declaration = details._2

        val tpi05Request = mapper.map(details)

        inside(tpi05Request) { case Right(EisSubmitClaimRequest(PostNewClaimsRequest(common, details))) =>
          common.originatingSystem should be(MDTP)

          details.claimantEORI         should ===(claim.claimantInformation.eori)
          details.claimantEmailAddress should ===(
            claim.claimantInformation.contactInformation.emailAddress.map(Email(_)).value
          )

          details should have(
            Symbol("CDFPayService")(NDRC),
            Symbol("dateReceived")(ISOLocalDate.now.some),
            Symbol("customDeclarationType")(CustomDeclarationType.MRN.some),
            Symbol("claimDate")(ISOLocalDate.now.some),
            Symbol("claimType")(ClaimType.CE1179.some),
            Symbol("claimant")(Some(if (claim.claimantType === ClaimantType.Consignee) Importer else Representative)),
            Symbol("payeeIndicator")(Some(if (claim.payeeType === PayeeType.Consignee) Importer else Representative)),
            Symbol("claimAmountTotal")(claim.claimedAmountAsString.some),
            Symbol("reimbursementMethod")(None),
            Symbol("basisOfClaim")(claim.basisOfClaim.toTPI05DisplayString.some),
            Symbol("goodsDetails")(
              GoodsDetails(
                descOfGoods = claim.detailsOfRejectedGoods.some.map(WAFRules.asSafeText),
                anySpecialCircumstances = claim.basisOfClaimSpecialCircumstances.map(WAFRules.asSafeText),
                dateOfInspection = claim.inspectionDate.toIsoLocalDate.some,
                atTheImporterOrDeclarantAddress = claim.inspectionAddress.addressType.toTPI05DisplayString.some,
                inspectionAddress = InspectionAddress(
                  addressLine1 = claim.inspectionAddress.addressLine1,
                  addressLine2 = claim.inspectionAddress.addressLine2,
                  addressLine3 = claim.inspectionAddress.addressLine3,
                  city = claim.inspectionAddress.city,
                  countryCode = claim.inspectionAddress.countryCode,
                  postalCode = claim.inspectionAddress.postalCode
                ).some
              ).some
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
                  val maybeConsigneeDetails = declaration.displayResponseDetail.effectiveConsigneeDetails
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
            Symbol("MRNDetails")(
              List(
                MrnDetail(
                  MRNNumber = claim.movementReferenceNumber.some,
                  acceptanceDate = AcceptanceDate
                    .fromDisplayFormat(declaration.displayResponseDetail.acceptanceDate)
                    .flatMap(_.toTpi05DateString)
                    .toOption,
                  declarantReferenceNumber = declaration.displayResponseDetail.declarantReferenceNumber,
                  mainDeclarationReference = true.some,
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
                  NDRCDetails = claim.getClaimedReimbursements.map { reimbursement =>
                    NdrcDetails(
                      paymentMethod = reimbursement.paymentMethod,
                      paymentReference = reimbursement.paymentReference,
                      CMAEligible = None,
                      taxType = reimbursement.taxCode,
                      amount = reimbursement.paidAmount.roundToTwoDecimalPlaces.toString(),
                      claimAmount = reimbursement.claimAmount.roundToTwoDecimalPlaces.toString().some,
                      claim.tpi05ReimbursementMethod.some
                    )
                  }.some
                )
              ).some
            ),
            Symbol("caseType")(Bulk.some),
            Symbol("declarationMode")(ParentDeclaration.some)
          )
        }
    }

    "fail with the error" when {

      "mapping claim having incorrect NDRC details" in {
        val ndrcDetailsLens = lens[DisplayDeclaration].displayResponseDetail.ndrcDetails

        forAll { (random: UUID, amount: BigDecimal, details: (ScheduledRejectedGoodsClaim, DisplayDeclaration)) =>
          val value       = random.toString
          val claim       = details._1
          val declaration = details._2
          val ndrcDetails = declaration.displayResponseDetail.ndrcDetails

          val declarationWithInvalidNdrcDetails = ndrcDetailsLens.set(declaration)(
            ndrcDetails.map(
              _.map(detail =>
                uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.NdrcDetails(
                  paymentMethod = value,
                  paymentReference = value,
                  cmaEligible = None,
                  taxType = detail.taxType,
                  amount = amount.toString()
                )
              )
            )
          )

          val tpi05Request =
            mapper.map((claim, declarationWithInvalidNdrcDetails))

          tpi05Request.left.map(
            _.value should be(
              s"Failed to build MRN detail - The payment method is expected to be 3 characters long: $value;\n" +
                s"The payment reference is blank or exceeds 18 characters: $value;\n" +
                s"Bad amount format: ${amount.toString()}"
            )
          )
        }
      }

      "cannot find NDRC details for claimed reimbursement" in {
        val ndrcLens                = lens[DisplayDeclaration].displayResponseDetail.ndrcDetails
        val reimbursementClaimsLens = lens[ScheduledRejectedGoodsClaim].reimbursementClaims

        forAll { (details: (ScheduledRejectedGoodsClaim, DisplayDeclaration), taxCode: TaxCode) =>
          val rejectedGoodsClaim = details._1
          val displayDeclaration = details._2

          val claims = Map("eu-duty" -> Map(taxCode -> AmountPaidWithRefund(BigDecimal(8), BigDecimal(7))))

          val updatedClaim = reimbursementClaimsLens.set(rejectedGoodsClaim)(claims)

          val updatedDeclaration = ndrcLens.set(displayDeclaration)(None)

          val tpi05Request = mapper.map((updatedClaim, updatedDeclaration))

          tpi05Request.left.map(
            _.value should be(
              s"Failed to build MRN detail - Cannot find NDRC details for tax code: $taxCode"
            )
          )
        }
      }
    }
  }
}
