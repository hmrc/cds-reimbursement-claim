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

import cats.data.NonEmptyList
import cats.data.Validated.{Invalid, Valid}
import cats.implicits.{catsSyntaxTuple2Semigroupal, toBifunctorOps}
import cats.kernel.Eq
import cats.syntax.apply._
import cats.syntax.eq._
import cats.syntax.traverse._
import com.google.inject.{ImplementedBy, Inject}
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.models
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DeclarationDetailsAnswer.CompleteDeclarationDetailsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DuplicateDeclarationDetailsAnswer.CompleteDuplicateDeclarationDetailsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ReasonAndBasisOfClaimAnswer.CompleteReasonAndBasisOfClaimAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{Address => _, BankDetails => _, NdrcDetails => _, _}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.DateGenerator
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums._
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.{EntryNumber, UUIDGenerator}
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error, Validation, invalid}
import uk.gov.hmrc.cdsreimbursementclaim.services.DefaultClaimTransformerService._
import uk.gov.hmrc.cdsreimbursementclaim.utils.DataUtils._
import uk.gov.hmrc.cdsreimbursementclaim.utils.MoneyUtils.roundedTwoDecimalPlacesToString
import uk.gov.hmrc.cdsreimbursementclaim.utils.{Logging, TimeUtils}

import javax.inject.Singleton

@ImplementedBy(classOf[DefaultClaimTransformerService])
trait ClaimTransformerService {
  def toEisSubmitClaimRequest(claimRequest: SubmitClaimRequest): Either[Error, EisSubmitClaimRequest]
}

@Singleton
class DefaultClaimTransformerService @Inject() (uuidGenerator: UUIDGenerator, dateGenerator: DateGenerator)
    extends ClaimTransformerService
    with Logging {

  private def buildMrnNumberPayload(
    submitClaimRequest: SubmitClaimRequest
  ): Either[Error, RequestDetail] = {
    val localDateNow = dateGenerator.nextIsoLocalDate

    val completeClaim = submitClaimRequest.completeClaim
    (
      makeReasonAndOrBasisOfClaim(completeClaim.maybeBasisOfClaimAnswer, None),
      setMrnDetails(
        completeClaim.maybeDisplayDeclaration,
        completeClaim
      ),
      setDuplicateMrnDetails(
        completeClaim.maybeDuplicateDisplayDeclaration,
        completeClaim
      ),
      buildEoriDetails(submitClaimRequest.completeClaim)
    ).mapN { case (maybeReasonAndOrBasis, mrnDetails, duplicateMrnDetails, eoriDetails) =>
      val requestDetailA = RequestDetailA(
        CDFPayService = CDFPayservice.NDRC,
        dateReceived = Some(localDateNow),
        claimType = Some(ClaimType.C285),
        caseType = Some(CaseType.Individual),
        customDeclarationType = Some(CustomDeclarationType.MRN),
        declarationMode = Some(DeclarationMode.ParentDeclaration),
        claimDate = Some(localDateNow),
        claimAmountTotal = Some(roundedTwoDecimalPlacesToString(submitClaimRequest.completeClaim.claims.total)),
        disposalMethod = None,
        reimbursementMethod = Some(ReimbursementMethod.BankTransfer),
        basisOfClaim = maybeReasonAndOrBasis._1,
        claimant = Some(
          DefaultClaimTransformerService.setPayeeIndicator(
            submitClaimRequest.completeClaim.declarantTypeAnswer
          )
        ),
        payeeIndicator = Some(
          DefaultClaimTransformerService.setPayeeIndicator(
            submitClaimRequest.completeClaim.declarantTypeAnswer
          )
        ),
        newEORI = None,
        newDAN = None,
        authorityTypeProvided = None,
        claimantEORI = Some(submitClaimRequest.signedInUserDetails.eori.value),
        claimantEmailAddress = submitClaimRequest.signedInUserDetails.email.map(email => email.value),
        goodsDetails = Some(
          makeGoodsDetails(
            completeClaim.declarantTypeAnswer,
            completeClaim.commodityDetailsAnswer,
            None
          )
        ),
        EORIDetails = Some(eoriDetails)
      )

      val requestDetailB = RequestDetailB(
        MRNDetails = Some(List(mrnDetails)),
        duplicateMRNDetails = duplicateMrnDetails,
        entryDetails = None,
        duplicateEntryDetails = None
      )

      RequestDetail(requestDetailA, requestDetailB)

    }.toEither
      .leftMap { errors =>
        Error(s"Could not create TPI05 EIS submit claim request for mrn journey: ${errors.toList.mkString("; ")}")
      }
  }

  private def buildLegacyNumberPayload(
    entryNumber: EntryNumber,
    submitClaimRequest: SubmitClaimRequest
  ): Either[Error, RequestDetail] = {
    val localDateNow = dateGenerator.nextIsoLocalDate

    val completeClaim = submitClaimRequest.completeClaim

    (
      makeReasonAndOrBasisOfClaim(
        completeClaim.maybeBasisOfClaimAnswer,
        completeClaim.maybeCompleteReasonAndBasisOfClaimAnswer
      ),
      makeEntryDetails(
        entryNumber,
        submitClaimRequest.signedInUserDetails,
        completeClaim
      ),
      makeDuplicateEntryDetails(
        entryNumber,
        submitClaimRequest.signedInUserDetails,
        completeClaim
      ),
      makeEntryEoriDetails(submitClaimRequest.completeClaim, submitClaimRequest.signedInUserDetails)
    ).mapN { case (maybeReasonAndOrBasis, entryDetails, duplicateEntryDetails, eoriDetails) =>
      val requestDetailA = RequestDetailA(
        CDFPayService = CDFPayservice.NDRC,
        dateReceived = Some(localDateNow),
        claimType = Some(ClaimType.C285),
        caseType = Some(CaseType.Individual),
        customDeclarationType = Some(CustomDeclarationType.Entry),
        declarationMode = Some(DeclarationMode.ParentDeclaration),
        claimDate = Some(localDateNow),
        claimAmountTotal = Some(roundedTwoDecimalPlacesToString(completeClaim.claims.total)),
        disposalMethod = None,
        reimbursementMethod = Some(ReimbursementMethod.BankTransfer),
        basisOfClaim = maybeReasonAndOrBasis._1,
        claimant = Some(
          DefaultClaimTransformerService.setPayeeIndicator(completeClaim.declarantTypeAnswer)
        ),
        payeeIndicator = Some(
          DefaultClaimTransformerService.setPayeeIndicator(completeClaim.declarantTypeAnswer)
        ),
        newEORI = None,
        newDAN = None,
        authorityTypeProvided = None,
        claimantEORI = Some(submitClaimRequest.signedInUserDetails.eori.value),
        claimantEmailAddress = submitClaimRequest.signedInUserDetails.email.map(email => email.value),
        goodsDetails = Some(
          makeGoodsDetails(
            completeClaim.declarantTypeAnswer,
            completeClaim.commodityDetailsAnswer,
            maybeReasonAndOrBasis._2
          )
        ),
        EORIDetails = Some(eoriDetails)
      )

      val requestDetailB = RequestDetailB(
        MRNDetails = None,
        duplicateMRNDetails = None,
        entryDetails = Some(List(entryDetails)),
        duplicateEntryDetails = duplicateEntryDetails
      )

      RequestDetail(requestDetailA, requestDetailB)

    }.toEither
      .leftMap { errors =>
        Error(
          s"Could not create TPI05 EIS submit claim request for legacy number journey: ${errors.toList.mkString("; ")}"
        )
      }
  }

  override def toEisSubmitClaimRequest(submitClaimRequest: SubmitClaimRequest): Either[Error, EisSubmitClaimRequest] = {

    val requestCommon = RequestCommon(
      originatingSystem = Platform.MDTP,
      receiptDate = dateGenerator.nextReceiptDate,
      acknowledgementReference = uuidGenerator.compactCorrelationId
    )

    submitClaimRequest.completeClaim.movementReferenceNumber.value match {
      case Left(entryNumber) =>
        buildLegacyNumberPayload(entryNumber, submitClaimRequest).fold(
          error => Left(Error(s"validation errors: ${error.toString}")),
          requestDetail =>
            Right(
              EisSubmitClaimRequest(
                PostNewClaimsRequest(
                  requestCommon = requestCommon,
                  requestDetail = requestDetail
                )
              )
            )
        )
      case Right(_)          =>
        buildMrnNumberPayload(submitClaimRequest).fold(
          error => Left(Error(s"validation errors: ${error.toString}")),
          requestDetail =>
            Right(
              EisSubmitClaimRequest(
                PostNewClaimsRequest(
                  requestCommon = requestCommon,
                  requestDetail = requestDetail
                )
              )
            )
        )
    }

  }

}

