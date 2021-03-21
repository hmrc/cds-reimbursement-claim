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
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.BankAccountDetailsAnswer.CompleteBankAccountDetailAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.BasisOfClaimAnswer.CompleteBasisOfClaimAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ClaimsAnswer.CompleteClaimsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.CompleteClaim.CompleteC285Claim
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DeclarantTypeAnswer.CompleteDeclarantTypeAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DeclarationDetailsAnswer.CompleteDeclarationDetailsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.MovementReferenceNumberAnswer.CompleteMovementReferenceNumberAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{Address => _, _}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.DateGenerator
import uk.gov.hmrc.cdsreimbursementclaim.models.eis
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.{MRNInformation, _}
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.ClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.CompleteClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.Generators.sample
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.{EntryNumber, MRN, UUIDGenerator}
import uk.gov.hmrc.cdsreimbursementclaim.utils.MoneyUtils.roundedTwoDecimalPlacesToString
import uk.gov.hmrc.cdsreimbursementclaim.utils.TimeUtils

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

  val transformer = new DefaultClaimTransformerService(mockUuidGenerator, mockDateGenerator)

  "Claim transformer" when {

    "passed a claim request" must {

      "make an EIS submit claim request for a valid legacy number claim" in {

        val claim = sample[Claim].copy(
          paymentMethod = "001",
          taxCode = "A00",
          paidAmount = BigDecimal(20.00),
          claimAmount = BigDecimal(10.00),
          paymentReference = "pay-ref"
        )

        val completeClaimsAnswer = sample[CompleteClaimsAnswer].copy(
          List(
            claim
          )
        )

        val completeBankAccountDetailAnswer: CompleteBankAccountDetailAnswer = sample[CompleteBankAccountDetailAnswer]

        val entryDeclarationDetails = sample[EntryDeclarationDetails].copy(
          dateOfImport = DateOfImport(LocalDate.parse("2020-12-06", DateTimeFormatter.ofPattern("u-M-d")))
        )

        val completeDeclarationDetailsAnswer = sample[CompleteDeclarationDetailsAnswer].copy(
          declarationDetails = entryDeclarationDetails
        )
        val completeBasisOfClaimAnswer       =
          sample[CompleteBasisOfClaimAnswer].copy(basisOfClaim = BasisOfClaim.DutySuspension)

        val completeDeclarantTypeAnswer           =
          sample[CompleteDeclarantTypeAnswer].copy(declarantType = DeclarantType.Importer)
        val completeMovementReferenceNumberAnswer = sample[CompleteMovementReferenceNumberAnswer]
          .copy(movementReferenceNumber = Left(EntryNumber("666541198B49856762")))

        val completeClaim =
          sample[CompleteC285Claim].copy(
            completeMovementReferenceNumberAnswer = completeMovementReferenceNumberAnswer,
            completeDeclarantTypeAnswer = completeDeclarantTypeAnswer,
            maybeBasisOfClaimAnswer = Some(completeBasisOfClaimAnswer),
            maybeCompleteBankAccountDetailAnswer = Some(completeBankAccountDetailAnswer),
            completeClaimsAnswer = completeClaimsAnswer,
            maybeCompleteReasonAndBasisOfClaimAnswer = None,
            maybeCompleteDuplicateDeclarationDetailsAnswer = None,
            maybeCompleteDeclarationDetailsAnswer = Some(completeDeclarationDetailsAnswer)
          )

        val submitClaimRequest = sample[SubmitClaimRequest].copy(completeClaim = completeClaim)
        val correlationId      = UUID.randomUUID()

        val goodsDetails = GoodsDetails(
          None,
          Some("Yes"),
          None,
          Some(completeClaim.commodityDetails)
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
              completeBankAccountDetailAnswer.bankAccountDetails.accountName.value,
              completeBankAccountDetailAnswer.bankAccountDetails.sortCode.value,
              completeBankAccountDetailAnswer.bankAccountDetails.accountNumber.value
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

        val entryEoriDetails = EoriDetails(
          agentEORIDetails = EORIInformation(
            EORINumber = submitClaimRequest.signedInUserDetails.eori.value,
            CDSFullName = None,
            legalEntityType = None,
            EORIStartDate = None,
            CDSEstablishmentAddress = Address.empty,
            contactInformation = Some(
              ContactInformation(
                contactPerson = Some(completeClaim.claimantDetailsAsIndividual.fullName),
                addressLine1 = Some(completeClaim.claimantDetailsAsIndividual.contactAddress.line1),
                addressLine2 = completeClaim.claimantDetailsAsIndividual.contactAddress.line2,
                addressLine3 = completeClaim.claimantDetailsAsIndividual.contactAddress.line3,
                street = Some(
                  s"${Some(completeClaim.claimantDetailsAsIndividual.contactAddress.line1)} ${completeClaim.claimantDetailsAsIndividual.contactAddress.line2}"
                ),
                city = Some(completeClaim.claimantDetailsAsIndividual.contactAddress.line4),
                countryCode = Some(completeClaim.claimantDetailsAsIndividual.contactAddress.country.code),
                postalCode = completeClaim.claimantDetailsAsIndividual.contactAddress.postcode,
                telephoneNumber = Some(completeClaim.claimantDetailsAsIndividual.phoneNumber.value),
                faxNumber = None,
                emailAddress = Some(completeClaim.claimantDetailsAsIndividual.emailAddress.value)
              )
            ),
            VATDetails = None
          ),
          importerEORIDetails = EORIInformation(
            EORINumber = submitClaimRequest.signedInUserDetails.eori.value,
            CDSFullName = None,
            legalEntityType = None,
            EORIStartDate = None,
            CDSEstablishmentAddress = Address.empty,
            contactInformation = Some(
              ContactInformation(
                contactPerson = completeClaim.claimantDetailsAsImporter.map(d => d.companyName),
                addressLine1 = completeClaim.claimantDetailsAsImporter.map(d => d.contactAddress.line1),
                addressLine2 = completeClaim.claimantDetailsAsImporter.flatMap(d => d.contactAddress.line2),
                addressLine3 = completeClaim.claimantDetailsAsImporter.flatMap(d => d.contactAddress.line3),
                street = None,
                city = completeClaim.claimantDetailsAsImporter.flatMap(d => d.contactAddress.line5),
                countryCode = completeClaim.claimantDetailsAsImporter.map(d => d.contactAddress.country.code),
                postalCode = completeClaim.claimantDetailsAsImporter.flatMap(d => d.contactAddress.postcode),
                telephoneNumber = completeClaim.claimantDetailsAsImporter.map(d => d.phoneNumber.value),
                faxNumber = None,
                emailAddress = completeClaim.claimantDetailsAsImporter.map(d => d.emailAddress.value)
              )
            ),
            VATDetails = None
          )
        )

        val requestCommon = sample[RequestCommon].copy(
          originatingSystem = Platform.MDTP,
          receiptDate = "2018-08-08T13:57:53Z",
          acknowledgementReference = correlationId.toString
        )

        val requestDetailA =
          sample[RequestDetailA].copy(
            CDFPayService = CDFPayservice.NDRC,
            dateReceived = Some(TimeUtils.isoLocalDateNow),
            claimType = Some(ClaimType.C285),
            caseType = Some(CaseType.Individual),
            customDeclarationType = Some(CustomDeclarationType.Entry),
            declarationMode = Some(DeclarationMode.ParentDeclaration),
            claimDate = Some(TimeUtils.isoLocalDateNow),
            claimAmountTotal = Some(roundedTwoDecimalPlacesToString(completeClaim.claims.total)),
            disposalMethod = None,
            reimbursementMethod = Some(ReimbursementMethod.BankTransfer),
            claimant = Some(
              DefaultClaimTransformerService.setPayeeIndicator(completeClaim.declarantType.declarantType)
            ),
            payeeIndicator = Some(
              DefaultClaimTransformerService.setPayeeIndicator(completeClaim.declarantType.declarantType)
            ),
            newEORI = None,
            newDAN = None,
            authorityTypeProvided = None,
            claimantEORI = Some(submitClaimRequest.signedInUserDetails.eori.value),
            claimantEmailAddress = submitClaimRequest.signedInUserDetails.email.map(email => email.value),
            goodsDetails = Some(goodsDetails),
            basisOfClaim = Some("Duty Suspension"),
            EORIDetails = Some(entryEoriDetails)
          )
        val requestDetailB =
          sample[RequestDetailB].copy(
            MRNDetails = None,
            duplicateMRNDetails = None,
            entryDetails = Some(List(entryDetails)),
            duplicateEntryDetails = None
          )

        val postNewClaimsRequest  = PostNewClaimsRequest(
          requestCommon = requestCommon,
          requestDetail = RequestDetail(requestDetailA, requestDetailB)
        )
        val eisSubmitClaimRequest = EisSubmitClaimRequest(postNewClaimsRequest)

        inSequence {
          mockGenerateReceiptDate("2018-08-08T13:57:53Z")
          mockGenerateUUID(correlationId)
        }

        transformer.toEisSubmitClaimRequest(submitClaimRequest) shouldBe Right(eisSubmitClaimRequest)

      }

      "make an EIS submit claim request for a valid mrn number claim" in {

        val displayResponseDetail =
          sample[DisplayResponseDetail].copy(acceptanceDate = "21 March 2021")
        val displayDeclaration    = sample[DisplayDeclaration].copy(displayResponseDetail = displayResponseDetail)

        val claim = sample[Claim].copy(
          paymentMethod = "001",
          taxCode = "A00",
          paidAmount = BigDecimal(20.00),
          claimAmount = BigDecimal(10.00),
          paymentReference = "pay-ref"
        )

        val completeClaimsAnswer = sample[CompleteClaimsAnswer].copy(
          List(
            claim
          )
        )

        val completeBankAccountDetailAnswer: CompleteBankAccountDetailAnswer = sample[CompleteBankAccountDetailAnswer]

        val completeBasisOfClaimAnswer =
          sample[CompleteBasisOfClaimAnswer].copy(basisOfClaim = BasisOfClaim.DutySuspension)

        val completeDeclarantTypeAnswer           =
          sample[CompleteDeclarantTypeAnswer].copy(declarantType = DeclarantType.Importer)
        val completeMovementReferenceNumberAnswer = sample[CompleteMovementReferenceNumberAnswer]
          .copy(movementReferenceNumber = Right(MRN("10ABCDEFGHIJKLMNO0")))

        val completeClaim =
          sample[CompleteC285Claim].copy(
            completeMovementReferenceNumberAnswer = completeMovementReferenceNumberAnswer,
            completeDeclarantTypeAnswer = completeDeclarantTypeAnswer,
            maybeBasisOfClaimAnswer = Some(completeBasisOfClaimAnswer),
            maybeCompleteBankAccountDetailAnswer = Some(completeBankAccountDetailAnswer),
            completeClaimsAnswer = completeClaimsAnswer,
            maybeCompleteReasonAndBasisOfClaimAnswer = None,
            maybeCompleteDuplicateDeclarationDetailsAnswer = None,
            maybeCompleteDeclarationDetailsAnswer = None,
            maybeDisplayDeclaration = Some(displayDeclaration)
          )

        val submitClaimRequest = sample[SubmitClaimRequest].copy(completeClaim = completeClaim)
        val correlationId      = UUID.randomUUID()

        val goodsDetails = GoodsDetails(
          None,
          Some("Yes"),
          None,
          Some(completeClaim.commodityDetails)
        )

        val bankDetails = uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.BankDetails(
          consigneeBankDetails = None,
          declarantBankDetails = Some(
            BankDetail(
              completeBankAccountDetailAnswer.bankAccountDetails.accountName.value,
              completeBankAccountDetailAnswer.bankAccountDetails.sortCode.value,
              completeBankAccountDetailAnswer.bankAccountDetails.accountNumber.value
            )
          )
        )

        val entryEoriDetails = EoriDetails(
          agentEORIDetails = EORIInformation(
            EORINumber = submitClaimRequest.signedInUserDetails.eori.value,
            CDSFullName = None,
            legalEntityType = None,
            EORIStartDate = None,
            CDSEstablishmentAddress = Address.empty,
            contactInformation = Some(
              ContactInformation(
                contactPerson = Some(completeClaim.claimantDetailsAsIndividual.fullName),
                addressLine1 = Some(completeClaim.claimantDetailsAsIndividual.contactAddress.line1),
                addressLine2 = completeClaim.claimantDetailsAsIndividual.contactAddress.line2,
                addressLine3 = completeClaim.claimantDetailsAsIndividual.contactAddress.line3,
                street = Some(
                  s"${Some(completeClaim.claimantDetailsAsIndividual.contactAddress.line1)} ${completeClaim.claimantDetailsAsIndividual.contactAddress.line2}"
                ),
                city = Some(completeClaim.claimantDetailsAsIndividual.contactAddress.line4),
                countryCode = Some(completeClaim.claimantDetailsAsIndividual.contactAddress.country.code),
                postalCode = completeClaim.claimantDetailsAsIndividual.contactAddress.postcode,
                telephoneNumber = Some(completeClaim.claimantDetailsAsIndividual.phoneNumber.value),
                faxNumber = None,
                emailAddress = Some(completeClaim.claimantDetailsAsIndividual.emailAddress.value)
              )
            ),
            VATDetails = None
          ),
          importerEORIDetails = EORIInformation(
            EORINumber = submitClaimRequest.signedInUserDetails.eori.value,
            CDSFullName = None,
            legalEntityType = None,
            EORIStartDate = None,
            CDSEstablishmentAddress = Address.empty,
            contactInformation = Some(
              ContactInformation(
                contactPerson = completeClaim.claimantDetailsAsImporter.map(d => d.companyName),
                addressLine1 = completeClaim.claimantDetailsAsImporter.map(d => d.contactAddress.line1),
                addressLine2 = completeClaim.claimantDetailsAsImporter.flatMap(d => d.contactAddress.line2),
                addressLine3 = completeClaim.claimantDetailsAsImporter.flatMap(d => d.contactAddress.line3),
                street = None,
                city = completeClaim.claimantDetailsAsImporter.flatMap(d => d.contactAddress.line5),
                countryCode = completeClaim.claimantDetailsAsImporter.map(d => d.contactAddress.country.code),
                postalCode = completeClaim.claimantDetailsAsImporter.flatMap(d => d.contactAddress.postcode),
                telephoneNumber = completeClaim.claimantDetailsAsImporter.map(d => d.phoneNumber.value),
                faxNumber = None,
                emailAddress = completeClaim.claimantDetailsAsImporter.map(d => d.emailAddress.value)
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

        val consigneeDetails = MRNInformation(
          EORI =
            displayDeclaration.displayResponseDetail.consigneeDetails.map(s => s.consigneeEORI).getOrElse("No eori"),
          legalName =
            displayDeclaration.displayResponseDetail.consigneeDetails.map(s => s.legalName).getOrElse("No legal name"),
          establishmentAddress = Address(
            contactPerson = displayDeclaration.displayResponseDetail.consigneeDetails.map(s => s.legalName),
            addressLine1 = displayDeclaration.displayResponseDetail.consigneeDetails.flatMap(s =>
              s.contactDetails.flatMap(s => s.addressLine1)
            ),
            addressLine2 = displayDeclaration.displayResponseDetail.consigneeDetails.flatMap(s =>
              s.contactDetails.flatMap(s => s.addressLine2)
            ),
            AddressLine3 = None,
            street = displayDeclaration.displayResponseDetail.consigneeDetails
              .flatMap(s => s.contactDetails.flatMap(s => s.addressLine1))
              .flatMap(line1 =>
                displayDeclaration.displayResponseDetail.consigneeDetails
                  .flatMap(s => s.contactDetails.flatMap(s => s.addressLine2))
                  .map(line2 => s"$line1 $line2")
              ),
            city = displayDeclaration.displayResponseDetail.consigneeDetails.flatMap(s =>
              s.contactDetails.flatMap(s => s.addressLine3)
            ),
            countryCode = displayDeclaration.displayResponseDetail.consigneeDetails
              .flatMap(s => s.contactDetails.flatMap(s => s.countryCode))
              .getOrElse("GB"),
            postalCode = displayDeclaration.displayResponseDetail.consigneeDetails.flatMap(s =>
              s.contactDetails.flatMap(s => s.postalCode)
            ),
            telephone = displayDeclaration.displayResponseDetail.consigneeDetails.flatMap(s =>
              s.contactDetails.flatMap(s => s.telephone)
            ),
            emailAddress = displayDeclaration.displayResponseDetail.consigneeDetails.flatMap(s =>
              s.contactDetails.flatMap(s => s.emailAddress)
            )
          ),
          contactDetails = ContactInformation(
            contactPerson = displayDeclaration.displayResponseDetail.consigneeDetails.map(s => s.legalName),
            addressLine1 = displayDeclaration.displayResponseDetail.consigneeDetails.flatMap(s =>
              s.contactDetails.flatMap(s => s.addressLine1)
            ),
            addressLine2 = displayDeclaration.displayResponseDetail.consigneeDetails.flatMap(s =>
              s.contactDetails.flatMap(s => s.addressLine2)
            ),
            addressLine3 = displayDeclaration.displayResponseDetail.consigneeDetails.flatMap(s =>
              s.contactDetails.flatMap(s => s.addressLine3)
            ),
            street = displayDeclaration.displayResponseDetail.consigneeDetails
              .flatMap(s => s.contactDetails.flatMap(s => s.addressLine1))
              .flatMap(line1 =>
                displayDeclaration.displayResponseDetail.consigneeDetails
                  .flatMap(s => s.contactDetails.flatMap(s => s.addressLine2))
                  .map(line2 => s"$line1 $line2")
              ),
            city = displayDeclaration.displayResponseDetail.consigneeDetails.flatMap(s =>
              s.contactDetails.flatMap(s => s.addressLine4)
            ),
            countryCode = displayDeclaration.displayResponseDetail.consigneeDetails.flatMap(s =>
              s.contactDetails.flatMap(s => s.countryCode)
            ),
            postalCode = displayDeclaration.displayResponseDetail.consigneeDetails.flatMap(s =>
              s.contactDetails.flatMap(s => s.postalCode)
            ),
            telephoneNumber = displayDeclaration.displayResponseDetail.consigneeDetails.flatMap(s =>
              s.contactDetails.flatMap(s => s.telephone)
            ),
            faxNumber = None,
            emailAddress = displayDeclaration.displayResponseDetail.consigneeDetails.flatMap(s =>
              s.contactDetails.flatMap(s => s.emailAddress)
            )
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
          consigneeDetails = Some(consigneeDetails),
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
          originatingSystem = Platform.MDTP,
          receiptDate = "2018-08-08T13:57:53Z",
          acknowledgementReference = correlationId.toString
        )

        val requestDetailA =
          sample[RequestDetailA].copy(
            CDFPayService = CDFPayservice.NDRC,
            dateReceived = Some(TimeUtils.isoLocalDateNow),
            claimType = Some(ClaimType.C285),
            caseType = Some(CaseType.Individual),
            customDeclarationType = Some(CustomDeclarationType.MRN),
            declarationMode = Some(DeclarationMode.ParentDeclaration),
            claimDate = Some(TimeUtils.isoLocalDateNow),
            claimAmountTotal = Some(roundedTwoDecimalPlacesToString(completeClaim.claims.total)),
            disposalMethod = None,
            reimbursementMethod = Some(ReimbursementMethod.BankTransfer),
            claimant = Some(
              DefaultClaimTransformerService.setPayeeIndicator(completeClaim.declarantType.declarantType)
            ),
            payeeIndicator = Some(
              DefaultClaimTransformerService.setPayeeIndicator(completeClaim.declarantType.declarantType)
            ),
            newEORI = None,
            newDAN = None,
            authorityTypeProvided = None,
            claimantEORI = Some(submitClaimRequest.signedInUserDetails.eori.value),
            claimantEmailAddress = submitClaimRequest.signedInUserDetails.email.map(email => email.value),
            goodsDetails = Some(goodsDetails),
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
        val _                    = EisSubmitClaimRequest(postNewClaimsRequest)

        inSequence {
          mockGenerateReceiptDate("2018-08-08T13:57:53Z")
          mockGenerateUUID(correlationId)
        }

        //TODO
        transformer.toEisSubmitClaimRequest(submitClaimRequest)
        1 shouldBe 1
      }

      "must return an error" in {
        //TODO: need to go through all the different validation failures
      }

    }
  }

}
