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
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimantType, Country, MultipleRejectedGoodsClaim, Street, TaxCode}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.{AcceptanceDate, ISOLocalDate, TemporalAccessorOps}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.*
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.CaseType.Bulk
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.DeclarationMode.AllDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{ClaimType, CustomDeclarationType}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.NdrcDetails as ResponseNdrcDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.RejectedGoodsClaimGen.*
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.TaxCodesGen.*
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.{BigDecimalOps, Lens}

import java.util.UUID

@SuppressWarnings(Array("org.wartremover.warts.TraversableOps", "org.wartremover.warts.IterableOps"))
class MultipleRejectedGoodsClaimMappingSpec
    extends AnyWordSpec
    with RejectedGoodsClaimSupport
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with OptionValues
    with TypeCheckedTripleEquals {

  val mapper = new RejectedGoodsClaimToTPI05Mapper[MultipleRejectedGoodsClaim](false)

  "The Reject Goods claim mapper" should {

    "map a valid Multiple claim to TPI05 request" in forAll(genMultipleRejectedGoodsClaim(ClaimantType.Declarant)) {
      (details: (MultipleRejectedGoodsClaim, List[DisplayDeclaration])) =>
        val (claim, declarations) = details
        val leadDeclaration       = declarations.head
        val tpi05Request          = mapper.map((claim, declarations))

        val claimsOverMrns = claim.reimbursementClaims.flatMap { case (mrn, taxesClaimed) =>
          declarations
            .filter(_.displayResponseDetail.declarationId === mrn.value)
            .map(declaration => mrn -> ((taxesClaimed, declaration)))
        }

        inside(tpi05Request) { case Right(EisSubmitClaimRequest(PostNewClaimsRequest(common, details))) =>
          common.originatingSystem should be(MDTP)

          details.claimantEORI         should ===(claim.claimantInformation.eori)
          details.claimantEmailAddress should ===(
            claim.claimantInformation.contactInformation.emailAddress.map(Email(_)).value
          )

          details should have(
            Symbol("CDFPayService")(NDRC),
            Symbol("newEORI")(None),
            Symbol("newDAN")(None),
            Symbol("dateReceived")(ISOLocalDate.now.some),
            Symbol("customDeclarationType")(CustomDeclarationType.MRN.some),
            Symbol("claimDate")(ISOLocalDate.now.some),
            Symbol("claimType")(ClaimType.CE1179.some),
            Symbol("claimant")(claim.claimant.some),
            Symbol("payeeIndicator")(claim.payeeIndicator.some),
            Symbol("claimAmountTotal")(claim.claimedAmountAsString.some),
            Symbol("reimbursementMethod")(claim.tpi05ReimbursementMethod.some),
            Symbol("basisOfClaim")(claim.basisOfClaim.toTPI05DisplayString.some),
            Symbol("goodsDetails")(
              GoodsDetails(
                descOfGoods = claim.detailsOfRejectedGoods.some.map(_.take(500)),
                anySpecialCircumstances = claim.basisOfClaimSpecialCircumstances.map(_.take(500)),
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
                  val maybeConsigneeDetails = Some(leadDeclaration.displayResponseDetail.effectiveConsigneeDetails)
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
                              claimAmount = claimedAmount.roundToTwoDecimalPlaces.toString().some,
                              None
                            )
                          )
                      }
                      .toList
                      .flatten(Option.option2Iterable)
                  }.some
                )
              }.some
            ),
            Symbol("caseType")(Bulk.some),
            Symbol("declarationMode")(AllDeclaration.some)
          )
        }
    }

    "fail with the error" when {
      val reimbursementClaimsLens = new Lens[MultipleRejectedGoodsClaim, Map[MRN, Map[TaxCode, BigDecimal]]] {
        override def set(
          root: MultipleRejectedGoodsClaim,
          value: Map[MRN, Map[TaxCode, BigDecimal]]
        ): MultipleRejectedGoodsClaim =
          root.copy(reimbursementClaims = value)
      }

      val ndrcLens = new Lens[DisplayDeclaration, Option[List[ResponseNdrcDetails]]] {
        override def set(root: DisplayDeclaration, value: Option[List[ResponseNdrcDetails]]): DisplayDeclaration =
          root.copy(displayResponseDetail = root.displayResponseDetail.copy(ndrcDetails = value))
      }

      "mapping claim having incorrect NDRC details" in forAll {
        (details: (MultipleRejectedGoodsClaim, List[DisplayDeclaration]), random: UUID, amount: BigDecimal) =>
          val (claim, declarations)             = details
          val value                             = random.toString
          val declarationWithInvalidNdrcDetails = declarations.map { declaration =>
            val ndrcDetails = declaration.displayResponseDetail.ndrcDetails
            ndrcLens.set(
              declaration,
              ndrcDetails.map(
                _.map(detail =>
                  ResponseNdrcDetails(
                    paymentMethod = value,
                    paymentReference = value,
                    cmaEligible = None,
                    taxType = detail.taxType,
                    amount = amount.toString()
                  )
                )
              )
            )
          }

          val tpi05Request = mapper.map((claim, declarationWithInvalidNdrcDetails))

          tpi05Request.left.map(
            _.value should be(
              s"Failed to build MRN detail - The payment method is expected to be 3 characters long: $value;\n" +
                s"The payment reference is blank or exceeds 18 characters: $value;\n" +
                s"Bad amount format: ${amount.toString()}"
            )
          )
      }

      "cannot find NDRC details for claimed reimbursement" in forAll {
        (details: (MultipleRejectedGoodsClaim, List[DisplayDeclaration]), taxCode: TaxCode) =>
          val (rejectedGoodsClaim, declarations) = details
          val reimbursement                      = Map(rejectedGoodsClaim.leadMrn -> Map(taxCode -> BigDecimal(7)))

          val updatedClaim        = reimbursementClaimsLens.set(rejectedGoodsClaim, reimbursement)
          val updatedDeclarations = declarations.map { declaration =>
            ndrcLens.set(declaration, None)
          }

          val tpi05Request = mapper.map((updatedClaim, updatedDeclarations))

          tpi05Request.left.map(
            _.value should be(
              s"Failed to build MRN detail - Cannot find NDRC details for tax code: $taxCode"
            )
          )
      }
    }
  }
}