object DefaultClaimTransformerService {

  def setPayeeIndicator(declarantType: DeclarantTypeAnswer): String =
    declarantType match {
      case DeclarantTypeAnswer.Importer => "Importer"
      case _                            => "Representative"
    }

  final case class CompareContactInformation(
    name: Option[String],
    line1: Option[String],
    line2: Option[String],
    line3: Option[String],
    line4: Option[String],
    line5: Option[String],
    postalCode: Option[String],
    countryCode: String,
    telephone: Option[String],
    email: Option[String]
  )

  object CompareContactInformation {
    implicit val eq: Eq[CompareContactInformation]                = Eq.fromUniversalEquals[CompareContactInformation]
    val emptyCompareContactInformation: CompareContactInformation =
      CompareContactInformation(
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        "GB",
        None,
        None
      )
  }

  //TODO: comparision logic when user overwrites data
  def makeEntryEoriDetails(
    completeClaim: CompleteClaim,
    signedInUserDetails: SignedInUserDetails
  ): Validation[EoriDetails] =
    Valid(
      EoriDetails(
        agentEORIDetails = EORIInformation(
          EORINumber = signedInUserDetails.eori.value,
          CDSFullName =
            completeClaim.maybeCompleteDeclarationDetailsAnswer.map(s => s.declarationDetails.declarantName),
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
            telephone = completeClaim.maybeCompleteDeclarationDetailsAnswer.map(s =>
              s.declarationDetails.declarantPhoneNumber.value
            ),
            emailAddress = completeClaim.maybeCompleteDeclarationDetailsAnswer.map(s =>
              s.declarationDetails.declarantEmailAddress.value
            )
          ),
          contactInformation = Some(
            ContactInformation(
              contactPerson =
                completeClaim.maybeCompleteDeclarationDetailsAnswer.map(s => s.declarationDetails.declarantName),
              addressLine1 = None,
              addressLine2 = None,
              addressLine3 = None,
              street = None,
              city = None,
              countryCode = None,
              postalCode = None,
              telephoneNumber = completeClaim.maybeCompleteDeclarationDetailsAnswer.map(s =>
                s.declarationDetails.declarantPhoneNumber.value
              ),
              faxNumber = None,
              emailAddress = completeClaim.maybeCompleteDeclarationDetailsAnswer.map(s =>
                s.declarationDetails.declarantEmailAddress.value
              )
            )
          ),
          VATDetails = None
        ),
        importerEORIDetails = EORIInformation(
          EORINumber = signedInUserDetails.eori.value,
          CDSFullName = completeClaim.maybeCompleteDeclarationDetailsAnswer.map(s => s.declarationDetails.importerName),
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
            telephone = completeClaim.maybeCompleteDeclarationDetailsAnswer.map(s =>
              s.declarationDetails.importerPhoneNumber.value
            ),
            emailAddress = completeClaim.maybeCompleteDeclarationDetailsAnswer.map(s =>
              s.declarationDetails.importerEmailAddress.value
            )
          ),
          contactInformation = Some(
            ContactInformation(
              contactPerson =
                completeClaim.maybeCompleteDeclarationDetailsAnswer.map(s => s.declarationDetails.importerName),
              addressLine1 = None,
              addressLine2 = None,
              addressLine3 = None,
              street = None,
              city = None,
              countryCode = None,
              postalCode = None,
              telephoneNumber = completeClaim.maybeCompleteDeclarationDetailsAnswer.map(s =>
                s.declarationDetails.importerPhoneNumber.value
              ),
              faxNumber = None,
              emailAddress = completeClaim.maybeCompleteDeclarationDetailsAnswer.map(s =>
                s.declarationDetails.importerEmailAddress.value
              )
            )
          ),
          VATDetails = None
        )
      )
    )

  def buildEoriDetails(
    completeClaim: CompleteClaim
  ): Validation[EoriDetails] = {

    val agentContactInfo = (completeClaim.mrnContactDetailsAnswer, completeClaim.mrnContactAddressAnswer)
      .mapN { (contactDetails, contactAddress) =>
        ContactInformation(
          contactPerson = Option(contactDetails.fullName),
          addressLine1 = Option(contactAddress.line1),
          addressLine2 = contactAddress.line2,
          addressLine3 = contactAddress.line3,
          street = buildStreet(Option(contactAddress.line1), contactAddress.line2),
          city = Option(contactAddress.line4),
          countryCode = Option(contactAddress.country.code),
          postalCode = Option(contactAddress.postcode),
          telephoneNumber = contactDetails.phoneNumber.map(_.value),
          faxNumber = None,
          emailAddress = Option(contactDetails.emailAddress.value)
        )
      }
      .getOrElse(
        completeClaim.declarantTypeAnswer match {
          case DeclarantTypeAnswer.Importer | DeclarantTypeAnswer.AssociatedWithImporterCompany =>
            ContactInformation(
              contactPerson = completeClaim.consigneeDetails.map(_.legalName),
              addressLine1 = completeClaim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.addressLine1),
              addressLine2 = completeClaim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.addressLine2),
              addressLine3 = completeClaim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.addressLine3),
              street = buildStreet(
                completeClaim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.addressLine1),
                completeClaim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.addressLine2)
              ),
              city = completeClaim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.addressLine3),
              postalCode = completeClaim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.postalCode),
              countryCode = completeClaim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.countryCode),
              telephoneNumber = completeClaim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.telephone),
              faxNumber = None,
              emailAddress = completeClaim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.emailAddress)
            )
          case DeclarantTypeAnswer.AssociatedWithRepresentativeCompany                          =>
            ContactInformation(
              contactPerson = completeClaim.declarantDetails.map(_.legalName),
              addressLine1 = completeClaim.declarantDetails.flatMap(_.contactDetails).flatMap(_.addressLine1),
              addressLine2 = completeClaim.declarantDetails.flatMap(_.contactDetails).flatMap(_.addressLine2),
              addressLine3 = completeClaim.declarantDetails.flatMap(_.contactDetails).flatMap(_.addressLine3),
              street = buildStreet(
                completeClaim.declarantDetails.flatMap(_.contactDetails).flatMap(_.addressLine1),
                completeClaim.declarantDetails.flatMap(_.contactDetails).flatMap(_.addressLine2)
              ),
              city = completeClaim.declarantDetails.flatMap(_.contactDetails).flatMap(_.addressLine3),
              postalCode = completeClaim.declarantDetails.flatMap(_.contactDetails).flatMap(_.postalCode),
              countryCode = completeClaim.declarantDetails.flatMap(_.contactDetails).flatMap(_.countryCode),
              telephoneNumber = completeClaim.declarantDetails.flatMap(_.contactDetails).flatMap(_.telephone),
              faxNumber = None,
              emailAddress = completeClaim.declarantDetails.flatMap(_.contactDetails).flatMap(_.emailAddress)
            )
        }
      )

    Valid(
      EoriDetails(
        agentEORIDetails = EORIInformation(
          EORINumber = completeClaim.declarantDetails.map(s => s.declarantEORI).getOrElse(""),
          CDSFullName = completeClaim.declarantDetails.map(s => s.legalName),
          legalEntityType = None,
          EORIStartDate = None,
          CDSEstablishmentAddress = Address(
            contactPerson = Option(completeClaim.detailsRegisteredWithCdsAnswer.fullName),
            addressLine1 = Option(completeClaim.detailsRegisteredWithCdsAnswer.contactAddress.line1),
            addressLine2 = completeClaim.detailsRegisteredWithCdsAnswer.contactAddress.line2,
            AddressLine3 = completeClaim.detailsRegisteredWithCdsAnswer.contactAddress.line3,
            street = buildStreet(
              Option(completeClaim.detailsRegisteredWithCdsAnswer.contactAddress.line1),
              completeClaim.detailsRegisteredWithCdsAnswer.contactAddress.line2
            ),
            city = Some(completeClaim.detailsRegisteredWithCdsAnswer.contactAddress.line4),
            postalCode = completeClaim.detailsRegisteredWithCdsAnswer.contactAddress.postcode,
            countryCode = completeClaim.detailsRegisteredWithCdsAnswer.contactAddress.country.code,
            telephone = None,
            emailAddress = Option(completeClaim.detailsRegisteredWithCdsAnswer.emailAddress.value)
          ),
          contactInformation = Some(agentContactInfo),
          VATDetails = None
        ),
        importerEORIDetails = EORIInformation(
          EORINumber = completeClaim.consigneeDetails.map(s => s.consigneeEORI).getOrElse(""),
          CDSFullName = completeClaim.consigneeDetails.map(s => s.legalName),
          legalEntityType = None,
          EORIStartDate = None,
          CDSEstablishmentAddress = Address(
            contactPerson = None,
            addressLine1 = completeClaim.consigneeDetails.map(_.establishmentAddress.addressLine1),
            addressLine2 = completeClaim.consigneeDetails.flatMap(_.establishmentAddress.addressLine2),
            AddressLine3 = completeClaim.consigneeDetails.flatMap(_.establishmentAddress.addressLine3),
            street = buildStreet(
              completeClaim.consigneeDetails.map(_.establishmentAddress.addressLine1),
              completeClaim.consigneeDetails.flatMap(_.establishmentAddress.addressLine2)
            ),
            city = completeClaim.consigneeDetails.flatMap(_.establishmentAddress.addressLine3),
            countryCode = completeClaim.consigneeDetails.map(_.establishmentAddress.countryCode).getOrElse("GB"),
            postalCode = completeClaim.consigneeDetails.flatMap(_.establishmentAddress.postalCode),
            telephone = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.telephone)),
            emailAddress = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.emailAddress))
          ),
          contactInformation = Some(
            ContactInformation(
              contactPerson = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.contactName)),
              addressLine1 = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine1)),
              addressLine2 = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine2)),
              addressLine3 = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine3)),
              street = buildStreet(
                completeClaim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine1)),
                completeClaim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine2))
              ),
              city = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine3)),
              countryCode = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.countryCode)),
              postalCode = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.postalCode)),
              telephoneNumber = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.telephone)),
              faxNumber = None,
              emailAddress = completeClaim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.emailAddress))
            )
          ),
          VATDetails = None
        )
      )
    )
  }

  def buildStreet(lineA: Option[String], lineB: Option[String]): Option[String] =
    (lineA, lineB) match {
      case (Some(s1), Some(s2)) => Some(s"$s1 $s2")
      case (Some(s1), None)     => Some(s1)
      case (None, Some(s2))     => Some(s2)
      case _                    => Some("")
    }

  def buildConsigneeEstablishmentAddressWithUserInput(
    userInput: CompareContactInformation
  ): Validation[Address] =
    if (userInput.countryCode.length === 0) {
      invalid("country code from FE is blank")
    } else {
      Valid(
        Address(
          contactPerson = userInput.name,
          addressLine1 = userInput.line1,
          addressLine2 = userInput.line2,
          AddressLine3 = userInput.line3,
          street = buildStreet(userInput.line1, userInput.line2),
          city = userInput.line4,
          countryCode = userInput.countryCode,
          postalCode = userInput.postalCode,
          telephone = userInput.telephone,
          emailAddress = userInput.email
        )
      )
    }

  def buildConsigneeContactInformationWithUserInput(
    userInput: CompareContactInformation
  ): Validation[ContactInformation] =
    Valid(
      ContactInformation(
        contactPerson = userInput.name,
        addressLine1 = userInput.line1,
        addressLine2 = userInput.line2,
        addressLine3 = userInput.line3,
        street = buildStreet(userInput.line1, userInput.line2),
        city = userInput.line4,
        countryCode = Some(userInput.countryCode),
        postalCode = userInput.postalCode,
        telephoneNumber = userInput.telephone,
        faxNumber = None,
        emailAddress = userInput.email
      )
    )

  def validateContactPersonName(contactPersonName: String): Validation[String] =
    if (contactPersonName.length === 0) invalid("contact person name is blank") else Valid(contactPersonName)

  def validateCountryCode(countryCode: String): Validation[String] =
    if (countryCode.length === 0) invalid("country code is blank") else Valid(countryCode)

  def buildDeclarantCdsEstablishmentAddress(
    maybeDisplayDeclaration: Option[models.claim.DisplayDeclaration]
  ): Validation[Address] =
    maybeDisplayDeclaration match {
      case Some(displayDeclaration) =>
        validateCountryCode(displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.countryCode)
          .fold[Validation[Address]](
            _ => invalid("country code is blank"),
            countryCode =>
              Valid(
                Address(
                  contactPerson = Some(displayDeclaration.displayResponseDetail.declarantDetails.legalName),
                  addressLine1 =
                    Some(displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.addressLine1),
                  addressLine2 =
                    displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.addressLine2,
                  AddressLine3 =
                    displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.addressLine3,
                  street =
                    Some(displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.addressLine1)
                      .flatMap(line1 =>
                        displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.addressLine2.map(
                          line2 => s"$line1 $line2"
                        )
                      ),
                  city = displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.addressLine3,
                  countryCode = countryCode,
                  postalCode =
                    displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.postalCode,
                  telephone = None,
                  emailAddress = None
                )
              )
          )
      case None                     => Invalid(NonEmptyList.one("could not find address"))
    }

  def buildConsigneeCdsEstablishmentAddress(
    maybeDisplayDeclaration: Option[models.claim.DisplayDeclaration]
  ): Validation[Address] =
    maybeDisplayDeclaration match {
      case Some(displayDeclaration) =>
        displayDeclaration.displayResponseDetail.consigneeDetails match {
          case Some(consigneeDetails) =>
            validateCountryCode(consigneeDetails.establishmentAddress.countryCode).fold[Validation[Address]](
              _ => invalid("no country code"),
              countryCode =>
                Valid(
                  Address(
                    contactPerson = Some(consigneeDetails.legalName),
                    addressLine1 = Some(consigneeDetails.establishmentAddress.addressLine1),
                    addressLine2 = consigneeDetails.establishmentAddress.addressLine2,
                    AddressLine3 = consigneeDetails.establishmentAddress.addressLine3,
                    street = buildStreet(
                      Option(consigneeDetails.establishmentAddress.addressLine1),
                      consigneeDetails.establishmentAddress.addressLine2
                    ),
                    city = consigneeDetails.establishmentAddress.addressLine3,
                    countryCode = countryCode,
                    postalCode = consigneeDetails.establishmentAddress.postalCode,
                    telephone = None,
                    emailAddress = None
                  )
                )
            )
          case None                   => Invalid(NonEmptyList.one("could not find consignee details"))
        }
      case None                     => Invalid(NonEmptyList.one("could not find address"))
    }

  def makeGoodsDetails(
    declarantType: DeclarantTypeAnswer,
    commodityDetails: CommodityDetails,
    reasonForClaim: Option[String]
  ): GoodsDetails =
    GoodsDetails(
      placeOfImport = None,
      isPrivateImporter = Some(declarantType match {
        case DeclarantTypeAnswer.Importer => "Yes"
        case _                            => "No"
      }),
      groundsForRepaymentApplication = reasonForClaim,
      descOfGoods = Some(commodityDetails.value)
    )

  def makeReasonAndOrBasisOfClaim(
    maybeBasisOfClaim: Option[BasisOfClaim],
    maybeCompleteReasonAndBasisOfClaimAnswer: Option[CompleteReasonAndBasisOfClaimAnswer]
  ): Validation[(Option[String], Option[String])] =
    (maybeBasisOfClaim, maybeCompleteReasonAndBasisOfClaimAnswer) match {
      case (Some(_), Some(_))                  =>
        invalid("Must not have both basis of claim and reason and basis of claim")
      case (None, Some(reasonAndBasisOfClaim)) =>
        Valid(
          (
            Some(
              BasisForClaim.toBasisForClaimToString(reasonAndBasisOfClaim.selectReasonForBasisAndClaim.basisForClaim)
            ),
            Some(reasonAndBasisOfClaim.selectReasonForBasisAndClaim.reasonForClaim.repr)
          )
        )
      case (Some(basisOfClaim), None)          => Valid((Some(BasisOfClaim.basisOfClaimToString(basisOfClaim)), None))
      case (None, None)                        => invalid("Could not find either basis of claim nor reason and basis for claim")
    }

  def buildConsigneeEstablishmentAddress(
    consigneeDetails: models.claim.ConsigneeDetails
  ): Validation[Address] =
    Valid(
      Address(
        contactPerson = None,
        addressLine1 = Some(consigneeDetails.establishmentAddress.addressLine1),
        addressLine2 = consigneeDetails.establishmentAddress.addressLine2,
        AddressLine3 = consigneeDetails.establishmentAddress.addressLine3,
        street = buildStreet(
          Option(consigneeDetails.establishmentAddress.addressLine1),
          consigneeDetails.establishmentAddress.addressLine2
        ),
        city = consigneeDetails.establishmentAddress.addressLine3,
        countryCode = consigneeDetails.establishmentAddress.countryCode,
        postalCode = consigneeDetails.establishmentAddress.postalCode,
        telephone = None,
        emailAddress = None
      )
    )

  def buildDeclarantEstablishmentAddress(
    declarantDetails: models.claim.DeclarantDetails
  ): Validation[Address] =
    Valid(
      Address(
        contactPerson = None,
        addressLine1 = Some(declarantDetails.establishmentAddress.addressLine1),
        addressLine2 = declarantDetails.establishmentAddress.addressLine2,
        AddressLine3 = declarantDetails.establishmentAddress.addressLine3,
        street = buildStreet(
          Option(declarantDetails.establishmentAddress.addressLine1),
          declarantDetails.establishmentAddress.addressLine2
        ),
        city = declarantDetails.establishmentAddress.addressLine3,
        countryCode = declarantDetails.establishmentAddress.countryCode,
        postalCode = declarantDetails.establishmentAddress.postalCode,
        telephone = None,
        emailAddress = None
      )
    )

  def buildDeclarantContactInformation(
    maybeDeclarantDetails: Option[models.claim.DeclarantDetails]
  ): Validation[ContactInformation] =
    maybeDeclarantDetails match {
      case Some(declarantDetails) =>
        declarantDetails.contactDetails match {
          case Some(contactDetails) =>
            Valid(
              ContactInformation(
                contactPerson = contactDetails.contactName,
                addressLine1 = contactDetails.addressLine1,
                addressLine2 = contactDetails.addressLine2,
                addressLine3 = contactDetails.addressLine3,
                street = buildStreet(contactDetails.addressLine1, contactDetails.addressLine2),
                city = contactDetails.addressLine3,
                countryCode = contactDetails.countryCode,
                postalCode = contactDetails.postalCode,
                telephoneNumber = contactDetails.telephone,
                faxNumber = None,
                emailAddress = contactDetails.emailAddress
              )
            )
          case None                 =>
            Invalid(NonEmptyList.one("could not find contact details to buidl declarant contact information"))
        }

      case None => Invalid(NonEmptyList.one("could not find declarant details"))
    }

  def buildConsigneeContactInformation(
    maybeConsigneeDetails: Option[models.claim.ConsigneeDetails]
  ): Validation[ContactInformation] =
    maybeConsigneeDetails match {
      case Some(consigneeDetails) =>
        consigneeDetails.contactDetails match {
          case Some(contactDetails) =>
            Valid(
              ContactInformation(
                contactPerson = contactDetails.contactName,
                addressLine1 = contactDetails.addressLine1,
                addressLine2 = contactDetails.addressLine2,
                addressLine3 = contactDetails.addressLine3,
                street = buildStreet(contactDetails.addressLine1, contactDetails.addressLine2),
                city = contactDetails.addressLine3,
                countryCode = contactDetails.countryCode,
                postalCode = contactDetails.postalCode,
                telephoneNumber = contactDetails.telephone,
                faxNumber = None,
                emailAddress = contactDetails.emailAddress
              )
            )
          case None                 =>
            Invalid(NonEmptyList.one("could not find contact details to build consigned contact information"))
        }
      case None                   => Invalid(NonEmptyList.one("could not find consignee contact information"))
    }

  def setDeclarantDetails(declarantDetails: models.claim.DeclarantDetails): Validation[MRNInformation] =
    (buildDeclarantEstablishmentAddress(declarantDetails), buildDeclarantContactInformation(Some(declarantDetails)))
      .mapN { case (establishmentAddress, contactInformation) =>
        MRNInformation(
          EORI = declarantDetails.declarantEORI,
          legalName = declarantDetails.legalName,
          establishmentAddress = establishmentAddress,
          contactDetails = contactInformation
        )
      }

  def setConsigneeDetails(
    maybeConsigneeDetails: Option[models.claim.ConsigneeDetails]
  ): Validation[MRNInformation] =
    maybeConsigneeDetails match {
      case Some(consigneeDetails) =>
        (buildConsigneeEstablishmentAddress(consigneeDetails), buildConsigneeContactInformation(Some(consigneeDetails)))
          .mapN { case (establishmentAddress, contactInformation) =>
            MRNInformation(
              EORI = consigneeDetails.consigneeEORI,
              legalName = consigneeDetails.legalName,
              establishmentAddress = establishmentAddress,
              contactDetails = contactInformation
            )
          }
      case None                   => Invalid(NonEmptyList.one("could not find consignee details"))
    }

  def makeEntryConsigneeDetails(
    signedInUserDetails: SignedInUserDetails,
    completeDeclarationDetailsAnswer: CompleteDeclarationDetailsAnswer
  ): Validation[MRNInformation] =
    Valid(
      MRNInformation(
        EORI = signedInUserDetails.eori.value,
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
    )

  def setDuplicateEntryConsigneeDetails(
    signedInUserDetails: SignedInUserDetails,
    completeDuplicateDeclarationDetailsAnswer: CompleteDuplicateDeclarationDetailsAnswer
  ): Validation[MRNInformation] =
    completeDuplicateDeclarationDetailsAnswer.duplicateDeclaration match {
      case Some(entryDeclarationDetails) =>
        Valid(
          MRNInformation(
            EORI = signedInUserDetails.eori.value,
            legalName = entryDeclarationDetails.declarantName,
            establishmentAddress = Address.empty,
            contactDetails = ContactInformation(
              contactPerson = Some(entryDeclarationDetails.importerName),
              addressLine1 = None,
              addressLine2 = None,
              addressLine3 = None,
              street = None,
              city = None,
              countryCode = None,
              postalCode = None,
              telephoneNumber = Some(entryDeclarationDetails.importerPhoneNumber.value),
              faxNumber = None,
              emailAddress = Some(entryDeclarationDetails.importerEmailAddress.value)
            )
          )
        )
      case None                          => invalid("Could not find duplication declaration details")
    }

  def makeEntryDeclarantDetails(
    signedInUserDetails: SignedInUserDetails,
    completeDeclarationDetailsAnswer: CompleteDeclarationDetailsAnswer
  ): Validation[MRNInformation] =
    Valid(
      MRNInformation(
        EORI = signedInUserDetails.eori.value,
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
    )

  def setDuplicateEntryDeclarationDetails(
    signedInUserDetails: SignedInUserDetails,
    completeDeclarationDetailsAnswer: CompleteDuplicateDeclarationDetailsAnswer
  ): Validation[MRNInformation] =
    completeDeclarationDetailsAnswer.duplicateDeclaration match {
      case Some(entryDeclarationDetails) =>
        Valid(
          MRNInformation(
            EORI = signedInUserDetails.eori.value,
            legalName = entryDeclarationDetails.declarantName,
            establishmentAddress = Address.empty,
            contactDetails = ContactInformation(
              contactPerson = Some(entryDeclarationDetails.declarantName),
              addressLine1 = None,
              addressLine2 = None,
              addressLine3 = None,
              street = None,
              city = None,
              countryCode = None,
              postalCode = None,
              telephoneNumber = Some(entryDeclarationDetails.declarantPhoneNumber.value),
              faxNumber = None,
              emailAddress = Some(entryDeclarationDetails.declarantEmailAddress.value)
            )
          )
        )
      case None                          => invalid("could not find duplicate entry declaration details")
    }

  def buildBankDetails(
    maybeBankDetails: Option[models.claim.BankDetails],
    completeClaim: CompleteClaim
  ): Validation[BankDetails] =
    (maybeBankDetails, completeClaim.maybeBankAccountDetailsAnswer) match {
      case (_, Some(bankAccountDetails)) =>
        Valid(
          BankDetails(
            consigneeBankDetails = Some(
              BankDetail(
                bankAccountDetails.accountName.value,
                bankAccountDetails.sortCode.value,
                bankAccountDetails.accountNumber.value
              )
            ),
            declarantBankDetails = Some(
              BankDetail(
                bankAccountDetails.accountName.value,
                bankAccountDetails.sortCode.value,
                bankAccountDetails.accountNumber.value
              )
            )
          )
        )
      case (Some(bankDetails), None)     =>
        (bankDetails.consigneeBankDetails, bankDetails.declarantBankDetails) match {
          case (Some(consignee), Some(declarant)) =>
            Valid(
              BankDetails(
                consigneeBankDetails =
                  Some(BankDetail(consignee.accountHolderName, consignee.sortCode, consignee.accountNumber)),
                declarantBankDetails =
                  Some(BankDetail(declarant.accountHolderName, declarant.sortCode, declarant.accountNumber))
              )
            )
          case (Some(consignee), None)            =>
            Valid(
              BankDetails(
                consigneeBankDetails =
                  Some(BankDetail(consignee.accountHolderName, consignee.sortCode, consignee.accountNumber)),
                declarantBankDetails = None
              )
            )
          case (None, Some(declarant))            =>
            Valid(
              BankDetails(
                consigneeBankDetails = None,
                declarantBankDetails =
                  Some(BankDetail(declarant.accountHolderName, declarant.sortCode, declarant.accountNumber))
              )
            )
          case (None, None)                       =>
            Valid(
              BankDetails(consigneeBankDetails = None, declarantBankDetails = None)
            )
        }
      case _                             => invalid("could not build bank details")
    }

  def makeEntryBankDetails(
    maybeBankAccountDetailsAnswer: Option[BankAccountDetails]
  ): Validation[BankDetails] =
    maybeBankAccountDetailsAnswer match {
      case Some(bankAccountDetailAnswer) =>
        Valid(
          BankDetails(
            consigneeBankDetails = None,
            declarantBankDetails = Some(
              BankDetail(
                bankAccountDetailAnswer.accountName.value,
                bankAccountDetailAnswer.sortCode.value,
                bankAccountDetailAnswer.accountNumber.value
              )
            )
          )
        )
      case None                          => invalid("Could not find entered bank details")
    }

  def makeNdrcDetails(claims: NonEmptyList[Claim]): Validation[List[NdrcDetails]] = {
    val result: List[Validation[NdrcDetails]] = claims.map { claim =>
      (
        isValidPaymentMethod(claim.paymentMethod),
        isValidTaxType(claim.taxCode),
        isValidPaymentReference(claim.paymentReference),
        isValidAmount(claim.paidAmount),
        isValidAmount(claim.claimAmount)
      ).mapN { case (paymentMethod, taxType, paymentReference, paidAmount, claimAmount) =>
        NdrcDetails(
          paymentMethod = paymentMethod,
          paymentReference = paymentReference,
          None,
          taxType = taxType,
          amount = paidAmount,
          claimAmount = Some(claimAmount)
        )
      }
    }.toList
    if (result.sequence.isInvalid) {
      val errors = result.collect { case e: Invalid[NonEmptyList[String]] => e }
      invalid(s"there is at least one claim which has failed validation: ${errors.map(s => s.e.toList).mkString("|")}")
    } else {
      val dd: List[NdrcDetails] = result.collect { case Valid(value) => value }
      Valid(dd)
    }

  }

  def setAcceptanceDate(
    displayAcceptanceDate: String
  ): Validation[String] =
    TimeUtils.fromDisplayAcceptanceDateFormat(displayAcceptanceDate) match {
      case Some(acceptanceDate) => Valid(acceptanceDate)
      case None                 => invalid("Could not format display acceptance date")
    }

  def makeEntryDate(
    dateOfImport: DateOfImport
  ): Validation[String] =
    TimeUtils.toEntryDateFormat(
      dateOfImport.value
    ) match {
      case Some(entryDate) => Valid(entryDate)
      case None            => invalid(s"Could not format entry date: ${dateOfImport.value.toString}")
    }

  def setDuplicateEntryAcceptanceDate(
    completeDuplicateDeclarationDetailsAnswer: CompleteDuplicateDeclarationDetailsAnswer
  ): Validation[String] =
    completeDuplicateDeclarationDetailsAnswer.duplicateDeclaration match {
      case Some(entryDeclarationDetails) =>
        TimeUtils.toEntryDateFormat(
          entryDeclarationDetails.dateOfImport.value
        ) match {
          case Some(entryDate) => Valid(entryDate)
          case None            =>
            invalid(
              s"Could not format display entry date for duplicate entry: ${entryDeclarationDetails.dateOfImport.value.toString}"
            )
        }
      case None                          => invalid("could not find acceptance date")
    }

  def setMrnDetails(
    maybeDisplayDeclaration: Option[models.claim.DisplayDeclaration],
    completeClaim: CompleteClaim
  ): Validation[MrnDetail] =
    maybeDisplayDeclaration match {
      case Some(displayDeclaration) =>
        (
          setAcceptanceDate(displayDeclaration.displayResponseDetail.acceptanceDate),
          setDeclarantDetails(displayDeclaration.displayResponseDetail.declarantDetails),
          setConsigneeDetails(displayDeclaration.displayResponseDetail.consigneeDetails),
          buildBankDetails(displayDeclaration.displayResponseDetail.bankDetails, completeClaim),
          makeNdrcDetails(completeClaim.claims)
        ).mapN { case (acceptanceDate, declarationDetails, consigneeDetails, bankDetails, ndrcDetails) =>
          MrnDetail(
            MRNNumber = Some(displayDeclaration.displayResponseDetail.declarationId),
            acceptanceDate = Some(acceptanceDate),
            declarantReferenceNumber = displayDeclaration.displayResponseDetail.declarantReferenceNumber,
            mainDeclarationReference = Some(true),
            procedureCode = Some(displayDeclaration.displayResponseDetail.procedureCode),
            declarantDetails = Some(declarationDetails),
            accountDetails = None,
            consigneeDetails = Some(consigneeDetails),
            bankDetails = Some(bankDetails),
            NDRCDetails = Some(ndrcDetails)
          )
        }
      case None                     => Invalid(NonEmptyList.one("could not build mrn details"))
    }

  def setDuplicateMrnDetails(
    maybeDisplayDeclaration: Option[models.claim.DisplayDeclaration],
    completeClaim: CompleteClaim
  ): Validation[Option[MrnDetail]] =
    maybeDisplayDeclaration match {
      case Some(displayDeclaration) =>
        (
          setDeclarantDetails(displayDeclaration.displayResponseDetail.declarantDetails),
          setConsigneeDetails(displayDeclaration.displayResponseDetail.consigneeDetails),
          setAcceptanceDate(displayDeclaration.displayResponseDetail.acceptanceDate),
          buildBankDetails(displayDeclaration.displayResponseDetail.bankDetails, completeClaim),
          makeNdrcDetails(completeClaim.claims)
        ).mapN { case (declarationDetails, consigneeDetails, acceptanceDate, bankDetails, ndrcDetails) =>
          Some(
            MrnDetail(
              MRNNumber = Some(displayDeclaration.displayResponseDetail.declarationId),
              acceptanceDate = Some(acceptanceDate),
              declarantReferenceNumber = displayDeclaration.displayResponseDetail.declarantReferenceNumber,
              mainDeclarationReference = Some(true),
              procedureCode = Some(displayDeclaration.displayResponseDetail.procedureCode),
              declarantDetails = Some(declarationDetails),
              accountDetails = None,
              consigneeDetails = Some(consigneeDetails),
              bankDetails = Some(bankDetails),
              NDRCDetails = Some(ndrcDetails)
            )
          )
        }
      case None                     => Valid(None)
    }

  def makeEntryDetails(
    entryNumber: EntryNumber,
    signedInUserDetails: SignedInUserDetails,
    completeClaim: CompleteClaim
  ): Validation[EntryDetail] = {
    val maybeEntryDeclarationDetailsAnswer = completeClaim.maybeCompleteDeclarationDetailsAnswer
    maybeEntryDeclarationDetailsAnswer match {
      case Some(entryDeclarationDetails) =>
        (
          makeEntryDate(entryDeclarationDetails.declarationDetails.dateOfImport),
          makeEntryDeclarantDetails(signedInUserDetails, entryDeclarationDetails),
          makeEntryConsigneeDetails(signedInUserDetails, entryDeclarationDetails),
          makeEntryBankDetails(completeClaim.maybeBankAccountDetailsAnswer),
          makeNdrcDetails(completeClaim.claims)
        ).mapN { case (entryDate, declarant, consigneeDetails, bankDetails, ndrcDetails) =>
          EntryDetail(
            entryNumber = Some(entryNumber.value),
            entryDate = Some(entryDate),
            declarantReferenceNumber = None,
            mainDeclarationReference = Some(true),
            declarantDetails = Some(declarant),
            accountDetails = None,
            consigneeDetails = Some(consigneeDetails),
            bankDetails = Some(bankDetails),
            NDRCDetails = Some(ndrcDetails)
          )
        }
      case None                          => Invalid(NonEmptyList.one("could not build entry details"))
    }
  }

  def makeDuplicateEntryDetails(
    entryNumber: EntryNumber,
    signedInUserDetails: SignedInUserDetails,
    completeClaim: CompleteClaim
  ): Validation[Option[EntryDetail]] = {
    val maybeDuplicateDeclarationDetailsAnswer
      : Option[DuplicateDeclarationDetailsAnswer.CompleteDuplicateDeclarationDetailsAnswer] =
      completeClaim.maybeCompleteDuplicateDeclarationDetailsAnswer

    maybeDuplicateDeclarationDetailsAnswer match {
      case Some(duplicateDeclarationDetailsAnswer) =>
        duplicateDeclarationDetailsAnswer.duplicateDeclaration match {
          case Some(_) =>
            (
              setDuplicateEntryAcceptanceDate(duplicateDeclarationDetailsAnswer),
              setDuplicateEntryDeclarationDetails(signedInUserDetails, duplicateDeclarationDetailsAnswer),
              setDuplicateEntryConsigneeDetails(signedInUserDetails, duplicateDeclarationDetailsAnswer),
              makeEntryBankDetails(completeClaim.maybeBankAccountDetailsAnswer),
              makeNdrcDetails(completeClaim.claims)
            ).mapN { case (acceptanceDate, declarant, consigneeDetails, bankDetails, ndrcDetails) =>
              Some(
                EntryDetail(
                  entryNumber = Some(entryNumber.value),
                  entryDate = Some(acceptanceDate),
                  declarantReferenceNumber = None,
                  mainDeclarationReference = Some(true),
                  declarantDetails = Some(declarant),
                  accountDetails = None,
                  consigneeDetails = Some(consigneeDetails),
                  bankDetails = Some(bankDetails),
                  NDRCDetails = Some(ndrcDetails)
                )
              )
            }
          case None    => Valid(None)
        }

      case None => Valid(None)
    }
  }

}
