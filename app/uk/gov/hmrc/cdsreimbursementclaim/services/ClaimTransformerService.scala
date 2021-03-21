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
import cats.implicits.catsSyntaxTuple2Semigroupal
import cats.kernel.Eq
import cats.syntax.apply._
import cats.syntax.either._
import cats.syntax.eq._
import cats.syntax.traverse._
import com.google.inject.{ImplementedBy, Inject}
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.models
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.BankAccountDetailsAnswer.CompleteBankAccountDetailAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DeclarationDetailsAnswer.CompleteDeclarationDetailsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.DuplicateDeclarationDetailsAnswer.CompleteDuplicateDeclarationDetailsAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{BasisForClaim, Address => _, BankDetails => _, NdrcDetails => _, _}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.DateGenerator
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.BasisOfClaim.{DuplicateEntry, DutySuspension, EndUseRelief, IncorrectCommodityCode, IncorrectCpc, IncorrectEoriAndDefermentAccountNumber, IncorrectValue, InwardProcessingReliefFromCustomsDuty, Miscellaneous, OutwardProcessingRelief, PersonalEffects, Preference, ProofOfReturnRefundGiven, RGR}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums._
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.UUIDGenerator
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

  override def toEisSubmitClaimRequest(claimRequest: SubmitClaimRequest): Either[Error, EisSubmitClaimRequest] = {
    val requestCommon = RequestCommon(
      originatingSystem = Platform.MDTP,
      receiptDate = dateGenerator.nextReceiptDate,
      acknowledgementReference = uuidGenerator.compactCorrelationId
    )
    claimRequest.completeClaim.movementReferenceNumber match {
      case Left(_)  =>
        val completeClaim = claimRequest.completeClaim
        (
          setGoodsDetails(completeClaim),
          setBasisOfClaim(completeClaim),
          setEntryDetails(
            claimRequest.signedInUserDetails,
            completeClaim
          ),
          setDuplicateEntryDetails(
            claimRequest.signedInUserDetails,
            completeClaim
          ),
          buildEntryEoriDetails(claimRequest.completeClaim, claimRequest.signedInUserDetails)
        ).mapN { case (goodsDetails, basisOfClaim, entryDetails, duplicateEntryDetails, eoriDetails) =>
          val requestDetailA = RequestDetailA(
            CDFPayService = CDFPayservice.NDRC,
            dateReceived = Some(TimeUtils.isoLocalDateNow),
            claimType = Some(ClaimType.C285),
            caseType = Some(CaseType.Individual),
            customDeclarationType = Some(CustomDeclarationType.Entry),
            declarationMode = Some(DeclarationMode.ParentDeclaration),
            claimDate = Some(TimeUtils.isoLocalDateNow),
            claimAmountTotal = Some(roundedTwoDecimalPlacesToString(claimRequest.completeClaim.claims.total)),
            disposalMethod = None,
            reimbursementMethod = Some(ReimbursementMethod.BankTransfer),
            basisOfClaim = Some(basisOfClaim),
            claimant = Some(
              DefaultClaimTransformerService.setPayeeIndicator(claimRequest.completeClaim.declarantType.declarantType)
            ),
            payeeIndicator = Some(
              DefaultClaimTransformerService.setPayeeIndicator(claimRequest.completeClaim.declarantType.declarantType)
            ),
            newEORI = None,
            newDAN = None,
            authorityTypeProvided = None,
            claimantEORI = Some(claimRequest.signedInUserDetails.eori.value),
            claimantEmailAddress = claimRequest.signedInUserDetails.email.map(email => email.value),
            goodsDetails = Some(goodsDetails),
            EORIDetails = Some(eoriDetails)
          )

          val requestDetailB = RequestDetailB(
            MRNDetails = None,
            duplicateMRNDetails = None,
            entryDetails = Some(List(entryDetails)),
            duplicateEntryDetails = duplicateEntryDetails
          )

          val postNewClaimsRequest = PostNewClaimsRequest(
            requestCommon = requestCommon,
            requestDetail = RequestDetail(requestDetailA, requestDetailB)
          )
          EisSubmitClaimRequest(postNewClaimsRequest)

        }.toEither
          .leftMap { errors =>
            Error(s"Could not create TPI05 EIS submit claim request for entry journey: ${errors.toList.mkString("; ")}")
          }
      case Right(_) =>
        val completeClaim = claimRequest.completeClaim
        (
          setGoodsDetails(completeClaim),
          setBasisOfClaim(completeClaim),
          setMrnDetails(
            completeClaim.displayDeclaration,
            completeClaim
          ),
          setDuplicateMrnDetails(
            completeClaim.duplicateDisplayDeclaration,
            completeClaim
          ),
          buildEoriDetails(claimRequest.completeClaim, claimRequest.signedInUserDetails)
        ).mapN { case (goodsDetails, basisOfClaim, mrnDetails, duplicateMrnDetails, eoriDetails) =>
          val requestDetailA = RequestDetailA(
            CDFPayService = CDFPayservice.NDRC,
            dateReceived = Some(TimeUtils.isoLocalDateNow),
            claimType = Some(ClaimType.C285),
            caseType = Some(CaseType.Individual),
            customDeclarationType = Some(CustomDeclarationType.MRN),
            declarationMode = Some(DeclarationMode.ParentDeclaration),
            claimDate = Some(TimeUtils.isoLocalDateNow),
            claimAmountTotal = Some(roundedTwoDecimalPlacesToString(claimRequest.completeClaim.claims.total)),
            disposalMethod = None,
            reimbursementMethod = Some(ReimbursementMethod.BankTransfer),
            basisOfClaim = Some(basisOfClaim),
            claimant = Some(
              DefaultClaimTransformerService.setPayeeIndicator(claimRequest.completeClaim.declarantType.declarantType)
            ),
            payeeIndicator = Some(
              DefaultClaimTransformerService.setPayeeIndicator(claimRequest.completeClaim.declarantType.declarantType)
            ),
            newEORI = None,
            newDAN = None,
            authorityTypeProvided = None,
            claimantEORI = Some(claimRequest.signedInUserDetails.eori.value),
            claimantEmailAddress = claimRequest.signedInUserDetails.email.map(email => email.value),
            goodsDetails = Some(goodsDetails),
            EORIDetails = Some(eoriDetails)
          )

          val requestDetailB = RequestDetailB(
            MRNDetails = Some(List(mrnDetails)),
            duplicateMRNDetails = duplicateMrnDetails,
            entryDetails = None,
            duplicateEntryDetails = None
          )

          val postNewClaimsRequest = PostNewClaimsRequest(
            requestCommon = requestCommon,
            requestDetail = RequestDetail(requestDetailA, requestDetailB)
          )
          EisSubmitClaimRequest(postNewClaimsRequest)

        }.toEither
          .leftMap { errors =>
            Error(s"Could not create TPI05 EIS submit claim request for mrn journey: ${errors.toList.mkString("; ")}")
          }
    }
  }

}

