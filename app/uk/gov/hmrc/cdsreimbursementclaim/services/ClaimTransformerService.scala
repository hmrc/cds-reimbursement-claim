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
import com.google.inject.{ImplementedBy, Inject}
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.models
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.BankAccountDetailsAnswer.CompleteBankAccountDetailAnswer
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{Address => _, BankDetails => _, NdrcDetails => _, _}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums._
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.UUIDGenerator
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error, Validation}
import uk.gov.hmrc.cdsreimbursementclaim.services.DefaultClaimTransformerService.{buildEoriDetails, setBasisOfClaim, setDuplicateMrnDetails, setGoodsDetails, setMrnDetails}
import uk.gov.hmrc.cdsreimbursementclaim.utils.MoneyUtils.roundedTwoDecimalPlaces
import uk.gov.hmrc.cdsreimbursementclaim.utils.{Logging, TimeUtils}

import javax.inject.Singleton

@ImplementedBy(classOf[DefaultClaimTransformerService])
trait ClaimTransformerService {
  def toEisSubmitClaimRequest(claimRequest: SubmitClaimRequest): Either[Error, EisSubmitClaimRequest]
}

@Singleton
class DefaultClaimTransformerService @Inject() (uuidGenerator: UUIDGenerator)
    extends ClaimTransformerService
    with Logging {

  override def toEisSubmitClaimRequest(claimRequest: SubmitClaimRequest): Either[Error, EisSubmitClaimRequest] = {
    val requestCommon = RequestCommon(
      originatingSystem = Platform.MDTP,
      receiptDate = TimeUtils.iso8601DateTimeNow,
      acknowledgementReference = uuidGenerator.compactCorrelationId
    )
    claimRequest.completeClaim.movementReferenceNumber match {
      case Left(_)  => Left(Error("not implemented"))
      case Right(_) =>
        (
          setGoodsDetails(claimRequest.completeClaim),
          setBasisOfClaim(claimRequest.completeClaim),
          setMrnDetails(
            claimRequest.completeClaim.displayDeclaration,
            claimRequest.completeClaim
          ),
          setDuplicateMrnDetails(
            claimRequest.completeClaim.duplicateDisplayDeclaration,
            claimRequest.completeClaim
          ),
          buildEoriDetails(claimRequest.completeClaim, claimRequest.signedInUserDetails)
        ).mapN { case (goodsDetails, basisOfClaim, mrnDetails, duplicateMrnDetails, eoriDetails) =>
          val requestDetailA = RequestDetailA(
            CDFPayService = CDFPayservice.NDRC,
            dateReceived = Some(TimeUtils.isoLocalDateNow),
            claimType = Some(ClaimType.C285),
            caseType = Some(CaseType.Individual),
            customDeclarationType = Some(CustomDeclarationType.Entry),
            declarationMode = Some(DeclarationMode.ParentDeclaration),
            claimDate = Some(TimeUtils.isoLocalDateNow),
            claimAmountTotal = Some(roundedTwoDecimalPlaces(claimRequest.completeClaim.claims.total)),
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
            Error(s"could not create TPI05 EIS submit claim request: ${errors.toList.mkString("; ")}")
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
    name: String,
    line1: String,
    line2: String,
    line3: String,
    line4: String,
    line5: String,
    postalCode: String,
    countryCode: String,
    telephone: String,
    email: String
  )
  object CompareContactInformation {
    implicit val eq: Eq[CompareContactInformation]                = Eq.fromUniversalEquals[CompareContactInformation]
    val emptyCompareContactInformation: CompareContactInformation =
      CompareContactInformation("", "", "", "", "", "", "", "", "", "")
  }

  def buildEoriDetails(
    completeClaim: CompleteClaim,
    signedInUserDetails: SignedInUserDetails
  ): Validation[EoriDetails] = {

    //if the consign contact info has been ovewrriten then we overwir
    val contactInfoFromConsignee: Option[models.claim.ContactDetails] =
      completeClaim.consigneeDetails.flatMap(consigneeDetails => consigneeDetails.contactDetails)
    val claimantAsIndividualDetails                                   = completeClaim.claimantDetailsAsIndividual //contact info

    val a: CompareContactInformation = contactInfoFromConsignee match {
      case Some(contactDetails) =>
        CompareContactInformation(
          contactDetails.contactName.getOrElse("No contact name"),
          contactDetails.addressLine1.getOrElse("No address line"),
          contactDetails.addressLine2.getOrElse("No address line"),
          contactDetails.addressLine3.getOrElse("No address line"),
          contactDetails.addressLine4.getOrElse("No address line"),
          "",
          contactDetails.postalCode.getOrElse("No postcode"),
          contactDetails.countryCode.getOrElse("GB"),
          contactDetails.telephone.getOrElse("No telephone"),
          contactDetails.emailAddress.getOrElse("No email")
        )
      case None                 => CompareContactInformation.emptyCompareContactInformation
    }

    val b: CompareContactInformation = CompareContactInformation(
      claimantAsIndividualDetails.fullName,
      claimantAsIndividualDetails.contactAddress.line1,
      claimantAsIndividualDetails.contactAddress.line2.getOrElse("No address line"),
      claimantAsIndividualDetails.contactAddress.line3.getOrElse("No address line"),
      claimantAsIndividualDetails.contactAddress.line4,
      claimantAsIndividualDetails.contactAddress.line5.getOrElse("No address line"),
      claimantAsIndividualDetails.contactAddress.postcode.getOrElse("No postcode"),
      claimantAsIndividualDetails.contactAddress.country.code,
      claimantAsIndividualDetails.phoneNumber.value,
      claimantAsIndividualDetails.emailAddress.value
    )

    val buildConsigneeContactInformationWithPossibleUserInpit: Validation[ContactInformation] = if (a === b) {
      buildConsigneeContactInformation(completeClaim.consigneeDetails)
    } else {
      buildConsigneeContactInformationWithUserInput(b)
    }

    //if the consigne estb has been cahnged then we overwritt
    val claimantAsImporterDetails                       = completeClaim.claimantDetailsAsImporter //est add
    val establishmentAddressForConsigneeFromDeclaration =
      completeClaim.consigneeDetails.map(s => s.establishmentAddress)

    val c: CompareContactInformation = establishmentAddressForConsigneeFromDeclaration match {
      case Some(establishmentAddress) =>
        CompareContactInformation(
          completeClaim.consigneeDetails.map(s => s.legalName).getOrElse(""),
          establishmentAddress.addressLine1,
          establishmentAddress.addressLine2.getOrElse(""),
          establishmentAddress.addressLine3.getOrElse(""),
          "",
          "",
          establishmentAddress.postalCode.getOrElse(""),
          establishmentAddress.countryCode,
          "",
          ""
        )
      case None                       => CompareContactInformation.emptyCompareContactInformation
    }

    val d = claimantAsImporterDetails match {
      case Some(claimantDetailsAsImporterCompany) =>
        CompareContactInformation(
          claimantDetailsAsImporterCompany.companyName,
          claimantDetailsAsImporterCompany.contactAddress.line1,
          claimantDetailsAsImporterCompany.contactAddress.line2.getOrElse(""),
          claimantDetailsAsImporterCompany.contactAddress.line3.getOrElse(""),
          claimantDetailsAsImporterCompany.contactAddress.line4,
          claimantDetailsAsImporterCompany.contactAddress.line5.getOrElse(""),
          claimantDetailsAsImporterCompany.contactAddress.postcode.getOrElse(""),
          claimantDetailsAsImporterCompany.contactAddress.country.code,
          claimantDetailsAsImporterCompany.phoneNumber.value,
          claimantDetailsAsImporterCompany.emailAddress.value
        )
      case None                                   => CompareContactInformation.emptyCompareContactInformation
    }

    val buildConsigneeEstablishmentContactDetails: Validation[Address] = if (c === d) {
      buildConsigneeCdsEstablishmentAddress(completeClaim.displayDeclaration)
    } else {
      buildConsigneeEstablishmentAddressWithUserInput(d)
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
    Valid(
      Address(
        contactPerson = Some(userInput.name),
        addressLine1 = Some(userInput.line1),
        addressLine2 = Some(userInput.line2),
        AddressLine3 = Some(userInput.line3),
        street = Some(s"${userInput.line1} ${userInput.line2}"),
        city = Some(userInput.line4),
        countryCode = userInput.countryCode,
        postalCode = Some(userInput.postalCode),
        telephone = Some(userInput.telephone),
        emailAddress = Some(userInput.email)
      )
    )

  def buildConsigneeContactInformationWithUserInput(
    userInput: CompareContactInformation
  ): Validation[ContactInformation] =
    Valid(
      ContactInformation(
        contactPerson = Some(userInput.name),
        addressLine1 = Some(userInput.line1),
        addressLine2 = Some(userInput.line2),
        addressLine3 = Some(userInput.line3),
        street = Some(s"${userInput.line1} ${userInput.line2}"),
        city = Some(userInput.line4),
        countryCode = Some(userInput.countryCode),
        postalCode = Some(userInput.postalCode),
        telephoneNumber = Some(userInput.telephone),
        faxNumber = None,
        emailAddress = Some(userInput.email)
      )
    )
  def buildDeclarantCdsEstablishmentAddress(
    maybeDisplayDeclaration: Option[models.claim.DisplayDeclaration]
  ): Validation[Address]            =
    maybeDisplayDeclaration match {
      case Some(displayDeclaration) =>
        Valid(
          Address(
            contactPerson = Some(displayDeclaration.displayResponseDetail.declarantDetails.legalName),
            addressLine1 =
              Some(displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.addressLine1),
            addressLine2 = displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.addressLine2,
            AddressLine3 = displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.addressLine3,
            street = Some(displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.addressLine1)
              .flatMap(line1 =>
                displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.addressLine2.map(line2 =>
                  s"$line1 $line2"
                )
              ),
            city = displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.addressLine3,
            countryCode = displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.countryCode,
            postalCode = displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.postalCode,
            telephone = None,
            emailAddress = None
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
                countryCode = consigneeDetails.establishmentAddress.countryCode,
                postalCode = consigneeDetails.establishmentAddress.postalCode,
                telephone = None,
                emailAddress = None
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

  def setBasisOfClaim(completeClaim: CompleteClaim): Validation[String] = completeClaim.basisOfClaim match {
    case Some(value) => Valid(value)
    case None        => Invalid(NonEmptyList.one("could not find basis of claim"))
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
      case _                                                              => Invalid(NonEmptyList.one("could not build bank details"))
    }

  def setNdrcDetails(claims: List[Claim]): Validation[List[NdrcDetails]] =
    Valid(claims.map { claim =>
      NdrcDetails(
        paymentMethod = claim.paymentMethod,
        paymentReference = claim.paymentReference,
        CMAEligible = None,
        taxType = claim.taxCode.description,
        amount = claim.paidAmount.setScale(2).toString(),
        claimAmount = Some(claim.claimAmount.setScale(2).toString())
      )
    })

  def setMrnDetails(
    maybeDisplayDeclaration: Option[models.claim.DisplayDeclaration],
    completeClaim: CompleteClaim
  ): Validation[MrnDetail] =
    maybeDisplayDeclaration match {
      case Some(displayDeclaration) =>
        (
          setDeclarantDetails(displayDeclaration.displayResponseDetail.declarantDetails),
          setConsigneeDetails(displayDeclaration.displayResponseDetail.consigneeDetails),
          buildBankDetails(displayDeclaration.displayResponseDetail.bankDetails, completeClaim),
          setNdrcDetails(completeClaim.claims)
        ).mapN { case (declarationDetails, consigneeDetails, bankDetails, ndrcDetails) =>
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

}
