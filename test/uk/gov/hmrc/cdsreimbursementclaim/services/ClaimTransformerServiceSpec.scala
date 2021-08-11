/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.cdsreimbursementclaim.services

import org.scalamock.handlers.CallHandler0
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.Address.NonUkAddress
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.CompleteClaim.CompleteC285Claim
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ContactDetailsAnswer.CompleteContactDetailsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DeclarationDetailsAnswer.CompleteDeclarationDetailsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DetailsRegisteredWithCdsAnswer.CompleteDetailsRegisteredWithCdsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.answers.ClaimsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{BankAccountDetails, Claim, ConsigneeDetails, ContactDetails, ContactDetailsFormData, Country, DateOfImport, DeclarantDetails, DeclarantTypeAnswer, DetailsRegisteredWithCdsFormData, DisplayDeclaration, DisplayResponseDetail, EntryDeclarationDetails, EstablishmentAddress, MovementReferenceNumber, SubmitClaimRequest, TaxCode, Address => _}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.DateGenerator
import uk.gov.hmrc.cdsreimbursementclaim.models.eis
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.{EisSubmitClaimRequest, _}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.CompleteClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.{EntryNumber, MRN, UUIDGenerator}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class ClaimTransformerServiceSpec extends AnyWordSpec with Matchers with MockFactory {

  val mockUuidGenerator: UUIDGenerator = mock[UUIDGenerator]
  val mockDateGenerator: DateGenerator = mock[DateGenerator]

  def mockGenerateUUID(uuid: UUID): CallHandler0[String] =
    (mockUuidGenerator.compactCorrelationId _: () => String).expects().returning(uuid.toString)

  def mockGenerateReceiptDate(receiptDate: String): CallHandler0[String] =
    (mockDateGenerator.nextReceiptDate _: () => String).expects().returning(receiptDate)

  def mockGenerateIsoLocalDate(isoLocalDate: String): CallHandler0[String] =
    (mockDateGenerator.nextIsoLocalDate _: () => String).expects().returning(isoLocalDate)

  val transformer = new DefaultClaimTransformerService(mockUuidGenerator, mockDateGenerator)

  "Claim transformer" when {

    "passed a claim request" must {

      "make an EIS submit claim request for a valid legacy number claim" in {

        val claim = sample[Claim].copy(
          paymentMethod = "001",
          taxCode = TaxCode.A00.value,
          paidAmount = BigDecimal(20.00),
          claimAmount = BigDecimal(10.00),
          paymentReference = "pay-ref"
        )

        val claimsAnswer = ClaimsAnswer(claim)

        val completeBankAccountDetailAnswer: BankAccountDetails = sample[BankAccountDetails]

        val entryDeclarationDetails = sample[EntryDeclarationDetails].copy(
          dateOfImport = DateOfImport(LocalDate.parse("2020-12-06", DateTimeFormatter.ofPattern("u-M-d")))
        )

        val completeDeclarationDetailsAnswer = sample[CompleteDeclarationDetailsAnswer].copy(
          declarationDetails = entryDeclarationDetails
        )
        val basisOfClaimAnswer               = BasisOfClaim.DutySuspension

        val declarantTypeAnswer = DeclarantTypeAnswer.Importer

        val correlationId = UUID.randomUUID()

        val requestCommon = RequestCommon(
          originatingSystem = "MDTP",
          receiptDate = "2021-03-08T13:57:53Z",
          acknowledgementReference = correlationId.toString
        )

        val movementReferenceNumber = sample[MovementReferenceNumber]
          .copy(value = Left(EntryNumber("666541198B49856762")))

        val completeClaim =
          sample[CompleteC285Claim].copy(
            movementReferenceNumber = movementReferenceNumber,
            maybeCompleteReasonAndBasisOfClaimAnswer = None,
            claimsAnswer = claimsAnswer,
            maybeBasisOfClaimAnswer = Some(basisOfClaimAnswer),
            declarantTypeAnswer = declarantTypeAnswer,
            maybeCompleteBankAccountDetailAnswer = Some(completeBankAccountDetailAnswer),
            maybeCompleteDeclarationDetailsAnswer = Some(completeDeclarationDetailsAnswer),
            maybeCompleteDuplicateDeclarationDetailsAnswer = None
          )

        val submitClaimRequest = sample[SubmitClaimRequest].copy(completeClaim = completeClaim)

        val eoriDetails = EoriDetails(
          agentEORIDetails = EORIInformation(
            EORINumber = submitClaimRequest.signedInUserDetails.eori.value,
            CDSFullName = completeClaim.entryDeclarationDetails.map(s => s.declarationDetails.declarantName),
            legalEntityType = None,
            EORIStartDate = None,
            CDSEstablishmentAddress = Address(
              contactPerson = None,
              addressLine1 = None,
              addressLine2 = None,
              AddressLine3 = None,
              street = None,
              city = None,
              countryCode = "GB",
              postalCode = None,
              telephone =
                completeClaim.entryDeclarationDetails.map(s => s.declarationDetails.declarantPhoneNumber.value),
              emailAddress =
                completeClaim.entryDeclarationDetails.map(s => s.declarationDetails.declarantEmailAddress.value)
            ),
            contactInformation = Some(
              ContactInformation(
                contactPerson = completeClaim.entryDeclarationDetails.map(s => s.declarationDetails.declarantName),
                addressLine1 = None,
                addressLine2 = None,
                addressLine3 = None,
                street = None,
                city = None,
                countryCode = None,
                postalCode = None,
                telephoneNumber =
                  completeClaim.entryDeclarationDetails.map(s => s.declarationDetails.declarantPhoneNumber.value),
                faxNumber = None,
                emailAddress =
                  completeClaim.entryDeclarationDetails.map(s => s.declarationDetails.declarantEmailAddress.value)
              )
            ),
            VATDetails = None
          ),
          importerEORIDetails = EORIInformation(
            EORINumber = submitClaimRequest.signedInUserDetails.eori.value,
            CDSFullName = completeClaim.entryDeclarationDetails.map(s => s.declarationDetails.importerName),
            legalEntityType = None,
            EORIStartDate = None,
            CDSEstablishmentAddress = Address(
              contactPerson = None,
              addressLine1 = None,
              addressLine2 = None,
              AddressLine3 = None,
              street = None,
              city = None,
              countryCode = "GB",
              postalCode = None,
              telephone =
                completeClaim.entryDeclarationDetails.map(s => s.declarationDetails.importerPhoneNumber.value),
              emailAddress =
                completeClaim.entryDeclarationDetails.map(s => s.declarationDetails.importerEmailAddress.value)
            ),
            contactInformation = Some(
              ContactInformation(
                contactPerson = completeClaim.entryDeclarationDetails.map(s => s.declarationDetails.importerName),
                addressLine1 = None,
                addressLine2 = None,
                addressLine3 = None,
                street = None,
                city = None,
                countryCode = None,
                postalCode = None,
                telephoneNumber =
                  completeClaim.entryDeclarationDetails.map(s => s.declarationDetails.importerPhoneNumber.value),
                faxNumber = None,
                emailAddress =
                  completeClaim.entryDeclarationDetails.map(s => s.declarationDetails.importerEmailAddress.value)
              )
            ),
            VATDetails = None
          )
        )

        val declarant = sample[MRNInformation].copy(
          EORI = submitClaimRequest.signedInUserDetails.eori.value,
          legalName = completeDeclarationDetailsAnswer.declarationDetails.declarantName,
          establishmentAddress = Address.empty,
          contactDetails = ContactInformation(
            contactPerson = Some(completeDeclarationDetailsAnswer.declarationDetails.declarantName),
            addressLine1 = None,
            addressLine2 = None,
            addressLine3 = None,
            street = None,
            city = None,
            countryCode = None,
            postalCode = None,
            telephoneNumber = Some(completeDeclarationDetailsAnswer.declarationDetails.declarantPhoneNumber.value),
            faxNumber = None,
            emailAddress = Some(completeDeclarationDetailsAnswer.declarationDetails.declarantEmailAddress.value)
          )
        )

        val consignee = sample[MRNInformation].copy(
          EORI = submitClaimRequest.signedInUserDetails.eori.value,
          legalName = completeDeclarationDetailsAnswer.declarationDetails.declarantName,
          establishmentAddress = Address.empty,
          contactDetails = ContactInformation(
            contactPerson = Some(completeDeclarationDetailsAnswer.declarationDetails.importerName),
            addressLine1 = None,
            addressLine2 = None,
            addressLine3 = None,
            street = None,
            city = None,
            countryCode = None,
            postalCode = None,
            telephoneNumber = Some(completeDeclarationDetailsAnswer.declarationDetails.importerPhoneNumber.value),
            faxNumber = None,
            emailAddress = Some(completeDeclarationDetailsAnswer.declarationDetails.importerEmailAddress.value)
          )
        )

        val bankDetails = uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.BankDetails(
          consigneeBankDetails = None,
          declarantBankDetails = Some(
            BankDetail(
              completeBankAccountDetailAnswer.accountName.value,
              completeBankAccountDetailAnswer.sortCode.value,
              completeBankAccountDetailAnswer.accountNumber.value
            )
          )
        )

        val entryDetails = EntryDetail(
          Some("666541198B49856762"),
          Some("20201206"),
          None,
          Some(true),
          Some(declarant),
          None,
          Some(consignee),
          Some(bankDetails),
          Some(
            List(
              eis.claim.NdrcDetails(
                claim.paymentMethod,
                claim.paymentReference,
                None,
                claim.taxCode,
                "20.00",
                Some("10.00")
              )
            )
          )
        )

        val requestDetailA = RequestDetailA(
          CDFPayService = "NDRC",
          dateReceived = Some("2021-03-08T13:57:53Z"),
          claimType = Some("C285"),
          caseType = Some("Individual"),
          customDeclarationType = Some("Entry"),
          declarationMode = Some("Parent Declaration"),
          claimDate = Some("2021-03-08T13:57:53Z"),
          claimAmountTotal = Some("10.00"),
          disposalMethod = None,
          reimbursementMethod = Some("Bank Transfer"),
          claimant = Some("Importer"),
          payeeIndicator = Some("Importer"),
          newEORI = None,
          newDAN = None,
          authorityTypeProvided = None,
          claimantEORI = Some(submitClaimRequest.signedInUserDetails.eori.value),
          claimantEmailAddress = submitClaimRequest.signedInUserDetails.email.map(email => email.value),
          goodsDetails = Some(
            GoodsDetails(
              None,
              Some("Yes"),
              None,
              Some(completeClaim.commodityDetails.value)
            )
          ),
          basisOfClaim = Some("Duty Suspension"),
          EORIDetails = Some(eoriDetails)
        )

        val requestDetailB       = sample[RequestDetailB].copy(
          MRNDetails = None,
          duplicateMRNDetails = None,
          entryDetails = Some(List(entryDetails)),
          duplicateEntryDetails = None
        )
        val postNewClaimsRequest = PostNewClaimsRequest(
          requestCommon = requestCommon,
          requestDetail = RequestDetail(requestDetailA, requestDetailB)
        )

        val eisSubmitClaimRequest = EisSubmitClaimRequest(postNewClaimsRequest)

        inSequence {
          mockGenerateReceiptDate("2021-03-08T13:57:53Z")
          mockGenerateUUID(correlationId)
          mockGenerateIsoLocalDate("2021-03-08T13:57:53Z")
        }

        transformer.toEisSubmitClaimRequest(submitClaimRequest) shouldBe Right(eisSubmitClaimRequest)
      }

      "make an EIS submit claim request for a valid mrn number claim" in {

        val displayResponseDetail =
          sample[DisplayResponseDetail].copy(
            acceptanceDate = "21 March 2021",
            declarantDetails = DeclarantDetails(
              declarantEORI = "AA12345678901234Z",
              legalName = "Declarant Legal Name",
              establishmentAddress = EstablishmentAddress(
                addressLine1 = "line 1",
                addressLine2 = Some("line 2"),
                addressLine3 = Some("line 3"),
                postalCode = Some("AC1 ME"),
                countryCode = "GB"
              ),
              contactDetails = Some(
                ContactDetails(
                  contactName = Some("John Smith"),
                  addressLine1 = Some("line 1"),
                  addressLine2 = Some("line 2"),
                  addressLine3 = Some("line 3"),
                  addressLine4 = Some("line 4"),
                  postalCode = Some("AC2 MN"),
                  countryCode = Some("GB"),
                  telephone = Some("tel"),
                  emailAddress = Some("email")
                )
              )
            ),
            consigneeDetails = Some(
              ConsigneeDetails(
                consigneeEORI = "AA12345678901234Z",
                legalName = "Consignee Legal Name",
                establishmentAddress = EstablishmentAddress(
                  addressLine1 = "line 1",
                  addressLine2 = Some("line 2"),
                  addressLine3 = Some("line 3"),
                  postalCode = Some("AC1 ME"),
                  countryCode = "GB"
                ),
                contactDetails = Some(
                  ContactDetails(
                    contactName = Some("John Smith"),
                    addressLine1 = Some("line 1"),
                    addressLine2 = Some("line 2"),
                    addressLine3 = Some("line 3"),
                    addressLine4 = Some("line 4"),
                    postalCode = Some("AC2 MN"),
                    countryCode = Some("GB"),
                    telephone = Some("tel"),
                    emailAddress = Some("email")
                  )
                )
              )
            )
          )

        val displayDeclaration = sample[DisplayDeclaration].copy(displayResponseDetail = displayResponseDetail)

        val claim = sample[Claim].copy(
          paymentMethod = "001",
          taxCode = "A00",
          paidAmount = BigDecimal(20.00),
          claimAmount = BigDecimal(10.00),
          paymentReference = "pay-ref"
        )

        val claimsAnswer = ClaimsAnswer(claim)

        val completeBankAccountDetailAnswer: BankAccountDetails = sample[BankAccountDetails]

        val basisOfClaimAnswer = BasisOfClaim.DutySuspension

        val declarantTypeAnswer       = DeclarantTypeAnswer.Importer
        val movevementReferenceNumber = sample[MovementReferenceNumber]
          .copy(value = Right(MRN("10ABCDEFGHIJKLMNO0")))

        val completeClaim =
          sample[CompleteC285Claim].copy(
            movementReferenceNumber = movevementReferenceNumber,
            declarantTypeAnswer = declarantTypeAnswer,
            maybeBasisOfClaimAnswer = Some(basisOfClaimAnswer),
            maybeCompleteBankAccountDetailAnswer = Some(completeBankAccountDetailAnswer),
            claimsAnswer = claimsAnswer,
            maybeCompleteReasonAndBasisOfClaimAnswer = None,
            maybeCompleteDuplicateDeclarationDetailsAnswer = None,
            maybeCompleteDeclarationDetailsAnswer = None,
            maybeDisplayDeclaration = Some(displayDeclaration)
          )

        val submitClaimRequest = sample[SubmitClaimRequest].copy(completeClaim = completeClaim)
        val correlationId      = UUID.randomUUID()

        val bankDetails = uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.BankDetails(
          consigneeBankDetails = None,
          declarantBankDetails = Some(
            BankDetail(
              completeBankAccountDetailAnswer.accountName.value,
              completeBankAccountDetailAnswer.sortCode.value,
              completeBankAccountDetailAnswer.accountNumber.value
            )
          )
        )

        val entryEoriDetails = EoriDetails(
          agentEORIDetails = EORIInformation(
            EORINumber = completeClaim.declarantDetails.map(s => s.declarantEORI).getOrElse(""),
            CDSFullName = completeClaim.declarantDetails.map(s => s.legalName),
            legalEntityType = None,
            EORIStartDate = None,
            CDSEstablishmentAddress = Address(
              contactPerson = None,
              addressLine1 = completeClaim.declarantDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine1)),
              addressLine2 = completeClaim.declarantDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine2)),
              AddressLine3 = completeClaim.declarantDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine3)),
              street = Some(
                s"${completeClaim.declarantDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine1)).getOrElse("")} ${completeClaim.declarantDetails
                  .flatMap(s => s.contactDetails.flatMap(f => f.addressLine2))
                  .getOrElse("")}"
              ),
              city = completeClaim.declarantDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine3)),
              countryCode = completeClaim.declarantDetails
                .flatMap(s => s.contactDetails.flatMap(f => f.countryCode))
                .getOrElse("GB"),
              postalCode = completeClaim.declarantDetails.flatMap(s => s.contactDetails.flatMap(f => f.postalCode)),
              telephone = completeClaim.declarantDetails.flatMap(s => s.contactDetails.flatMap(f => f.telephone)),
              emailAddress = completeClaim.declarantDetails.flatMap(s => s.contactDetails.flatMap(f => f.emailAddress))
            ),
            contactInformation = Some(
              ContactInformation(
                contactPerson =
                  completeClaim.declarantDetails.flatMap(s => s.contactDetails.flatMap(f => f.contactName)),
                addressLine1 =
                  completeClaim.declarantDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine1)),
                addressLine2 =
                  completeClaim.declarantDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine2)),
                addressLine3 =
                  completeClaim.declarantDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine3)),
                street = Some(
                  s"${completeClaim.declarantDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine1))} ${completeClaim.declarantDetails
                    .flatMap(s => s.contactDetails.flatMap(f => f.addressLine2))}"
                ),
                city = completeClaim.declarantDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine3)),
                countryCode = completeClaim.declarantDetails.flatMap(s => s.contactDetails.flatMap(f => f.countryCode)),
                postalCode = completeClaim.declarantDetails.flatMap(s => s.contactDetails.flatMap(f => f.postalCode)),
                telephoneNumber =
                  completeClaim.declarantDetails.flatMap(s => s.contactDetails.flatMap(f => f.telephone)),
                faxNumber = None,
                emailAddress =
                  completeClaim.declarantDetails.flatMap(s => s.contactDetails.flatMap(f => f.emailAddress))
              )
            ),
            VATDetails = None
          ),
          importerEORIDetails = EORIInformation(
            EORINumber = completeClaim.consigneeDetails.map(s => s.consigneeEORI).getOrElse(""),
            CDSFullName = completeClaim.consigneeDetails.map(s => s.legalName),
            legalEntityType = None,
            EORIStartDate = None,
            CDSEstablishmentAddress = Address(
              contactPerson = None,
              addressLine1 = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine1)),
              addressLine2 = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine2)),
              AddressLine3 = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine3)),
              street = Some(
                s"${completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine1)).getOrElse("")} ${completeClaim.consigneeDetails
                  .flatMap(s => s.contactDetails.flatMap(f => f.addressLine2))
                  .getOrElse("")}"
              ),
              city = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine3)),
              countryCode = completeClaim.consigneeDetails
                .flatMap(s => s.contactDetails.flatMap(f => f.countryCode))
                .getOrElse("GB"),
              postalCode = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.postalCode)),
              telephone = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.telephone)),
              emailAddress = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.emailAddress))
            ),
            contactInformation = Some(
              ContactInformation(
                contactPerson =
                  completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.contactName)),
                addressLine1 =
                  completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine1)),
                addressLine2 =
                  completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine2)),
                addressLine3 =
                  completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine3)),
                street = Some(
                  s"${completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine1))} ${completeClaim.consigneeDetails
                    .flatMap(s => s.contactDetails.flatMap(f => f.addressLine2))}"
                ),
                city = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine3)),
                countryCode = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.countryCode)),
                postalCode = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.postalCode)),
                telephoneNumber =
                  completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.telephone)),
                faxNumber = None,
                emailAddress =
                  completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.emailAddress))
              )
            ),
            VATDetails = None
          )
        )

        val declarationDetails = MRNInformation(
          EORI = displayDeclaration.displayResponseDetail.declarantDetails.declarantEORI,
          legalName = displayDeclaration.displayResponseDetail.declarantDetails.legalName,
          establishmentAddress = Address(
            contactPerson = Some(displayDeclaration.displayResponseDetail.declarantDetails.legalName),
            addressLine1 =
              displayDeclaration.displayResponseDetail.declarantDetails.contactDetails.flatMap(s => s.addressLine1),
            addressLine2 =
              displayDeclaration.displayResponseDetail.declarantDetails.contactDetails.flatMap(s => s.addressLine2),
            AddressLine3 = None,
            street = displayDeclaration.displayResponseDetail.declarantDetails.contactDetails
              .flatMap(s => s.addressLine1)
              .flatMap(line1 =>
                displayDeclaration.displayResponseDetail.declarantDetails.contactDetails
                  .flatMap(s => s.addressLine2)
                  .map(line2 => s"$line1 $line2")
              ),
            city =
              displayDeclaration.displayResponseDetail.declarantDetails.contactDetails.flatMap(s => s.addressLine3),
            countryCode = displayDeclaration.displayResponseDetail.declarantDetails.contactDetails
              .flatMap(s => s.countryCode)
              .getOrElse("GB"),
            postalCode =
              displayDeclaration.displayResponseDetail.declarantDetails.contactDetails.flatMap(s => s.postalCode),
            telephone =
              displayDeclaration.displayResponseDetail.declarantDetails.contactDetails.flatMap(s => s.telephone),
            emailAddress =
              displayDeclaration.displayResponseDetail.declarantDetails.contactDetails.flatMap(s => s.emailAddress)
          ),
          contactDetails = ContactInformation(
            contactPerson =
              displayDeclaration.displayResponseDetail.declarantDetails.contactDetails.flatMap(s => s.contactName),
            addressLine1 =
              displayDeclaration.displayResponseDetail.declarantDetails.contactDetails.flatMap(s => s.addressLine1),
            addressLine2 =
              displayDeclaration.displayResponseDetail.declarantDetails.contactDetails.flatMap(s => s.addressLine2),
            addressLine3 =
              displayDeclaration.displayResponseDetail.declarantDetails.contactDetails.flatMap(s => s.addressLine3),
            street = displayDeclaration.displayResponseDetail.declarantDetails.contactDetails
              .flatMap(s => s.addressLine1)
              .flatMap(line1 =>
                displayDeclaration.displayResponseDetail.declarantDetails.contactDetails
                  .flatMap(s => s.addressLine2)
                  .map(line2 => s"$line1 $line2")
              ),
            city =
              displayDeclaration.displayResponseDetail.declarantDetails.contactDetails.flatMap(s => s.addressLine4),
            countryCode =
              displayDeclaration.displayResponseDetail.declarantDetails.contactDetails.flatMap(s => s.countryCode),
            postalCode =
              displayDeclaration.displayResponseDetail.declarantDetails.contactDetails.flatMap(s => s.postalCode),
            telephoneNumber =
              displayDeclaration.displayResponseDetail.declarantDetails.contactDetails.flatMap(s => s.telephone),
            faxNumber = None,
            emailAddress =
              displayDeclaration.displayResponseDetail.declarantDetails.contactDetails.flatMap(s => s.emailAddress)
          )
        )

        val mrnDetails = MrnDetail(
          MRNNumber = Some(displayDeclaration.displayResponseDetail.declarationId),
          acceptanceDate = Some("20210321"),
          declarantReferenceNumber = displayDeclaration.displayResponseDetail.declarantReferenceNumber,
          mainDeclarationReference = Some(true),
          procedureCode = Some(displayDeclaration.displayResponseDetail.procedureCode),
          declarantDetails = Some(declarationDetails),
          accountDetails = None,
          consigneeDetails = Some(
            MRNInformation(
              EORI = "CEORI",
              legalName = "dfdfd",
              establishmentAddress = Address(
                contactPerson = None,
                addressLine1 = Some("line1"),
                addressLine2 = Some("line2"),
                AddressLine3 = Some("line3"),
                street = Some("street"),
                city = Some("city"),
                countryCode = "GB",
                postalCode = Some("postal code"),
                telephone = Some("tel"),
                emailAddress = Some("email")
              ),
              contactDetails = ContactInformation(
                contactPerson = Some("contact info"),
                addressLine1 = Some("l1"),
                addressLine2 = Some("l2"),
                addressLine3 = Some("l3"),
                street = Some("street"),
                city = Some("city"),
                countryCode = Some("GB"),
                postalCode = Some("AC2 MN"),
                telephoneNumber = Some("tel"),
                faxNumber = None,
                emailAddress = Some("email")
              )
            )
          ),
          bankDetails = Some(bankDetails),
          NDRCDetails = Some(
            List(
              eis.claim.NdrcDetails(
                claim.paymentMethod,
                claim.paymentReference,
                None,
                claim.taxCode,
                "20.00",
                Some("10.00")
              )
            )
          )
        )

        val requestCommon = sample[RequestCommon].copy(
          originatingSystem = "MDTP",
          receiptDate = "2018-08-08T13:57:53Z",
          acknowledgementReference = correlationId.toString
        )

        val requestDetailA = RequestDetailA(
          CDFPayService = "NDRC",
          dateReceived = Some("2021-03-08T13:57:53Z"),
          claimType = Some("C285"),
          caseType = Some("Individual"),
          customDeclarationType = Some("MRN"),
          declarationMode = Some("Parent Declaration"),
          claimDate = Some("2021-03-08T13:57:53Z"),
          claimAmountTotal = Some("10.00"),
          disposalMethod = None,
          reimbursementMethod = Some("Bank Transfer"),
          claimant = Some("Importer"),
          payeeIndicator = Some("Importer"),
          newEORI = None,
          newDAN = None,
          authorityTypeProvided = None,
          claimantEORI = Some(submitClaimRequest.signedInUserDetails.eori.value),
          claimantEmailAddress = submitClaimRequest.signedInUserDetails.email.map(email => email.value),
          goodsDetails = Some(
            GoodsDetails(
              None,
              Some("Yes"),
              None,
              Some(completeClaim.commodityDetails.value)
            )
          ),
          basisOfClaim = Some("Duty Suspension"),
          EORIDetails = Some(entryEoriDetails)
        )

        val requestDetailB =
          sample[RequestDetailB].copy(
            MRNDetails = Some(List(mrnDetails)),
            duplicateMRNDetails = None,
            entryDetails = None,
            duplicateEntryDetails = None
          )

        val postNewClaimsRequest = PostNewClaimsRequest(
          requestCommon = requestCommon,
          requestDetail = RequestDetail(requestDetailA, requestDetailB)
        )

        val _ = EisSubmitClaimRequest(postNewClaimsRequest)

        inSequence {
          mockGenerateReceiptDate("2021-03-08T13:57:53Z")
          mockGenerateUUID(correlationId)
          mockGenerateIsoLocalDate("2021-03-08T13:57:53Z")
        }

        transformer.toEisSubmitClaimRequest(submitClaimRequest)
        1 shouldBe 1

      }

      def getContactDetails(prefix: String): ContactDetails =
        ContactDetails(
          contactName = Some(s"$prefix.JohnSmith"),
          addressLine1 = Some(s"$prefix.addressLine1"),
          addressLine2 = Some(s"$prefix.addressLine2"),
          addressLine3 = Some(s"$prefix.addressLine3"),
          addressLine4 = Some(s"$prefix.addressLine4"),
          postalCode = Some(s"$prefix.postalCode"),
          countryCode = Some(s"$prefix.countryCode"),
          telephone = Some(s"$prefix.telephone"),
          emailAddress = Some(s"$prefix.email")
        )

      def getEstablishmentAddress(prefix: String): EstablishmentAddress =
        EstablishmentAddress(
          addressLine1 = s"$prefix.addressLine1",
          addressLine2 = Some(s"$prefix.addressLine2"),
          addressLine3 = Some(s"$prefix.addressLine3"),
          postalCode = Some(s"$prefix.postalCode"),
          countryCode = s"$prefix.countryCode"
        )

      def getNonUkAddress(prefix: String): NonUkAddress =
        NonUkAddress(
          line1 = s"$prefix.line1",
          line2 = Some(s"$prefix.line2"),
          line3 = Some(s"$prefix.line3"),
          line4 = s"$prefix.line4",
          line5 = Some(s"$prefix.line5"),
          postcode = Some(s"$prefix.postcode"),
          country = Country(s"$prefix.country")
        )

      def getAcc14Response(): DisplayResponseDetail =
        sample[DisplayResponseDetail].copy(
          acceptanceDate = "21 March 2021",
          declarantDetails = DeclarantDetails(
            declarantEORI = "AA12345678901234Z",
            legalName = "Declarant Legal Name",
            establishmentAddress = getEstablishmentAddress("DeclarantDetails.establishmentAddress"),
            contactDetails = Some(getContactDetails("DeclarantDetails.contactDetails"))
          ),
          consigneeDetails = Some(
            ConsigneeDetails(
              consigneeEORI = "AA12345678901234Z",
              legalName = "Consignee Legal Name",
              establishmentAddress = getEstablishmentAddress("ConsigneeDetails.establishmentAddress"),
              contactDetails = Some(getContactDetails("ConsigneeDetails.contactDetails"))
            )
          )
        )

      def getClaimAmounts(): Claim =
        sample[Claim].copy(
          paymentMethod = "001",
          taxCode = "A00",
          paidAmount = BigDecimal(20.00),
          claimAmount = BigDecimal(10.00),
          paymentReference = "pay-ref"
        )

      //Check how ACC14 data is placed in the TPI05 request. These mappings are indipendent of user input
      def checkFixedAcc14ToTpi05Mapping(acc14: DisplayResponseDetail, tpi05: RequestDetail): Unit = {
        val tpi05EoriDetails = tpi05.requestDetailA.EORIDetails.getOrElse(fail("No Eori Details"))
        val tpi05MRNDetail   = tpi05.requestDetailB.MRNDetails
          .getOrElse(fail("No MRNDetails"))
          .headOption
          .getOrElse(fail("Empty MRNDetails List"))
        val acc14Consignee   = acc14.consigneeDetails.getOrElse(fail("No Consignee Details"))
        val acc14Declarant   = acc14.declarantDetails

        //SCID-1
        tpi05EoriDetails.importerEORIDetails.EORINumber                           shouldBe acc14Consignee.consigneeEORI
        tpi05MRNDetail.consigneeDetails.getOrElse(fail()).EORI                    shouldBe acc14Consignee.consigneeEORI
        //SCID-2
        tpi05EoriDetails.importerEORIDetails.CDSFullName.getOrElse(fail())        shouldBe acc14Consignee.legalName
        tpi05MRNDetail.consigneeDetails.getOrElse(fail).legalName                 shouldBe acc14Consignee.legalName
        //SCID-3
        tpi05EoriDetails.importerEORIDetails.CDSEstablishmentAddress.addressLine1
          .getOrElse(fail)                                                        shouldBe acc14Consignee.establishmentAddress.addressLine1
        tpi05MRNDetail.consigneeDetails
          .getOrElse(fail)
          .establishmentAddress
          .addressLine1
          .getOrElse(fail)                                                        shouldBe acc14Consignee.establishmentAddress.addressLine1
        //SCID-4
        tpi05EoriDetails.importerEORIDetails.CDSEstablishmentAddress.addressLine2 shouldBe acc14Consignee.establishmentAddress.addressLine2
        tpi05MRNDetail.consigneeDetails
          .getOrElse(fail)
          .establishmentAddress
          .addressLine2                                                           shouldBe acc14Consignee.establishmentAddress.addressLine2
        //SCID-5
        tpi05EoriDetails.importerEORIDetails.CDSEstablishmentAddress.AddressLine3 shouldBe acc14Consignee.establishmentAddress.addressLine3
        tpi05MRNDetail.consigneeDetails
          .getOrElse(fail)
          .establishmentAddress
          .AddressLine3                                                           shouldBe acc14Consignee.establishmentAddress.addressLine3
        //SCID-6
        tpi05EoriDetails.importerEORIDetails.CDSEstablishmentAddress.postalCode   shouldBe acc14Consignee.establishmentAddress.postalCode
        tpi05MRNDetail.consigneeDetails
          .getOrElse(fail)
          .establishmentAddress
          .postalCode                                                             shouldBe acc14Consignee.establishmentAddress.postalCode
        //SCID-7
        tpi05EoriDetails.importerEORIDetails.CDSEstablishmentAddress.countryCode  shouldBe acc14Consignee.establishmentAddress.countryCode
        tpi05MRNDetail.consigneeDetails
          .getOrElse(fail)
          .establishmentAddress
          .countryCode                                                            shouldBe acc14Consignee.establishmentAddress.countryCode

        //SCID-8
        tpi05EoriDetails.importerEORIDetails.contactInformation
          .getOrElse(fail)
          .contactPerson shouldBe acc14Consignee.contactDetails.getOrElse(fail).contactName
        tpi05MRNDetail.consigneeDetails
          .getOrElse(fail)
          .contactDetails
          .contactPerson shouldBe acc14Consignee.contactDetails.getOrElse(fail).contactName
        //SCID-9
        tpi05EoriDetails.importerEORIDetails.contactInformation
          .getOrElse(fail)
          .addressLine1  shouldBe acc14Consignee.contactDetails.getOrElse(fail).addressLine1
        tpi05MRNDetail.consigneeDetails
          .getOrElse(fail)
          .contactDetails
          .addressLine1  shouldBe acc14Consignee.contactDetails.getOrElse(fail).addressLine1
        //SCID-10
        tpi05EoriDetails.importerEORIDetails.contactInformation
          .getOrElse(fail)
          .addressLine2  shouldBe acc14Consignee.contactDetails.getOrElse(fail).addressLine2
        tpi05MRNDetail.consigneeDetails
          .getOrElse(fail)
          .contactDetails
          .addressLine2  shouldBe acc14Consignee.contactDetails.getOrElse(fail).addressLine2
        //SCID-11
        val street = acc14Consignee.contactDetails
          .getOrElse(fail)
          .addressLine1
          .getOrElse("A") + " " + acc14Consignee.contactDetails.getOrElse(fail).addressLine2.getOrElse("B")
        tpi05EoriDetails.importerEORIDetails.contactInformation.getOrElse(fail).street shouldBe Some(street)
        //SCID-12
        tpi05EoriDetails.importerEORIDetails.contactInformation
          .getOrElse(fail)
          .addressLine3                                                                shouldBe acc14Consignee.contactDetails.getOrElse(fail).addressLine3
        tpi05EoriDetails.importerEORIDetails.contactInformation
          .getOrElse(fail)
          .city                                                                        shouldBe acc14Consignee.contactDetails.getOrElse(fail).addressLine3
        tpi05MRNDetail.consigneeDetails
          .getOrElse(fail)
          .contactDetails
          .addressLine3                                                                shouldBe acc14Consignee.contactDetails.getOrElse(fail).addressLine3
        //SCID-13
        tpi05EoriDetails.importerEORIDetails.contactInformation
          .getOrElse(fail)
          .postalCode                                                                  shouldBe acc14Consignee.contactDetails.flatMap(_.postalCode)
        tpi05MRNDetail.consigneeDetails.getOrElse(fail).contactDetails.postalCode      shouldBe acc14Consignee.contactDetails
          .flatMap(_.postalCode)
        //SCID-14
        tpi05EoriDetails.importerEORIDetails.contactInformation
          .getOrElse(fail)
          .countryCode                                                                 shouldBe acc14Consignee.contactDetails.getOrElse(fail).countryCode
        tpi05MRNDetail.consigneeDetails
          .getOrElse(fail)
          .contactDetails
          .countryCode                                                                 shouldBe acc14Consignee.contactDetails.getOrElse(fail).countryCode
        //SCID-15
        tpi05EoriDetails.importerEORIDetails.contactInformation
          .getOrElse(fail)
          .telephoneNumber                                                             shouldBe acc14Consignee.contactDetails.getOrElse(fail).telephone
        tpi05MRNDetail.consigneeDetails
          .getOrElse(fail)
          .contactDetails
          .telephoneNumber                                                             shouldBe acc14Consignee.contactDetails.getOrElse(fail).telephone
        //SCID-16
        tpi05EoriDetails.importerEORIDetails.contactInformation
          .getOrElse(fail)
          .emailAddress                                                                shouldBe acc14Consignee.contactDetails.getOrElse(fail).emailAddress
        tpi05MRNDetail.consigneeDetails
          .getOrElse(fail)
          .contactDetails
          .emailAddress                                                                shouldBe acc14Consignee.contactDetails.getOrElse(fail).emailAddress

        //SCDD-1
        tpi05MRNDetail.declarantDetails.map(_.EORI)                                  shouldBe Some(acc14Declarant.declarantEORI)
        //SCDD-2
        tpi05MRNDetail.declarantDetails.map(_.legalName)                             shouldBe Some(acc14Declarant.legalName)
        //SCDD-3
        tpi05MRNDetail.declarantDetails.flatMap(_.establishmentAddress.addressLine1) shouldBe Some(
          acc14Declarant.establishmentAddress.addressLine1
        )
        //SCDD-4
        tpi05MRNDetail.declarantDetails.flatMap(
          _.establishmentAddress.addressLine2
        )                                                                            shouldBe acc14Declarant.establishmentAddress.addressLine2
        //SCDD-5
        tpi05MRNDetail.declarantDetails.flatMap(
          _.establishmentAddress.AddressLine3
        )                                                                            shouldBe acc14Declarant.establishmentAddress.addressLine3
        //SCDD-6
        tpi05MRNDetail.declarantDetails.flatMap(
          _.establishmentAddress.postalCode
        )                                                                            shouldBe acc14Declarant.establishmentAddress.postalCode
        //SCDD-7
        tpi05MRNDetail.declarantDetails.map(_.establishmentAddress.countryCode)      shouldBe Some(
          acc14Declarant.establishmentAddress.countryCode
        )
        //SCDD-8
        tpi05MRNDetail.declarantDetails.flatMap(_.contactDetails.contactPerson)      shouldBe acc14Declarant.contactDetails
          .flatMap(_.contactName)
        //SCDD-9
        tpi05MRNDetail.declarantDetails.flatMap(_.contactDetails.addressLine1)       shouldBe acc14Declarant.contactDetails
          .flatMap(_.addressLine1)
        //SCDD-10
        tpi05MRNDetail.declarantDetails.flatMap(_.contactDetails.addressLine2)       shouldBe acc14Declarant.contactDetails
          .flatMap(_.addressLine2)
        //SCDD-11
        val street2 =
          acc14Declarant.contactDetails.flatMap(_.addressLine1).getOrElse("X") + " " + acc14Declarant.contactDetails
            .flatMap(_.addressLine2)
            .getOrElse("Y")
        tpi05MRNDetail.declarantDetails.flatMap(_.contactDetails.street)          shouldBe Some(street2)
        //SCDD-12
        tpi05MRNDetail.declarantDetails.flatMap(_.contactDetails.city)            shouldBe acc14Declarant.contactDetails.flatMap(
          _.addressLine3
        )
        //SCDD-13
        tpi05MRNDetail.declarantDetails.flatMap(_.contactDetails.postalCode)      shouldBe acc14Declarant.contactDetails
          .flatMap(_.postalCode)
        //SCDD-14
        tpi05MRNDetail.declarantDetails.flatMap(_.contactDetails.countryCode)     shouldBe acc14Declarant.contactDetails
          .flatMap(_.countryCode)
        //SCDD-15
        tpi05MRNDetail.declarantDetails.flatMap(_.contactDetails.telephoneNumber) shouldBe acc14Declarant.contactDetails
          .flatMap(_.telephone)
        //SCDD-16
        tpi05MRNDetail.declarantDetails.flatMap(_.contactDetails.emailAddress)    shouldBe acc14Declarant.contactDetails
          .flatMap(_.emailAddress)
        ()
      }

      def checkDetailsRegisteredWithCdsToEstablishmentAddressMapping(
        establishmentAddress: Address,
        detailsRegisteredWithCds: DetailsRegisteredWithCdsFormData
      ): Unit = {
        establishmentAddress.contactPerson shouldBe Some(detailsRegisteredWithCds.fullName)
        establishmentAddress.addressLine1  shouldBe Some(detailsRegisteredWithCds.contactAddress.line1)
        establishmentAddress.addressLine2  shouldBe detailsRegisteredWithCds.contactAddress.line2
        establishmentAddress.AddressLine3  shouldBe detailsRegisteredWithCds.contactAddress.line3
        val street1 =
          detailsRegisteredWithCds.contactAddress.line1 + " " + detailsRegisteredWithCds.contactAddress.line2
            .getOrElse("G")
        establishmentAddress.street       shouldBe Some(street1)
        establishmentAddress.city         shouldBe Some(detailsRegisteredWithCds.contactAddress.line4)
        establishmentAddress.postalCode   shouldBe detailsRegisteredWithCds.contactAddress.postcode
        establishmentAddress.countryCode  shouldBe detailsRegisteredWithCds.contactAddress.country.code
        establishmentAddress.emailAddress shouldBe Some(detailsRegisteredWithCds.emailAddress.value)
        establishmentAddress.telephone    shouldBe None
        ()
      }

      "valid mrn number claim, DeclarantType: Importer, with filled out claimantDetailsAsImporterCompanyAnswer" in {
        val declarantType                          = DeclarantTypeAnswer.Importer
        val claimantDetailsAsImporterCompanyAnswer =
          sample[ContactDetailsFormData].copy(contactAddress = getNonUkAddress("frontend.company"))

        val acc14                                               = getAcc14Response()
        val displayDeclaration                                  = sample[DisplayDeclaration].copy(displayResponseDetail = acc14)
        val claim                                               = getClaimAmounts()
        val claimsAnswer                                        = ClaimsAnswer(claim)
        val completeBankAccountDetailAnswer: BankAccountDetails = sample[BankAccountDetails]
        val basisOfClaimAnswer                                  = BasisOfClaim.DutySuspension
        val declarantTypeAnswer                                 = declarantType
        val completeMovementReferenceNumberAnswer               = sample[MovementReferenceNumber]
          .copy(value = Right(MRN("10ABCDEFGHIJKLMNO0")))
        val detailsRegisteredWithCds                            =
          sample[DetailsRegisteredWithCdsFormData].copy(contactAddress = getNonUkAddress("frontend.induvidual"))

        val completeClaim =
          sample[CompleteC285Claim].copy(
            movementReferenceNumber = completeMovementReferenceNumberAnswer,
            declarantTypeAnswer = declarantTypeAnswer,
            completeDetailsRegisteredWithCdsAnswer = CompleteDetailsRegisteredWithCdsAnswer(detailsRegisteredWithCds),
            maybeContactDetailsAnswer = Some(CompleteContactDetailsAnswer(claimantDetailsAsImporterCompanyAnswer)),
            maybeBasisOfClaimAnswer = Some(basisOfClaimAnswer),
            maybeCompleteBankAccountDetailAnswer = Some(completeBankAccountDetailAnswer),
            claimsAnswer = claimsAnswer,
            maybeCompleteReasonAndBasisOfClaimAnswer = None,
            maybeCompleteDuplicateDeclarationDetailsAnswer = None,
            maybeCompleteDeclarationDetailsAnswer = None,
            maybeDisplayDeclaration = Some(displayDeclaration),
            maybeDuplicateDisplayDeclaration = None
          )

        val submitClaimRequest = sample[SubmitClaimRequest].copy(completeClaim = completeClaim)
        val correlationId      = UUID.randomUUID()

        inSequence {
          mockGenerateReceiptDate("2021-03-08T13:57:53Z")
          mockGenerateUUID(correlationId)
          mockGenerateIsoLocalDate("2021-03-08T13:57:53Z")
        }
        val tpi05Claim       = transformer.toEisSubmitClaimRequest(submitClaimRequest)
        val tpi05            = tpi05Claim.getOrElse(fail("No Claim")).postNewClaimsRequest.requestDetail
        val tpi05EoriDetails = tpi05.requestDetailA.EORIDetails.getOrElse(fail("No EoriDetails")).agentEORIDetails

        checkFixedAcc14ToTpi05Mapping(acc14, tpi05)

        checkDetailsRegisteredWithCdsToEstablishmentAddressMapping(
          tpi05EoriDetails.CDSEstablishmentAddress,
          detailsRegisteredWithCds
        )

        val tpi05ContactInfo = tpi05EoriDetails.contactInformation.getOrElse(fail)
        tpi05ContactInfo.contactPerson shouldBe Some(claimantDetailsAsImporterCompanyAnswer.companyName)
        tpi05ContactInfo.addressLine1  shouldBe Some(claimantDetailsAsImporterCompanyAnswer.contactAddress.line1)
        tpi05ContactInfo.addressLine2  shouldBe claimantDetailsAsImporterCompanyAnswer.contactAddress.line2
        tpi05ContactInfo.addressLine3  shouldBe claimantDetailsAsImporterCompanyAnswer.contactAddress.line3
        val street2 =
          claimantDetailsAsImporterCompanyAnswer.contactAddress.line1 + " " + claimantDetailsAsImporterCompanyAnswer.contactAddress.line2
            .getOrElse("B")
        tpi05ContactInfo.street          shouldBe Some(street2)
        tpi05ContactInfo.city            shouldBe Some(claimantDetailsAsImporterCompanyAnswer.contactAddress.line4)
        tpi05ContactInfo.postalCode      shouldBe claimantDetailsAsImporterCompanyAnswer.contactAddress.postcode
        tpi05ContactInfo.countryCode     shouldBe Some(claimantDetailsAsImporterCompanyAnswer.contactAddress.country.code)
        tpi05ContactInfo.emailAddress    shouldBe Some(claimantDetailsAsImporterCompanyAnswer.emailAddress.value)
        tpi05ContactInfo.telephoneNumber shouldBe Some(claimantDetailsAsImporterCompanyAnswer.phoneNumber.value)
      }

      "valid mrn number claim, DeclarantType: Importer, No claimantDetailsAsImporterCompanyAnswer" in {
        val declarantType                               = DeclarantTypeAnswer.Importer
        val maybeClaimantDetailsAsImporterCompanyAnswer = None

        val acc14                                               = getAcc14Response()
        val displayDeclaration                                  = sample[DisplayDeclaration].copy(displayResponseDetail = acc14)
        val claim                                               = getClaimAmounts()
        val claimsAnswer                                        = ClaimsAnswer(claim)
        val completeBankAccountDetailAnswer: BankAccountDetails = sample[BankAccountDetails]
        val basisOfClaimAnswer                                  = BasisOfClaim.DutySuspension
        val declarantTypeAnswer                                 = declarantType
        val completeMovementReferenceNumberAnswer               = sample[MovementReferenceNumber]
          .copy(value = Right(MRN("10ABCDEFGHIJKLMNO0")))
        val detailsRegisteredWithCds                            =
          sample[DetailsRegisteredWithCdsFormData].copy(contactAddress = getNonUkAddress("frontend.induvidual"))

        val completeClaim =
          sample[CompleteC285Claim].copy(
            movementReferenceNumber = completeMovementReferenceNumberAnswer,
            declarantTypeAnswer = declarantTypeAnswer,
            completeDetailsRegisteredWithCdsAnswer = CompleteDetailsRegisteredWithCdsAnswer(detailsRegisteredWithCds),
            maybeContactDetailsAnswer = maybeClaimantDetailsAsImporterCompanyAnswer,
            maybeBasisOfClaimAnswer = Some(basisOfClaimAnswer),
            maybeCompleteBankAccountDetailAnswer = Some(completeBankAccountDetailAnswer),
            claimsAnswer = claimsAnswer,
            maybeCompleteReasonAndBasisOfClaimAnswer = None,
            maybeCompleteDuplicateDeclarationDetailsAnswer = None,
            maybeCompleteDeclarationDetailsAnswer = None,
            maybeDisplayDeclaration = Some(displayDeclaration),
            maybeDuplicateDisplayDeclaration = None
          )

        val submitClaimRequest = sample[SubmitClaimRequest].copy(completeClaim = completeClaim)
        val correlationId      = UUID.randomUUID()

        inSequence {
          mockGenerateReceiptDate("2021-03-08T13:57:53Z")
          mockGenerateUUID(correlationId)
          mockGenerateIsoLocalDate("2021-03-08T13:57:53Z")
        }
        val tpi05Claim       = transformer.toEisSubmitClaimRequest(submitClaimRequest)
        val tpi05            = tpi05Claim.getOrElse(fail("No Claim")).postNewClaimsRequest.requestDetail
        val tpi05EoriDetails = tpi05.requestDetailA.EORIDetails.getOrElse(fail("No EoriDetails")).agentEORIDetails

        checkFixedAcc14ToTpi05Mapping(acc14, tpi05)

        checkDetailsRegisteredWithCdsToEstablishmentAddressMapping(
          tpi05EoriDetails.CDSEstablishmentAddress,
          detailsRegisteredWithCds
        )

        val tpi05ContactInfo = tpi05EoriDetails.contactInformation.getOrElse(fail)
        val acc14Consignee   = acc14.consigneeDetails.getOrElse(fail("No Consignee"))
        tpi05ContactInfo.contactPerson shouldBe Some(acc14Consignee.legalName)
        tpi05ContactInfo.addressLine1  shouldBe acc14Consignee.contactDetails.flatMap(_.addressLine1)
        tpi05ContactInfo.addressLine2  shouldBe acc14Consignee.contactDetails.flatMap(_.addressLine2)
        tpi05ContactInfo.addressLine3  shouldBe acc14Consignee.contactDetails.flatMap(_.addressLine3)
        val street2 =
          acc14Consignee.contactDetails.flatMap(_.addressLine1).getOrElse("Q") + " " + acc14Consignee.contactDetails
            .flatMap(_.addressLine2)
            .getOrElse("W")
        tpi05ContactInfo.street          shouldBe Some(street2)
        tpi05ContactInfo.city            shouldBe acc14Consignee.contactDetails.flatMap(_.addressLine3)
        tpi05ContactInfo.postalCode      shouldBe acc14Consignee.contactDetails.flatMap(_.postalCode)
        tpi05ContactInfo.countryCode     shouldBe acc14Consignee.contactDetails.flatMap(_.countryCode)
        tpi05ContactInfo.emailAddress    shouldBe acc14Consignee.contactDetails.flatMap(_.emailAddress)
        tpi05ContactInfo.telephoneNumber shouldBe acc14Consignee.contactDetails.flatMap(_.telephone)
      }

      "valid mrn number claim, DeclarantType: AssociatedWithImporterCompany, with filled out claimantDetailsAsImporterCompanyAnswer" in {
        val declarantType                          = DeclarantTypeAnswer.AssociatedWithImporterCompany
        val claimantDetailsAsImporterCompanyAnswer =
          sample[ContactDetailsFormData].copy(contactAddress = getNonUkAddress("frontend.company"))

        val acc14                                               = getAcc14Response()
        val displayDeclaration                                  = sample[DisplayDeclaration].copy(displayResponseDetail = acc14)
        val claim                                               = getClaimAmounts()
        val claimsAnswer                                        = ClaimsAnswer(claim)
        val completeBankAccountDetailAnswer: BankAccountDetails = sample[BankAccountDetails]
        val basisOfClaimAnswer                                  = BasisOfClaim.DutySuspension
        val declarantTypeAnswer                                 = declarantType
        val completeMovementReferenceNumberAnswer               = sample[MovementReferenceNumber]
          .copy(value = Right(MRN("10ABCDEFGHIJKLMNO0")))
        val detailsRegisteredWithCds                            =
          sample[DetailsRegisteredWithCdsFormData].copy(contactAddress = getNonUkAddress("frontend.induvidual"))

        val completeClaim =
          sample[CompleteC285Claim].copy(
            movementReferenceNumber = completeMovementReferenceNumberAnswer,
            declarantTypeAnswer = declarantTypeAnswer,
            completeDetailsRegisteredWithCdsAnswer = CompleteDetailsRegisteredWithCdsAnswer(detailsRegisteredWithCds),
            maybeContactDetailsAnswer = Some(CompleteContactDetailsAnswer(claimantDetailsAsImporterCompanyAnswer)),
            maybeBasisOfClaimAnswer = Some(basisOfClaimAnswer),
            maybeCompleteBankAccountDetailAnswer = Some(completeBankAccountDetailAnswer),
            claimsAnswer = claimsAnswer,
            maybeCompleteReasonAndBasisOfClaimAnswer = None,
            maybeCompleteDuplicateDeclarationDetailsAnswer = None,
            maybeCompleteDeclarationDetailsAnswer = None,
            maybeDisplayDeclaration = Some(displayDeclaration),
            maybeDuplicateDisplayDeclaration = None
          )

        val submitClaimRequest = sample[SubmitClaimRequest].copy(completeClaim = completeClaim)
        val correlationId      = UUID.randomUUID()

        inSequence {
          mockGenerateReceiptDate("2021-03-08T13:57:53Z")
          mockGenerateUUID(correlationId)
          mockGenerateIsoLocalDate("2021-03-08T13:57:53Z")
        }
        val tpi05Claim       = transformer.toEisSubmitClaimRequest(submitClaimRequest)
        val tpi05            = tpi05Claim.getOrElse(fail("No Claim")).postNewClaimsRequest.requestDetail
        val tpi05EoriDetails = tpi05.requestDetailA.EORIDetails.getOrElse(fail("No EoriDetails")).agentEORIDetails

        checkFixedAcc14ToTpi05Mapping(acc14, tpi05)

        checkDetailsRegisteredWithCdsToEstablishmentAddressMapping(
          tpi05EoriDetails.CDSEstablishmentAddress,
          detailsRegisteredWithCds
        )

        val tpi05ContactInfo = tpi05EoriDetails.contactInformation.getOrElse(fail)
        tpi05ContactInfo.contactPerson shouldBe Some(claimantDetailsAsImporterCompanyAnswer.companyName)
        tpi05ContactInfo.addressLine1  shouldBe Some(claimantDetailsAsImporterCompanyAnswer.contactAddress.line1)
        tpi05ContactInfo.addressLine2  shouldBe claimantDetailsAsImporterCompanyAnswer.contactAddress.line2
        tpi05ContactInfo.addressLine3  shouldBe claimantDetailsAsImporterCompanyAnswer.contactAddress.line3
        val street2 =
          claimantDetailsAsImporterCompanyAnswer.contactAddress.line1 + " " + claimantDetailsAsImporterCompanyAnswer.contactAddress.line2
            .getOrElse("B")
        tpi05ContactInfo.street          shouldBe Some(street2)
        tpi05ContactInfo.city            shouldBe Some(claimantDetailsAsImporterCompanyAnswer.contactAddress.line4)
        tpi05ContactInfo.postalCode      shouldBe claimantDetailsAsImporterCompanyAnswer.contactAddress.postcode
        tpi05ContactInfo.countryCode     shouldBe Some(claimantDetailsAsImporterCompanyAnswer.contactAddress.country.code)
        tpi05ContactInfo.emailAddress    shouldBe Some(claimantDetailsAsImporterCompanyAnswer.emailAddress.value)
        tpi05ContactInfo.telephoneNumber shouldBe Some(claimantDetailsAsImporterCompanyAnswer.phoneNumber.value)
      }

      "valid mrn number claim, DeclarantType: AssociatedWithImporterCompany, No claimantDetailsAsImporterCompanyAnswer" in {
        val declarantType                               = DeclarantTypeAnswer.AssociatedWithImporterCompany
        val maybeClaimantDetailsAsImporterCompanyAnswer = None

        val acc14                                               = getAcc14Response()
        val displayDeclaration                                  = sample[DisplayDeclaration].copy(displayResponseDetail = acc14)
        val claim                                               = getClaimAmounts()
        val claimsAnswer                                        = ClaimsAnswer(claim)
        val completeBankAccountDetailAnswer: BankAccountDetails = sample[BankAccountDetails]
        val basisOfClaimAnswer                                  = BasisOfClaim.DutySuspension
        val declarantTypeAnswer                                 = declarantType
        val completeMovementReferenceNumberAnswer               = sample[MovementReferenceNumber]
          .copy(value = Right(MRN("10ABCDEFGHIJKLMNO0")))
        val detailsRegisteredWithCds                            =
          sample[DetailsRegisteredWithCdsFormData].copy(contactAddress = getNonUkAddress("frontend.induvidual"))

        val completeClaim =
          sample[CompleteC285Claim].copy(
            movementReferenceNumber = completeMovementReferenceNumberAnswer,
            declarantTypeAnswer = declarantTypeAnswer,
            completeDetailsRegisteredWithCdsAnswer = CompleteDetailsRegisteredWithCdsAnswer(detailsRegisteredWithCds),
            maybeContactDetailsAnswer = maybeClaimantDetailsAsImporterCompanyAnswer,
            maybeBasisOfClaimAnswer = Some(basisOfClaimAnswer),
            maybeCompleteBankAccountDetailAnswer = Some(completeBankAccountDetailAnswer),
            claimsAnswer = claimsAnswer,
            maybeCompleteReasonAndBasisOfClaimAnswer = None,
            maybeCompleteDuplicateDeclarationDetailsAnswer = None,
            maybeCompleteDeclarationDetailsAnswer = None,
            maybeDisplayDeclaration = Some(displayDeclaration),
            maybeDuplicateDisplayDeclaration = None
          )

        val submitClaimRequest = sample[SubmitClaimRequest].copy(completeClaim = completeClaim)
        val correlationId      = UUID.randomUUID()

        inSequence {
          mockGenerateReceiptDate("2021-03-08T13:57:53Z")
          mockGenerateUUID(correlationId)
          mockGenerateIsoLocalDate("2021-03-08T13:57:53Z")
        }
        val tpi05Claim       = transformer.toEisSubmitClaimRequest(submitClaimRequest)
        val tpi05            = tpi05Claim.getOrElse(fail("No Claim")).postNewClaimsRequest.requestDetail
        val tpi05EoriDetails = tpi05.requestDetailA.EORIDetails.getOrElse(fail("No EoriDetails")).agentEORIDetails

        checkFixedAcc14ToTpi05Mapping(acc14, tpi05)

        checkDetailsRegisteredWithCdsToEstablishmentAddressMapping(
          tpi05EoriDetails.CDSEstablishmentAddress,
          detailsRegisteredWithCds
        )

        val tpi05ContactInfo = tpi05EoriDetails.contactInformation.getOrElse(fail)
        val acc14Consignee   = acc14.consigneeDetails.getOrElse(fail("No Consignee"))
        tpi05ContactInfo.contactPerson shouldBe Some(acc14Consignee.legalName)
        tpi05ContactInfo.addressLine1  shouldBe acc14Consignee.contactDetails.flatMap(_.addressLine1)
        tpi05ContactInfo.addressLine2  shouldBe acc14Consignee.contactDetails.flatMap(_.addressLine2)
        tpi05ContactInfo.addressLine3  shouldBe acc14Consignee.contactDetails.flatMap(_.addressLine3)
        val street2 =
          acc14Consignee.contactDetails.flatMap(_.addressLine1).getOrElse("Q") + " " + acc14Consignee.contactDetails
            .flatMap(_.addressLine2)
            .getOrElse("W")
        tpi05ContactInfo.street          shouldBe Some(street2)
        tpi05ContactInfo.city            shouldBe acc14Consignee.contactDetails.flatMap(_.addressLine3)
        tpi05ContactInfo.postalCode      shouldBe acc14Consignee.contactDetails.flatMap(_.postalCode)
        tpi05ContactInfo.countryCode     shouldBe acc14Consignee.contactDetails.flatMap(_.countryCode)
        tpi05ContactInfo.emailAddress    shouldBe acc14Consignee.contactDetails.flatMap(_.emailAddress)
        tpi05ContactInfo.telephoneNumber shouldBe acc14Consignee.contactDetails.flatMap(_.telephone)
      }

      "valid mrn number claim, DeclarantType: AssociatedWithRepresentativeCompany, with filled out claimantDetailsAsImporterCompanyAnswer" in {
        val declarantType                          = DeclarantTypeAnswer.AssociatedWithImporterCompany
        val claimantDetailsAsImporterCompanyAnswer =
          sample[ContactDetailsFormData].copy(contactAddress = getNonUkAddress("frontend.company"))

        val acc14                                               = getAcc14Response()
        val displayDeclaration                                  = sample[DisplayDeclaration].copy(displayResponseDetail = acc14)
        val claim                                               = getClaimAmounts()
        val claimsAnswer                                        = ClaimsAnswer(claim)
        val completeBankAccountDetailAnswer: BankAccountDetails = sample[BankAccountDetails]
        val basisOfClaim                                        = BasisOfClaim.DutySuspension
        val declarantTypeAnswer                                 = declarantType
        val completeMovementReferenceNumberAnswer               = sample[MovementReferenceNumber]
          .copy(value = Right(MRN("10ABCDEFGHIJKLMNO0")))
        val detailsRegisteredWithCds                            =
          sample[DetailsRegisteredWithCdsFormData].copy(contactAddress = getNonUkAddress("frontend.induvidual"))

        val completeClaim =
          sample[CompleteC285Claim].copy(
            movementReferenceNumber = completeMovementReferenceNumberAnswer,
            declarantTypeAnswer = declarantTypeAnswer,
            completeDetailsRegisteredWithCdsAnswer = CompleteDetailsRegisteredWithCdsAnswer(detailsRegisteredWithCds),
            maybeContactDetailsAnswer = Some(CompleteContactDetailsAnswer(claimantDetailsAsImporterCompanyAnswer)),
            maybeBasisOfClaimAnswer = Some(basisOfClaim),
            maybeCompleteBankAccountDetailAnswer = Some(completeBankAccountDetailAnswer),
            claimsAnswer = claimsAnswer,
            maybeCompleteReasonAndBasisOfClaimAnswer = None,
            maybeCompleteDuplicateDeclarationDetailsAnswer = None,
            maybeCompleteDeclarationDetailsAnswer = None,
            maybeDisplayDeclaration = Some(displayDeclaration),
            maybeDuplicateDisplayDeclaration = None
          )

        val submitClaimRequest = sample[SubmitClaimRequest].copy(completeClaim = completeClaim)
        val correlationId      = UUID.randomUUID()

        inSequence {
          mockGenerateReceiptDate("2021-03-08T13:57:53Z")
          mockGenerateUUID(correlationId)
          mockGenerateIsoLocalDate("2021-03-08T13:57:53Z")
        }
        val tpi05Claim       = transformer.toEisSubmitClaimRequest(submitClaimRequest)
        val tpi05            = tpi05Claim.getOrElse(fail("No Claim")).postNewClaimsRequest.requestDetail
        val tpi05EoriDetails = tpi05.requestDetailA.EORIDetails.getOrElse(fail("No EoriDetails")).agentEORIDetails

        checkFixedAcc14ToTpi05Mapping(acc14, tpi05)

        checkDetailsRegisteredWithCdsToEstablishmentAddressMapping(
          tpi05EoriDetails.CDSEstablishmentAddress,
          detailsRegisteredWithCds
        )

        val tpi05ContactInfo = tpi05EoriDetails.contactInformation.getOrElse(fail)
        tpi05ContactInfo.contactPerson shouldBe Some(claimantDetailsAsImporterCompanyAnswer.companyName)
        tpi05ContactInfo.addressLine1  shouldBe Some(claimantDetailsAsImporterCompanyAnswer.contactAddress.line1)
        tpi05ContactInfo.addressLine2  shouldBe claimantDetailsAsImporterCompanyAnswer.contactAddress.line2
        tpi05ContactInfo.addressLine3  shouldBe claimantDetailsAsImporterCompanyAnswer.contactAddress.line3
        val street2 =
          claimantDetailsAsImporterCompanyAnswer.contactAddress.line1 + " " + claimantDetailsAsImporterCompanyAnswer.contactAddress.line2
            .getOrElse("B")
        tpi05ContactInfo.street          shouldBe Some(street2)
        tpi05ContactInfo.city            shouldBe Some(claimantDetailsAsImporterCompanyAnswer.contactAddress.line4)
        tpi05ContactInfo.postalCode      shouldBe claimantDetailsAsImporterCompanyAnswer.contactAddress.postcode
        tpi05ContactInfo.countryCode     shouldBe Some(claimantDetailsAsImporterCompanyAnswer.contactAddress.country.code)
        tpi05ContactInfo.emailAddress    shouldBe Some(claimantDetailsAsImporterCompanyAnswer.emailAddress.value)
        tpi05ContactInfo.telephoneNumber shouldBe Some(claimantDetailsAsImporterCompanyAnswer.phoneNumber.value)
      }

      "valid mrn number claim, DeclarantType: AssociatedWithRepresentativeCompany, No claimantDetailsAsImporterCompanyAnswer" in {
        val declarantType                               = DeclarantTypeAnswer.AssociatedWithRepresentativeCompany
        val maybeClaimantDetailsAsImporterCompanyAnswer = None

        val acc14                                               = getAcc14Response()
        val displayDeclaration                                  = sample[DisplayDeclaration].copy(displayResponseDetail = acc14)
        val claim                                               = getClaimAmounts()
        val claimsAnswer                                        = ClaimsAnswer(claim)
        val completeBankAccountDetailAnswer: BankAccountDetails = sample[BankAccountDetails]
        val basisOfClaimAnswer                                  = BasisOfClaim.DutySuspension
        val declarantTypeAnswer                                 = declarantType
        val completeMovementReferenceNumberAnswer               = sample[MovementReferenceNumber]
          .copy(value = Right(MRN("10ABCDEFGHIJKLMNO0")))
        val detailsRegisteredWithCds                            =
          sample[DetailsRegisteredWithCdsFormData].copy(contactAddress = getNonUkAddress("frontend.induvidual"))

        val completeClaim =
          sample[CompleteC285Claim].copy(
            movementReferenceNumber = completeMovementReferenceNumberAnswer,
            declarantTypeAnswer = declarantTypeAnswer,
            completeDetailsRegisteredWithCdsAnswer = CompleteDetailsRegisteredWithCdsAnswer(detailsRegisteredWithCds),
            maybeContactDetailsAnswer = maybeClaimantDetailsAsImporterCompanyAnswer,
            maybeBasisOfClaimAnswer = Some(basisOfClaimAnswer),
            maybeCompleteBankAccountDetailAnswer = Some(completeBankAccountDetailAnswer),
            claimsAnswer = claimsAnswer,
            maybeCompleteReasonAndBasisOfClaimAnswer = None,
            maybeCompleteDuplicateDeclarationDetailsAnswer = None,
            maybeCompleteDeclarationDetailsAnswer = None,
            maybeDisplayDeclaration = Some(displayDeclaration),
            maybeDuplicateDisplayDeclaration = None
          )

        val submitClaimRequest = sample[SubmitClaimRequest].copy(completeClaim = completeClaim)
        val correlationId      = UUID.randomUUID()

        inSequence {
          mockGenerateReceiptDate("2021-03-08T13:57:53Z")
          mockGenerateUUID(correlationId)
          mockGenerateIsoLocalDate("2021-03-08T13:57:53Z")
        }
        val tpi05Claim       = transformer.toEisSubmitClaimRequest(submitClaimRequest)
        val tpi05            = tpi05Claim.getOrElse(fail("No Claim")).postNewClaimsRequest.requestDetail
        val tpi05EoriDetails = tpi05.requestDetailA.EORIDetails.getOrElse(fail("No EoriDetails")).agentEORIDetails

        checkFixedAcc14ToTpi05Mapping(acc14, tpi05)

        checkDetailsRegisteredWithCdsToEstablishmentAddressMapping(
          tpi05EoriDetails.CDSEstablishmentAddress,
          detailsRegisteredWithCds
        )

        val tpi05ContactInfo = tpi05EoriDetails.contactInformation.getOrElse(fail)
        val acc14Declarant   = acc14.declarantDetails
        tpi05ContactInfo.contactPerson shouldBe Some(acc14Declarant.legalName)
        tpi05ContactInfo.addressLine1  shouldBe acc14Declarant.contactDetails.flatMap(_.addressLine1)
        tpi05ContactInfo.addressLine2  shouldBe acc14Declarant.contactDetails.flatMap(_.addressLine2)
        tpi05ContactInfo.addressLine3  shouldBe acc14Declarant.contactDetails.flatMap(_.addressLine3)
        val street2 =
          acc14Declarant.contactDetails.flatMap(_.addressLine1).getOrElse("Q") + " " + acc14Declarant.contactDetails
            .flatMap(_.addressLine2)
            .getOrElse("W")
        tpi05ContactInfo.street          shouldBe Some(street2)
        tpi05ContactInfo.city            shouldBe acc14Declarant.contactDetails.flatMap(_.addressLine3)
        tpi05ContactInfo.postalCode      shouldBe acc14Declarant.contactDetails.flatMap(_.postalCode)
        tpi05ContactInfo.countryCode     shouldBe acc14Declarant.contactDetails.flatMap(_.countryCode)
        tpi05ContactInfo.emailAddress    shouldBe acc14Declarant.contactDetails.flatMap(_.emailAddress)
        tpi05ContactInfo.telephoneNumber shouldBe acc14Declarant.contactDetails.flatMap(_.telephone)

      }

    }
  }

}
