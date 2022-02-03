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

import cats.data.NonEmptyList
import cats.implicits.catsSyntaxOptionId
import org.scalacheck.Gen
import org.scalatest.Inside.inside
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import shapeless.lens
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform.MDTP
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{C285ClaimRequest, Country, Street}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.{AcceptanceDate, ISOLocalDate}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.CDFPayService.NDRC
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.{ClaimType, CustomDeclarationType}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Acc14DeclarationGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.C285ClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ClaimedReimbursementGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.utils.BigDecimalOps

class C285ClaimMappingSpec
    extends AnyWordSpec
    with C285Support
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with OptionValues {

  "The C285 claim mapper" should {

    "map a valid claim to TPI05 request" in forAll { c285ClaimRequest: C285ClaimRequest =>
      val tpi05Request = c285ClaimToTPI05Mapper map c285ClaimRequest

      inside(tpi05Request) { case Right(EisSubmitClaimRequest(PostNewClaimsRequest(common, details))) =>
        common.originatingSystem should be(MDTP)

        details should have(
          'CDFPayService (NDRC),
          'dateReceived (ISOLocalDate.now.some),
          'customDeclarationType (CustomDeclarationType.MRN.some),
          'claimDate (ISOLocalDate.now.some),
          'claimType (ClaimType.C285.some),
          'claimant (c285ClaimRequest.claim.claimant.some),
          'payeeIndicator (c285ClaimRequest.claim.claimant.some),
          'claimantEORI (c285ClaimRequest.signedInUserDetails.eori.some),
          'claimantEmailAddress (c285ClaimRequest.signedInUserDetails.email),
          'declarationMode (c285ClaimRequest.claim.declarationMode.some),
          'claimAmountTotal (c285ClaimRequest.claim.claimedAmountAsString.some),
          'reimbursementMethod (c285ClaimRequest.claim.reimbursementMethod.some),
          'basisOfClaim (c285ClaimRequest.claim.basisOfClaimAnswer.toTPI05DisplayString.some),
          'caseType (c285ClaimRequest.claim.caseType.some),
          'goodsDetails (
            GoodsDetails(
              descOfGoods = c285ClaimRequest.claim.commodityDetailsAnswer.value.some,
              isPrivateImporter = c285ClaimRequest.claim.isForPrivateImporter.some
            ).some
          ),
          'EORIDetails (
            EoriDetails(
              agentEORIDetails = EORIInformation(
                EORINumber = c285ClaimRequest.claim.declarantDetails.map(_.EORI),
                CDSFullName = c285ClaimRequest.claim.declarantDetails.map(_.legalName),
                CDSEstablishmentAddress = Address(
                  contactPerson = c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.fullName.some,
                  addressLine1 = c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.contactAddress.line1.some,
                  addressLine2 = c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.contactAddress.line2,
                  AddressLine3 = c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.contactAddress.line3,
                  street = Street.fromLines(
                    c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.contactAddress.line1.some,
                    c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.contactAddress.line2
                  ),
                  city = c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.contactAddress.line4.some,
                  postalCode = c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.contactAddress.postcode.value.some,
                  countryCode = c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.contactAddress.country.code,
                  emailAddress = c285ClaimRequest.claim.detailsRegisteredWithCdsAnswer.emailAddress.value.some,
                  telephone = None
                ),
                contactInformation = c285ClaimRequest.claim.contactInformation.some
              ),
              importerEORIDetails = {
                val maybeConsigneeDetails = c285ClaimRequest.claim.consigneeDetails
                val maybeContactDetails   = maybeConsigneeDetails.flatMap(_.contactDetails)

                EORIInformation(
                  EORINumber = maybeConsigneeDetails.map(_.EORI),
                  CDSFullName = maybeConsigneeDetails.map(_.legalName),
                  CDSEstablishmentAddress = Address(
                    contactPerson = None,
                    addressLine1 = maybeConsigneeDetails.map(_.establishmentAddress.addressLine1),
                    addressLine2 = maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine2),
                    AddressLine3 = maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine3),
                    street = Street.fromLines(
                      maybeConsigneeDetails.map(_.establishmentAddress.addressLine1),
                      maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine2)
                    ),
                    city = maybeConsigneeDetails.flatMap(_.establishmentAddress.addressLine3),
                    countryCode = maybeConsigneeDetails
                      .map(_.establishmentAddress.countryCode)
                      .getOrElse(Country.uk.code),
                    postalCode = maybeConsigneeDetails.flatMap(_.establishmentAddress.postalCode),
                    telephone = maybeContactDetails.flatMap(_.telephone),
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
          'MRNDetails (
            c285ClaimRequest.claim.displayDeclaration.toList.flatMap { displayDeclaration =>
              c285ClaimRequest.claim.multipleClaims.map { case (mrn, reimbursementClaim) =>
                MrnDetail(
                  MRNNumber = mrn.some,
                  acceptanceDate = AcceptanceDate
                    .fromDisplayFormat(displayDeclaration.displayResponseDetail.acceptanceDate)
                    .flatMap(_.toTpi05DateString)
                    .toOption,
                  declarantReferenceNumber = displayDeclaration.displayResponseDetail.declarantReferenceNumber,
                  mainDeclarationReference = (c285ClaimRequest.claim.movementReferenceNumber.value === mrn.value).some,
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
                        AddressLine3 = declarantDetails.establishmentAddress.addressLine3,
                        street = Street.fromLines(
                          declarantDetails.establishmentAddress.addressLine1.some,
                          declarantDetails.establishmentAddress.addressLine2
                        ),
                        city = declarantDetails.establishmentAddress.addressLine3,
                        countryCode = declarantDetails.establishmentAddress.countryCode,
                        postalCode = declarantDetails.establishmentAddress.postalCode,
                        telephone = None,
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
                    val consigneeDetails   = displayDeclaration.displayResponseDetail.consigneeDetails.value
                    val contactInformation = consigneeDetails.contactDetails.value

                    MRNInformation(
                      EORI = consigneeDetails.EORI,
                      legalName = consigneeDetails.legalName,
                      establishmentAddress = Address(
                        contactPerson = None,
                        addressLine1 = consigneeDetails.establishmentAddress.addressLine1.some,
                        addressLine2 = consigneeDetails.establishmentAddress.addressLine2,
                        AddressLine3 = consigneeDetails.establishmentAddress.addressLine3,
                        street = Street.fromLines(
                          consigneeDetails.establishmentAddress.addressLine1.some,
                          consigneeDetails.establishmentAddress.addressLine2
                        ),
                        city = consigneeDetails.establishmentAddress.addressLine3,
                        countryCode = consigneeDetails.establishmentAddress.countryCode,
                        postalCode = consigneeDetails.establishmentAddress.postalCode,
                        telephone = None,
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
                  bankDetails = Option(c285ClaimRequest.claim.movementReferenceNumber.value === mrn.value)
                    .filter(_ === true)
                    .flatMap(_ =>
                      c285ClaimRequest.claim.firstNonEmptyBankDetails(
                        c285ClaimRequest.claim.displayDeclaration.flatMap(_.displayResponseDetail.bankDetails)
                      )
                    ),
                  NDRCDetails = reimbursementClaim.toList
                    .map(claim =>
                      NdrcDetails(
                        paymentMethod = claim.paymentMethod,
                        paymentReference = claim.paymentReference,
                        CMAEligible = None,
                        taxType = claim.taxCode,
                        amount = claim.paidAmount.roundToTwoDecimalPlaces.toString(),
                        claimAmount = claim.claimAmount.roundToTwoDecimalPlaces.toString().some
                      )
                    )
                    .some
                )
              }
            }.some
          ),
          'duplicateMRNDetails (
            c285ClaimRequest.claim.duplicateDisplayDeclaration
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
                        AddressLine3 = declarantDetails.establishmentAddress.addressLine3,
                        street = Street.fromLines(
                          declarantDetails.establishmentAddress.addressLine1.some,
                          declarantDetails.establishmentAddress.addressLine2
                        ),
                        city = declarantDetails.establishmentAddress.addressLine3,
                        countryCode = declarantDetails.establishmentAddress.countryCode,
                        postalCode = declarantDetails.establishmentAddress.postalCode,
                        telephone = None,
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
                    val consigneeDetails   = details.consigneeDetails.value
                    val contactInformation = consigneeDetails.contactDetails.value

                    MRNInformation(
                      EORI = consigneeDetails.EORI,
                      legalName = consigneeDetails.legalName,
                      establishmentAddress = Address(
                        contactPerson = None,
                        addressLine1 = consigneeDetails.establishmentAddress.addressLine1.some,
                        addressLine2 = consigneeDetails.establishmentAddress.addressLine2,
                        AddressLine3 = consigneeDetails.establishmentAddress.addressLine3,
                        street = Street.fromLines(
                          consigneeDetails.establishmentAddress.addressLine1.some,
                          consigneeDetails.establishmentAddress.addressLine2
                        ),
                        city = consigneeDetails.establishmentAddress.addressLine3,
                        countryCode = consigneeDetails.establishmentAddress.countryCode,
                        postalCode = consigneeDetails.establishmentAddress.postalCode,
                        telephone = None,
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
                  bankDetails = c285ClaimRequest.claim.firstNonEmptyBankDetails(details.bankDetails),
                  NDRCDetails = c285ClaimRequest.claim.claims.toList.map { reimbursement =>
                    NdrcDetails(
                      paymentMethod = reimbursement.paymentMethod,
                      paymentReference = reimbursement.paymentReference,
                      CMAEligible = None,
                      taxType = reimbursement.taxCode,
                      amount = reimbursement.paidAmount.roundToTwoDecimalPlaces.toString(),
                      claimAmount = reimbursement.claimAmount.roundToTwoDecimalPlaces.toString().some
                    )
                  }.some
                )
              )
          )
        )
      }
    }

    "fail with the error" when {

      "mapping claim with negative claimed amount" in {
        val claimedReimbursementsLens = lens[C285ClaimRequest].claim.claimedReimbursementsAnswer

        forAll(Gen.negNum[Long], genClaimedReimbursement, arbitraryC285ClaimRequest.arbitrary) {
          (amount, reimbursement, request) =>
            val negativeAmountRequest = claimedReimbursementsLens.set(request)(
              NonEmptyList.one(reimbursement.copy(claimAmount = BigDecimal(amount)))
            )

            val tpi05Request = c285ClaimToTPI05Mapper map negativeAmountRequest

            tpi05Request.left.map(_.value should be("Total reimbursement amount must be greater than zero"))
        }
      }

      "mapping claim with missing email" in {
        val emailLens = lens[C285ClaimRequest].signedInUserDetails.email

        forAll { request: C285ClaimRequest =>
          val blankEmailRequest = emailLens.set(request)(Email("").some)

          val tpi05Request = c285ClaimToTPI05Mapper map blankEmailRequest

          tpi05Request.left.map(_.value should be("Email address is missing"))
        }
      }

      "mapping claim with missing bankDetails" in {
        val responseDetailLens = lens[DisplayDeclaration].displayResponseDetail
        val claimLens          = lens[C285ClaimRequest].claim

        forAll { request: C285ClaimRequest =>
          val missingBankDetailsDeclaration = responseDetailLens.modify(request.claim.displayDeclaration.value)(
            _.copy(
              bankDetails = None,
              maskedBankDetails = None
            )
          )

          val missingBankDetailsRequest = claimLens.modify(request)(
            _.copy(
              displayDeclaration = missingBankDetailsDeclaration.some,
              bankAccountDetailsAnswer = None
            )
          )

          val tpi05Request = c285ClaimToTPI05Mapper map missingBankDetailsRequest

          tpi05Request.left.map(_.value should be("Failed to build MRN detail - Missing bank details"))
        }
      }

      "mapping claim with invalid Acceptance date" in {
        val acceptanceDateLens     = lens[DisplayDeclaration].displayResponseDetail.acceptanceDate
        val displayDeclarationLens = lens[C285ClaimRequest].claim.displayDeclaration

        forAll { (request: C285ClaimRequest, date: AcceptanceDate) =>
          val invalidFormatAcceptanceDate = date.toTpi05DateString.getOrElse("")

          val invalidAcceptanceDateDeclaration =
            acceptanceDateLens.set(request.claim.displayDeclaration.value)(invalidFormatAcceptanceDate)

          val invalidAcceptanceDateRequest = displayDeclarationLens.set(request)(invalidAcceptanceDateDeclaration.some)

          val tpi05Request = c285ClaimToTPI05Mapper map invalidAcceptanceDateRequest

          tpi05Request.left.map(
            _.value should be(
              s"Failed to build MRN detail - Error formatting acceptance date: Text '$invalidFormatAcceptanceDate' could not be parsed at index 8"
            )
          )
        }
      }

      "mapping claim with missing consignee details" in {
        val consigneeDetailsLens   = lens[DisplayDeclaration].displayResponseDetail.consigneeDetails
        val displayDeclarationLens = lens[C285ClaimRequest].claim.displayDeclaration

        forAll { request: C285ClaimRequest =>
          val missingConsigneeDetailsDeclaration =
            consigneeDetailsLens.set(request.claim.displayDeclaration.value)(None)

          val missingConsigneeDetailsRequest =
            displayDeclarationLens.set(request)(missingConsigneeDetailsDeclaration.some)

          val tpi05Request = c285ClaimToTPI05Mapper map missingConsigneeDetailsRequest

          tpi05Request.left.map(_.value should be(s"Failed to build MRN detail - The consignee details are missing"))
        }
      }

      "mapping claim with missing declarant contact details" in {
        val declarantContactDetailsLens = lens[DisplayDeclaration].displayResponseDetail.declarantDetails.contactDetails
        val displayDeclarationLens      = lens[C285ClaimRequest].claim.displayDeclaration

        forAll { request: C285ClaimRequest =>
          val missingDeclarantContactDetailsDeclaration =
            declarantContactDetailsLens.set(request.claim.displayDeclaration.value)(None)

          val missingDeclarantContactDetailsRequest =
            displayDeclarationLens.set(request)(missingDeclarantContactDetailsDeclaration.some)

          val tpi05Request = c285ClaimToTPI05Mapper map missingDeclarantContactDetailsRequest

          tpi05Request.left.map(
            _.value should be(s"Failed to build MRN detail - The declarant contact information is missing")
          )
        }
      }
    }
  }
}