object DefaultClaimTransformerService {

  def setPayeeIndicator(declarantType: DeclarantType): String =
    declarantType match {
      case DeclarantType.Importer => "Importer"
      case _                      => "Representative"
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
      ) //TODO: check the assumption that the country code can be defaulted to GB - this can happen if the importer details are not filled in
  }

  def buildEntryEoriDetails(
    completeClaim: CompleteClaim,
    signedInUserDetails: SignedInUserDetails
  ): Validation[EoriDetails] = {

    val claimantAsIndividualDetails = completeClaim.claimantDetailsAsIndividual

    val userProvidedIndividualContactInformation: CompareContactInformation = CompareContactInformation(
      Some(claimantAsIndividualDetails.fullName),
      Some(claimantAsIndividualDetails.contactAddress.line1),
      claimantAsIndividualDetails.contactAddress.line2,
      claimantAsIndividualDetails.contactAddress.line3,
      Some(claimantAsIndividualDetails.contactAddress.line4),
      claimantAsIndividualDetails.contactAddress.line5,
      claimantAsIndividualDetails.contactAddress.postcode,
      Some(claimantAsIndividualDetails.contactAddress.country.code).getOrElse("GB"),
      Some(claimantAsIndividualDetails.phoneNumber.value),
      Some(claimantAsIndividualDetails.emailAddress.value)
    )

    val buildConsigneeContactInformationWithPossibleUserInput: Validation[ContactInformation] =
      buildConsigneeContactInformationWithUserInput(userProvidedIndividualContactInformation)

    val claimantAsImporterDetails = completeClaim.claimantDetailsAsImporter

    val userProvidedCompanyContactInformation = claimantAsImporterDetails match {
      case Some(claimantDetailsAsImporterCompany) =>
        CompareContactInformation(
          Some(claimantDetailsAsImporterCompany.companyName),
          Some(claimantDetailsAsImporterCompany.contactAddress.line1),
          claimantDetailsAsImporterCompany.contactAddress.line2,
          claimantDetailsAsImporterCompany.contactAddress.line3,
          Some(claimantDetailsAsImporterCompany.contactAddress.line4),
          claimantDetailsAsImporterCompany.contactAddress.line5,
          claimantDetailsAsImporterCompany.contactAddress.postcode,
          claimantDetailsAsImporterCompany.contactAddress.country.code,
          Some(claimantDetailsAsImporterCompany.phoneNumber.value),
          Some(claimantDetailsAsImporterCompany.emailAddress.value)
        )
      case None                                   => CompareContactInformation.emptyCompareContactInformation
    }

    val buildConsigneeEstablishmentContactDetails: Validation[Address] =
      buildConsigneeEstablishmentAddressWithUserInput(userProvidedCompanyContactInformation)

    (
      buildConsigneeContactInformationWithPossibleUserInput,
      buildConsigneeEstablishmentContactDetails
    ).mapN { case (consignee, _) =>
      EoriDetails(
        agentEORIDetails = EORIInformation(
          EORINumber = signedInUserDetails.eori.value,
          CDSFullName = None,
          legalEntityType = None,
          EORIStartDate = None,
          CDSEstablishmentAddress = Address.empty,
          contactInformation = Some(consignee),
          VATDetails = None
        ),
        importerEORIDetails = EORIInformation(
          EORINumber = signedInUserDetails.eori.value,
          CDSFullName = None,
          legalEntityType = None,
          EORIStartDate = None,
          CDSEstablishmentAddress = Address.empty,
          contactInformation = Some(
            ContactInformation(
              contactPerson = claimantAsImporterDetails.map(d => d.companyName),
              addressLine1 = claimantAsImporterDetails.map(d => d.contactAddress.line1),
              addressLine2 = claimantAsImporterDetails.flatMap(d => d.contactAddress.line2),
              addressLine3 = claimantAsImporterDetails.flatMap(d => d.contactAddress.line3),
              street = None,
              city = claimantAsImporterDetails.flatMap(d => d.contactAddress.line5),
              countryCode = claimantAsImporterDetails.map(d => d.contactAddress.country.code),
              postalCode = claimantAsImporterDetails.flatMap(d => d.contactAddress.postcode),
              telephoneNumber = claimantAsImporterDetails.map(d => d.phoneNumber.value),
              faxNumber = None,
              emailAddress = claimantAsImporterDetails.map(d => d.emailAddress.value)
            )
          ),
          VATDetails = None
        )
      )
    }
  }

  def buildEoriDetails(
    completeClaim: CompleteClaim,
    signedInUserDetails: SignedInUserDetails
  ): Validation[EoriDetails] = {

    val contactInfoFromConsignee: Option[models.claim.ContactDetails] =
      completeClaim.consigneeDetails.flatMap(consigneeDetails => consigneeDetails.contactDetails)
    val claimantAsIndividualDetails                                   = completeClaim.claimantDetailsAsIndividual

    val declarationIndividualContactInformation: CompareContactInformation = contactInfoFromConsignee match {
      case Some(contactDetails) =>
        CompareContactInformation(
          contactDetails.contactName,
          contactDetails.addressLine1,
          contactDetails.addressLine2,
          contactDetails.addressLine3,
          contactDetails.addressLine4,
          None,
          contactDetails.postalCode,
          contactDetails.countryCode.getOrElse("GB"), //default to this? EIS data model forces this
          contactDetails.telephone,
          contactDetails.emailAddress
        )
      case None                 => CompareContactInformation.emptyCompareContactInformation
    }

    val userProvidedIndividualContactInformation: CompareContactInformation = CompareContactInformation(
      Some(claimantAsIndividualDetails.fullName),
      Some(claimantAsIndividualDetails.contactAddress.line1),
      claimantAsIndividualDetails.contactAddress.line2,
      claimantAsIndividualDetails.contactAddress.line3,
      Some(claimantAsIndividualDetails.contactAddress.line4),
      claimantAsIndividualDetails.contactAddress.line5,
      claimantAsIndividualDetails.contactAddress.postcode,
      Some(claimantAsIndividualDetails.contactAddress.country.code).getOrElse("GB"),
      Some(claimantAsIndividualDetails.phoneNumber.value),
      Some(claimantAsIndividualDetails.emailAddress.value)
    )

    val buildConsigneeContactInformationWithPossibleUserInpit: Validation[ContactInformation] =
      if (declarationIndividualContactInformation === userProvidedIndividualContactInformation) {
        buildConsigneeContactInformation(completeClaim.consigneeDetails)
      } else {
        buildConsigneeContactInformationWithUserInput(userProvidedIndividualContactInformation)
      }

    val claimantAsImporterDetails                       = completeClaim.claimantDetailsAsImporter
    val establishmentAddressForConsigneeFromDeclaration =
      completeClaim.consigneeDetails.map(s => s.establishmentAddress)

    val declarationCompanyContactInformation: CompareContactInformation =
      establishmentAddressForConsigneeFromDeclaration match {
        case Some(establishmentAddress) =>
          CompareContactInformation(
            completeClaim.consigneeDetails.map(s => s.legalName),
            Some(establishmentAddress.addressLine1),
            establishmentAddress.addressLine2,
            establishmentAddress.addressLine3,
            None,
            None,
            establishmentAddress.postalCode,
            establishmentAddress.countryCode,
            None,
            None
          )
        case None                       => CompareContactInformation.emptyCompareContactInformation
      }

    val userProvidedCompanyContactInformation = claimantAsImporterDetails match {
      case Some(claimantDetailsAsImporterCompany) =>
        CompareContactInformation(
          Some(claimantDetailsAsImporterCompany.companyName),
          Some(claimantDetailsAsImporterCompany.contactAddress.line1),
          claimantDetailsAsImporterCompany.contactAddress.line2,
          claimantDetailsAsImporterCompany.contactAddress.line3,
          Some(claimantDetailsAsImporterCompany.contactAddress.line4),
          claimantDetailsAsImporterCompany.contactAddress.line5,
          claimantDetailsAsImporterCompany.contactAddress.postcode,
          claimantDetailsAsImporterCompany.contactAddress.country.code,
          Some(claimantDetailsAsImporterCompany.phoneNumber.value),
          Some(claimantDetailsAsImporterCompany.emailAddress.value)
        )
      case None                                   => CompareContactInformation.emptyCompareContactInformation
    }

    val buildConsigneeEstablishmentContactDetails: Validation[Address] =
      if (declarationCompanyContactInformation === userProvidedCompanyContactInformation) {
        buildConsigneeCdsEstablishmentAddress(completeClaim.displayDeclaration)
      } else {
        buildConsigneeEstablishmentAddressWithUserInput(userProvidedCompanyContactInformation)
      }

    (
      buildConsigneeContactInformationWithPossibleUserInpit,
      buildDeclarantContactInformation(completeClaim.declarantDetails),
      buildConsigneeEstablishmentContactDetails,
      buildDeclarantCdsEstablishmentAddress(completeClaim.displayDeclaration)
    ).mapN { case (consignee, declarant, cdsConsignee, cdsDeclarant) =>
      EoriDetails(
        agentEORIDetails = EORIInformation(
          EORINumber = signedInUserDetails.eori.value,
          CDSFullName = None,
          legalEntityType = None,
          EORIStartDate = None,
          CDSEstablishmentAddress = cdsConsignee,
          contactInformation = Some(consignee),
          VATDetails = None
        ),
        importerEORIDetails = EORIInformation(
          EORINumber = signedInUserDetails.eori.value,
          CDSFullName = None,
          legalEntityType = None,
          EORIStartDate = None,
          CDSEstablishmentAddress = cdsDeclarant,
          contactInformation = Some(declarant),
          VATDetails = None
        )
      )
    }
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
          street = Some(s"${userInput.line1} ${userInput.line2}"),
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
        street = Some(s"${userInput.line1} ${userInput.line2}"),
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
                    street = Some(consigneeDetails.establishmentAddress.addressLine1).flatMap(line1 =>
                      consigneeDetails.establishmentAddress.addressLine2.map(line2 => s"$line1 $line2")
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

  private def isPrivateImporter(declarantType: DeclarantType): String =
    declarantType match {
      case DeclarantType.Importer => "Yes"
      case _                      => "No"
    }

  def setGoodsDetails(completeClaim: CompleteClaim): Validation[GoodsDetails] =
    Valid(
      GoodsDetails(
        None,
        Some(isPrivateImporter(completeClaim.declarantType.declarantType)),
        groundsForRepaymentApplication = None,
        descOfGoods = Some(completeClaim.commodityDetails)
      )
    )

  def setBasisOfClaim(completeClaim: CompleteClaim): Validation[String] =
    completeClaim.movementReferenceNumber match {
      case Left(_)  =>
        (completeClaim.basisOfClaim, completeClaim.reasonAndBasisOfClaim) match {
          case (Some(_), Some(_))  =>
            sys.error("cannot have both reason-basis for claim and basis of claim")
          case (Some(basis), None) =>
            Valid(basis match {
              case DuplicateEntry                         => "Duplicate Entry"
              case DutySuspension                         => "Duty Suspension"
              case EndUseRelief                           => "End Use"
              case IncorrectCommodityCode                 => "Incorrect Commodity Code"
              case IncorrectCpc                           => "Incorrect CPC"
              case IncorrectValue                         => "Incorrect Value"
              case IncorrectEoriAndDefermentAccountNumber => "Incorrect EORI & Deferment Acc. Num."
              case InwardProcessingReliefFromCustomsDuty  => "IP"
              case Miscellaneous                          => "Miscellaneous"
              case OutwardProcessingRelief                => "OPR"
              case PersonalEffects                        => "Personal Effects"
              case Preference                             => "Preference"
              case RGR                                    => "RGR"
              case ProofOfReturnRefundGiven               => "Proof of Return/Refund Given"
            })
          case (None, Some(r))     =>
            Valid(r.selectReasonForBasisAndClaim.basisForClaim match {
              case BasisForClaim.DuplicateEntry                         => "Duplicate Entry"
              case BasisForClaim.DutySuspension                         => "Duty Suspension"
              case BasisForClaim.EndUseRelief                           => "End Use"
              case BasisForClaim.IncorrectCommodityCode                 => "Incorrect Commodity Code"
              case BasisForClaim.IncorrectCpc                           => "Incorrect CPC"
              case BasisForClaim.IncorrectValue                         => "Incorrect Value"
              case BasisForClaim.IncorrectEoriAndDefermentAccountNumber => "Incorrect EORI & Deferment Acc. Num."
              case BasisForClaim.InwardProcessingReliefFromCustomsDuty  => "IP"
              case BasisForClaim.Miscellaneous                          => "Miscellaneous"
              case BasisForClaim.OutwardProcessingRelief                => "OPR"
              case BasisForClaim.PersonalEffects                        => "Personal Effects"
              case BasisForClaim.Preference                             => "Preference"
              case BasisForClaim.RGR                                    => "RGR"
              case BasisForClaim.ProofOfReturnRefundGiven               => "Proof of Return/Refund Given"
            })
          case _                   => invalid("inconsistent state of basis and reasons for claim")
        }
      case Right(_) =>
        completeClaim.basisOfClaim match {
          case Some(value) => Valid(value)
          case None        => Invalid(NonEmptyList.one("could not find basis of claim"))
        }
    }

  def buildConsigneeEstablishmentAddress(
    consigneeDetails: models.claim.ConsigneeDetails
  ): Validation[Address] =
    consigneeDetails.contactDetails match {
      case Some(contactDetails) =>
        contactDetails.countryCode.fold[Validation[Address]](
          Invalid(NonEmptyList.one("could not find consignee establishment address"))
        )(countryCode =>
          Valid(
            Address(
              contactPerson = Some(consigneeDetails.legalName),
              addressLine1 = contactDetails.addressLine1,
              addressLine2 = contactDetails.addressLine2,
              AddressLine3 = None,
              street = contactDetails.addressLine1.flatMap(line1 =>
                contactDetails.addressLine2.map(line2 => s"$line1 $line2")
              ),
              city = contactDetails.addressLine3,
              countryCode = countryCode,
              postalCode = contactDetails.postalCode,
              telephone = contactDetails.telephone,
              emailAddress = contactDetails.emailAddress
            )
          )
        )
      case None                 => Invalid(NonEmptyList.one("no contact details"))
    }

  def buildDeclarantEstablishmentAddress(
    declarantDetails: models.claim.DeclarantDetails
  ): Validation[Address] =
    declarantDetails.contactDetails match {
      case Some(contactDetails) =>
        contactDetails.countryCode.fold[Validation[Address]](
          Invalid(NonEmptyList.one("could not find declarant establishment address"))
        )(countryCode =>
          Valid(
            Address(
              contactPerson = Some(declarantDetails.legalName),
              addressLine1 = contactDetails.addressLine1,
              addressLine2 = contactDetails.addressLine2,
              AddressLine3 = None,
              street = contactDetails.addressLine1.flatMap(line1 =>
                contactDetails.addressLine2.map(line2 => s"$line1 $line2")
              ),
              city = contactDetails.addressLine3,
              countryCode = countryCode,
              postalCode = contactDetails.postalCode,
              telephone = contactDetails.telephone,
              emailAddress = contactDetails.emailAddress
            )
          )
        )
      case None                 => Invalid(NonEmptyList.one("no contact details"))
    }

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
                street = contactDetails.addressLine1.flatMap(line1 =>
                  contactDetails.addressLine2.map(line2 => s"$line1 $line2")
                ),
                city = contactDetails.addressLine4,
                countryCode = contactDetails.countryCode,
                postalCode = contactDetails.postalCode,
                telephoneNumber = contactDetails.telephone,
                faxNumber = None,
                emailAddress = contactDetails.emailAddress
              )
            )
          case None                 => Invalid(NonEmptyList.one("could not find contact details"))
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
                street = contactDetails.addressLine1.flatMap(line1 =>
                  contactDetails.addressLine2.map(line2 => s"$line1 $line2")
                ),
                city = contactDetails.addressLine4,
                countryCode = contactDetails.countryCode,
                postalCode = contactDetails.postalCode,
                telephoneNumber = contactDetails.telephone,
                faxNumber = None,
                emailAddress = contactDetails.emailAddress
              )
            )
          case None                 => Invalid(NonEmptyList.one("could not find contact details"))
        }
      case None                   => Invalid(NonEmptyList.one("could not find consignee details"))
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

  def setEntryConsigneeDetails(
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

  def setEntryDeclarationDetails(
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
    (maybeBankDetails, completeClaim.enteredBankDetails) match {
      case (_, Some(CompleteBankAccountDetailAnswer(bankAccountDetails))) =>
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
      case (Some(bankDetails), None)                                      =>
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
      case _                                                              => invalid("could not build bank details")
    }

  def buildEntryBankDetails(
    maybeCompleteBankAccountDetailAnswer: Option[CompleteBankAccountDetailAnswer]
  ): Validation[BankDetails] =
    maybeCompleteBankAccountDetailAnswer match {
      case Some(completeBankAccountDetailAnswer) =>
        Valid(
          BankDetails(
            consigneeBankDetails = None,
            declarantBankDetails = Some(
              BankDetail(
                completeBankAccountDetailAnswer.bankAccountDetails.accountName.value,
                completeBankAccountDetailAnswer.bankAccountDetails.sortCode.value,
                completeBankAccountDetailAnswer.bankAccountDetails.accountNumber.value
              )
            )
          )
        )
      case None                                  => invalid("Could not find entered bank details")
    }

  def setNdrcDetails(claims: List[Claim]): Validation[List[NdrcDetails]] = {
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
    }
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

  def setEntryAcceptanceDate(
    completeDeclarationDetailsAnswer: CompleteDeclarationDetailsAnswer
  ): Validation[String] =
    TimeUtils.fromEntryDisplayAcceptanceDateFormat(
      completeDeclarationDetailsAnswer.declarationDetails.dateOfImport.value.toString
    ) match {
      case Some(acceptanceDate) => Valid(acceptanceDate)
      case None                 => invalid("Could not format display acceptance date")
    }

  def setDuplicateEntryAcceptanceDate(
    completeDuplicateDeclarationDetailsAnswer: CompleteDuplicateDeclarationDetailsAnswer
  ): Validation[String] =
    completeDuplicateDeclarationDetailsAnswer.duplicateDeclaration match {
      case Some(value) =>
        TimeUtils.fromDisplayAcceptanceDateFormat(
          value.dateOfImport.value.toString
        ) match {
          case Some(acceptanceDate) => Valid(acceptanceDate)
          case None                 => invalid("Could not format display acceptance date for duplicate entry")
        }
      case None        => invalid("could not find acceptance date")
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
          setNdrcDetails(completeClaim.claims)
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
          buildBankDetails(displayDeclaration.displayResponseDetail.bankDetails, completeClaim),
          setNdrcDetails(completeClaim.claims)
        ).mapN { case (declarationDetails, consigneeDetails, bankDetails, ndrcDetails) =>
          Some(
            MrnDetail(
              MRNNumber = Some(displayDeclaration.displayResponseDetail.declarationId),
              acceptanceDate = Some(displayDeclaration.displayResponseDetail.acceptanceDate),
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

  def setEntryReferenceNumber(completeClaim: CompleteClaim): Validation[String] =
    completeClaim.movementReferenceNumber match {
      case Left(value) => Valid(value.value)
      case _           => invalid("could not find entry reference number")
    }

  def setEntryDetails(
    signedInUserDetails: SignedInUserDetails,
    completeClaim: CompleteClaim
  ): Validation[EntryDetail] = {
    val maybeEntryDeclarationDetailsAnswer = completeClaim.entryDeclarationDetails
    maybeEntryDeclarationDetailsAnswer match {
      case Some(entryDeclarationDetails) =>
        (
          setEntryReferenceNumber(completeClaim),
          setEntryAcceptanceDate(entryDeclarationDetails),
          setEntryDeclarationDetails(signedInUserDetails, entryDeclarationDetails),
          setEntryConsigneeDetails(signedInUserDetails, entryDeclarationDetails),
          buildEntryBankDetails(completeClaim.bankDetails),
          setNdrcDetails(completeClaim.claims)
        ).mapN { case (entryReferenceNumber, acceptanceDate, declarant, consigneeDetails, bankDetails, ndrcDetails) =>
          EntryDetail(
            entryNumber = Some(entryReferenceNumber),
            entryDate = Some(acceptanceDate),
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

  def setDuplicateEntryDetails(
    signedInUserDetails: SignedInUserDetails,
    completeClaim: CompleteClaim
  ): Validation[Option[EntryDetail]] = {
    val maybeDuplicateDeclarationDetailsAnswer
      : Option[DuplicateDeclarationDetailsAnswer.CompleteDuplicateDeclarationDetailsAnswer] =
      completeClaim.duplicateEntryDeclarationDetails

    maybeDuplicateDeclarationDetailsAnswer match {
      case Some(duplicateDeclarationDetailsAnswer) =>
        duplicateDeclarationDetailsAnswer.duplicateDeclaration match {
          case Some(_) => //FIXME
            (
              setEntryReferenceNumber(completeClaim),
              setDuplicateEntryAcceptanceDate(duplicateDeclarationDetailsAnswer),
              setDuplicateEntryDeclarationDetails(signedInUserDetails, duplicateDeclarationDetailsAnswer),
              setDuplicateEntryConsigneeDetails(signedInUserDetails, duplicateDeclarationDetailsAnswer),
              buildEntryBankDetails(completeClaim.bankDetails),
              setNdrcDetails(completeClaim.claims)
            ).mapN {
              case (entryReferenceNumber, acceptanceDate, declarant, consigneeDetails, bankDetails, ndrcDetails) =>
                Some(
                  EntryDetail(
                    entryNumber = Some(entryReferenceNumber),
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
