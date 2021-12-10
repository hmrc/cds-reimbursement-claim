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
import cats.data.Validated.{Invalid, Valid, invalidNel}
import cats.implicits.{catsSyntaxTuple2Semigroupal, toBifunctorOps}
import cats.kernel.Eq
import cats.syntax.apply._
import cats.syntax.eq._
import cats.syntax.traverse._
import com.google.inject.{ImplementedBy, Inject}
import configs.syntax._
import play.api.Configuration
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.models
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ReimbursementMethodAnswer.CurrentMonthAdjustment
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{ClaimedReimbursementsAnswer, Address => _, BankDetails => _, NdrcDetails => _, _}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.{ISO8601DateTime, ISOLocalDate}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.BasisOfClaimAnswer.IncorrectAdditionalInformationCode
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums._
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.{MRN, UUIDGenerator}
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error, Validation}
import uk.gov.hmrc.cdsreimbursementclaim.services.DefaultClaimTransformerService._
import uk.gov.hmrc.cdsreimbursementclaim.utils.DataUtils._
import uk.gov.hmrc.cdsreimbursementclaim.utils.{BigDecimalOps, Logging, TimeUtils}

import javax.inject.Singleton

@ImplementedBy(classOf[DefaultClaimTransformerService])
trait ClaimTransformerService {
  def toEisSubmitClaimRequest(claimRequest: C285ClaimRequest): Either[Error, EisSubmitClaimRequest]
}

@Singleton
class DefaultClaimTransformerService @Inject() (configuration: Configuration)
    extends ClaimTransformerService
    with Logging {

  private val enableCorrectAdditionalInformationCodeMappingFlag: String =
    "enable-correct-additional-information-code-mapping"

  def getFeatureFlag[A : configs.ConfigReader](key: String): A =
    configuration.underlying
      .get[A](s"feature.$key")
      .value

  private def buildMrnNumberPayload(
    submitClaimRequest: C285ClaimRequest
  ): Either[Error, RequestDetail] = {
    val localDateNow = ISOLocalDate.now

    val c285Claim = submitClaimRequest.claim
    (
      makeReasonAndOrBasisOfClaim(
        c285Claim.basisOfClaimAnswer,
        getFeatureFlag[Boolean](enableCorrectAdditionalInformationCodeMappingFlag)
      ),
      setMrnDetails(c285Claim),
      setDuplicateMrnDetails(
        c285Claim.duplicateDisplayDeclaration,
        c285Claim
      ),
      buildEoriDetails(submitClaimRequest.claim)
    ).mapN { case (maybeReasonAndOrBasis, mrnDetails, duplicateMrnDetails, eoriDetails) =>
      RequestDetail(
        CDFPayService = CDFPayService.NDRC,
        dateReceived = Some(localDateNow),
        claimType = Some(ClaimType.C285),
        caseType = setCaseType(c285Claim),
        customDeclarationType = Some(CustomDeclarationType.MRN),
        declarationMode = setDeclarationMode(c285Claim),
        claimDate = Some(localDateNow),
        claimAmountTotal = Some(submitClaimRequest.claim.totalReimbursementAmount.roundToTwoDecimalPlaces),
        disposalMethod = None,
        reimbursementMethod = setReimbursementMethod(c285Claim),
        basisOfClaim = maybeReasonAndOrBasis,
        claimant = Some(
          DefaultClaimTransformerService.setPayeeIndicator(
            submitClaimRequest.claim.declarantTypeAnswer
          )
        ),
        payeeIndicator = Some(
          DefaultClaimTransformerService.setPayeeIndicator(
            submitClaimRequest.claim.declarantTypeAnswer
          )
        ),
        newEORI = None,
        newDAN = None,
        authorityTypeProvided = None,

        claimantEORI = Some(submitClaimRequest.signedInUserDetails.eori.value),
        claimantEmailAddress = submitClaimRequest.signedInUserDetails.email.map(email => email.value),

        goodsDetails = Some(
          makeGoodsDetails(
            c285Claim.declarantTypeAnswer,
            c285Claim.commodityDetailsAnswer
          )
        ),
        EORIDetails = Some(eoriDetails),
        MRNDetails = Some(mrnDetails),
        duplicateMRNDetails = duplicateMrnDetails
      )
    }.toEither
      .leftMap { errors =>
        Error(s"Could not create TPI05 EIS submit claim request for mrn journey: ${errors.toList.mkString("; ")}")
      }
  }

  override def toEisSubmitClaimRequest(
    submitClaimRequest: C285ClaimRequest
  ): Either[Error, EisSubmitClaimRequest] = {

    val requestCommon = RequestCommon(
      originatingSystem = Platform.MDTP,
      receiptDate = ISO8601DateTime.now,
      acknowledgementReference = UUIDGenerator.compactCorrelationId
    )

    buildMrnNumberPayload(submitClaimRequest).bimap(
      error => Error(s"validation errors: ${error.toString}"),
      requestDetail =>
        EisSubmitClaimRequest(
          PostNewClaimsRequest(
            requestCommon = requestCommon,
            requestDetail = requestDetail
          )
        )
    )
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

  def buildEoriDetails(c285Claim: C285Claim): Validation[EoriDetails] = {

    val agentContactInfo = (c285Claim.mrnContactDetailsAnswer, c285Claim.mrnContactAddressAnswer)
      .mapN { (contactDetails, contactAddress) =>
        ContactInformation(
          contactPerson = Option(contactDetails.fullName),
          addressLine1 = Option(contactAddress.line1),
          addressLine2 = contactAddress.line2,
          addressLine3 = contactAddress.line3,
          street = buildStreet(Option(contactAddress.line1), contactAddress.line2),
          city = Option(contactAddress.line4),
          countryCode = Option(contactAddress.country.code),
          postalCode = Option(contactAddress.postcode.value),
          telephoneNumber = contactDetails.phoneNumber.map(_.value),
          faxNumber = None,
          emailAddress = Option(contactDetails.emailAddress.value)
        )
      }
      .getOrElse(
        c285Claim.declarantTypeAnswer match {
          case DeclarantTypeAnswer.Importer | DeclarantTypeAnswer.AssociatedWithImporterCompany =>
            ContactInformation(
              contactPerson = c285Claim.consigneeDetails.map(_.legalName),
              addressLine1 = c285Claim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.addressLine1),
              addressLine2 = c285Claim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.addressLine2),
              addressLine3 = c285Claim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.addressLine3),
              street = buildStreet(
                c285Claim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.addressLine1),
                c285Claim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.addressLine2)
              ),
              city = c285Claim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.addressLine3),
              postalCode = c285Claim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.postalCode),
              countryCode = c285Claim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.countryCode),
              telephoneNumber = c285Claim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.telephone),
              faxNumber = None,
              emailAddress = c285Claim.consigneeDetails.flatMap(_.contactDetails).flatMap(_.emailAddress)
            )
          case DeclarantTypeAnswer.AssociatedWithRepresentativeCompany                          =>
            ContactInformation(
              contactPerson = c285Claim.declarantDetails.map(_.legalName),
              addressLine1 = c285Claim.declarantDetails.flatMap(_.contactDetails).flatMap(_.addressLine1),
              addressLine2 = c285Claim.declarantDetails.flatMap(_.contactDetails).flatMap(_.addressLine2),
              addressLine3 = c285Claim.declarantDetails.flatMap(_.contactDetails).flatMap(_.addressLine3),
              street = buildStreet(
                c285Claim.declarantDetails.flatMap(_.contactDetails).flatMap(_.addressLine1),
                c285Claim.declarantDetails.flatMap(_.contactDetails).flatMap(_.addressLine2)
              ),
              city = c285Claim.declarantDetails.flatMap(_.contactDetails).flatMap(_.addressLine3),
              postalCode = c285Claim.declarantDetails.flatMap(_.contactDetails).flatMap(_.postalCode),
              countryCode = c285Claim.declarantDetails.flatMap(_.contactDetails).flatMap(_.countryCode),
              telephoneNumber = c285Claim.declarantDetails.flatMap(_.contactDetails).flatMap(_.telephone),
              faxNumber = None,
              emailAddress = c285Claim.declarantDetails.flatMap(_.contactDetails).flatMap(_.emailAddress)
            )
        }
      )

    Valid(
      EoriDetails(
        agentEORIDetails = EORIInformation(
          EORINumber = c285Claim.declarantDetails.map(s => s.declarantEORI).getOrElse(""),
          CDSFullName = c285Claim.declarantDetails.map(s => s.legalName),
          legalEntityType = None,
          EORIStartDate = None,
          CDSEstablishmentAddress = Address(
            contactPerson = Option(c285Claim.detailsRegisteredWithCdsAnswer.fullName),
            addressLine1 = Option(c285Claim.detailsRegisteredWithCdsAnswer.contactAddress.line1),
            addressLine2 = c285Claim.detailsRegisteredWithCdsAnswer.contactAddress.line2,
            AddressLine3 = c285Claim.detailsRegisteredWithCdsAnswer.contactAddress.line3,
            street = buildStreet(
              Option(c285Claim.detailsRegisteredWithCdsAnswer.contactAddress.line1),
              c285Claim.detailsRegisteredWithCdsAnswer.contactAddress.line2
            ),
            city = Some(c285Claim.detailsRegisteredWithCdsAnswer.contactAddress.line4),
            postalCode = c285Claim.detailsRegisteredWithCdsAnswer.contactAddress.postcode,
            countryCode = c285Claim.detailsRegisteredWithCdsAnswer.contactAddress.country.code,
            telephone = None,
            emailAddress = Option(c285Claim.detailsRegisteredWithCdsAnswer.emailAddress.value)
          ),
          contactInformation = Some(agentContactInfo),
          VATDetails = None
        ),
        importerEORIDetails = EORIInformation(
          EORINumber = c285Claim.consigneeDetails.map(s => s.consigneeEORI).getOrElse(""),
          CDSFullName = c285Claim.consigneeDetails.map(s => s.legalName),
          legalEntityType = None,
          EORIStartDate = None,
          CDSEstablishmentAddress = Address(
            contactPerson = None,
            addressLine1 = c285Claim.consigneeDetails.map(_.establishmentAddress.addressLine1),
            addressLine2 = c285Claim.consigneeDetails.flatMap(_.establishmentAddress.addressLine2),
            AddressLine3 = c285Claim.consigneeDetails.flatMap(_.establishmentAddress.addressLine3),
            street = buildStreet(
              c285Claim.consigneeDetails.map(_.establishmentAddress.addressLine1),
              c285Claim.consigneeDetails.flatMap(_.establishmentAddress.addressLine2)
            ),
            city = c285Claim.consigneeDetails.flatMap(_.establishmentAddress.addressLine3),
            countryCode = c285Claim.consigneeDetails.map(_.establishmentAddress.countryCode).getOrElse("GB"),
            postalCode = c285Claim.consigneeDetails.flatMap(_.establishmentAddress.postalCode),
            telephone = c285Claim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.telephone)),
            emailAddress = c285Claim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.emailAddress))
          ),
          contactInformation = Some(
            ContactInformation(
              contactPerson = c285Claim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.contactName)),
              addressLine1 = c285Claim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine1)),
              addressLine2 = c285Claim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine2)),
              addressLine3 = c285Claim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine3)),
              street = buildStreet(
                c285Claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine1)),
                c285Claim.consigneeDetails.flatMap(_.contactDetails.flatMap(_.addressLine2))
              ),
              city = c285Claim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.addressLine3)),
              countryCode = c285Claim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.countryCode)),
              postalCode = c285Claim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.postalCode)),
              telephoneNumber = c285Claim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.telephone)),
              faxNumber = None,
              emailAddress = c285Claim.consigneeDetails.flatMap(s => s.contactDetails.flatMap(f => f.emailAddress))
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

  def makeGoodsDetails(
    declarantType: DeclarantTypeAnswer,
    commodityDetails: CommodityDetailsAnswer
  ): GoodsDetails =
    GoodsDetails(
      placeOfImport = None,
      isPrivateImporter = Some(declarantType match {
        case DeclarantTypeAnswer.Importer => "Yes"
        case _                            => "No"
      }),
      groundsForRepaymentApplication = None,
      descOfGoods = Some(commodityDetails.value)
    )

  def makeReasonAndOrBasisOfClaim(
    basisOfClaim: BasisOfClaimAnswer,
    enableCorrectAdditionalInformationCodeMapping: Boolean
  ): Validation[Option[String]] =
    if (enableCorrectAdditionalInformationCodeMapping && basisOfClaim === IncorrectAdditionalInformationCode) {
      Valid(Some(basisOfClaim.toString))
    } else if (basisOfClaim === IncorrectAdditionalInformationCode) {
      Valid(Some("Risk Classification Error"))
    } else {
      Valid(Some(basisOfClaim.toString))
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
            Invalid(NonEmptyList.one("could not find contact details to build declarant contact information"))
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

  def transformToMaybeAccountDetail(maybeAccountDetails: Option[List[AccountDetails]]): Option[List[AccountDetail]] =
    maybeAccountDetails.map((adl: List[AccountDetails]) =>
      adl.map { ad: AccountDetails =>
        AccountDetail(
          accountType = ad.accountType,
          accountNumber = ad.accountNumber,
          EORI = ad.eori,
          legalName = ad.legalName,
          contactDetails = ad.contactDetails.map { cd =>
            ContactInformation(
              contactPerson = cd.contactName,
              addressLine1 = cd.addressLine1,
              addressLine2 = cd.addressLine2,
              addressLine3 = cd.addressLine3,
              street = cd.addressLine4,
              city = None,
              countryCode = cd.countryCode,
              postalCode = cd.postalCode,
              telephoneNumber = cd.telephone,
              faxNumber = None,
              emailAddress = cd.emailAddress
            )
          }
        )
      }
    )

  def buildBankDetails(
    maybeBankDetails: Option[models.claim.BankDetails],
    c285Claim: C285Claim
  ): Validation[BankDetails] =
    (maybeBankDetails, c285Claim.bankAccountDetailsAnswer) match {
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
        Valid(
          BankDetails(
            consigneeBankDetails = bankDetails.consigneeBankDetails.map(_.toBankDetail),
            declarantBankDetails = bankDetails.declarantBankDetails.map(_.toBankDetail)
          )
        )
      case _                             => invalidNel("could not build bank details")
    }

  def makeNdrcDetails(claims: NonEmptyList[ClaimedReimbursement]): Validation[List[NdrcDetails]] = {
    val result: List[Validation[NdrcDetails]] = claims.map { claim =>
      (
        isValidPaymentMethod(claim.paymentMethod),
        isValidTaxType(claim.taxCode.value),
        isValidPaymentReference(claim.paymentReference),
        isValidAmount(claim.paidAmount.roundToTwoDecimalPlaces),
        isValidAmount(claim.claimAmount.roundToTwoDecimalPlaces)
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

    result.sequence.leftMap(errors =>
      NonEmptyList.one(s"there is at least one claim which has failed validation: ${errors.toList.mkString("|")}")
    )
  }

  def setAcceptanceDate(
    displayAcceptanceDate: String
  ): Validation[String] =
    TimeUtils.fromDisplayAcceptanceDateFormat(displayAcceptanceDate) match {
      case Some(acceptanceDate) => Valid(acceptanceDate)
      case None                 => invalidNel("Could not format display acceptance date")
    }

  def multipleClaimsAnswer(c285Claim: C285Claim): List[(MRN, ClaimedReimbursementsAnswer)] = {
    val mrns   = c285Claim.movementReferenceNumber :: c285Claim.associatedMRNsAnswer.toList.flatMap(_.toList)
    val claims =
      c285Claim.claimedReimbursementsAnswer :: c285Claim.associatedMRNsClaimsAnswer.toList.flatMap(_.toList)
    mrns.zip(claims)
  }

  def setMrnDetails(c285Claim: C285Claim): Validation[List[MrnDetail]] = {

    val details = c285Claim.displayDeclaration.toList.flatMap(displayDeclaration =>
      multipleClaimsAnswer(c285Claim).map { case (mrn, claim) =>
        (
          setAcceptanceDate(displayDeclaration.displayResponseDetail.acceptanceDate),
          setDeclarantDetails(displayDeclaration.displayResponseDetail.declarantDetails),
          setConsigneeDetails(displayDeclaration.displayResponseDetail.consigneeDetails),
          buildBankDetails(displayDeclaration.displayResponseDetail.bankDetails, c285Claim),
          makeNdrcDetails(claim)
        ).mapN { case (acceptanceDate, declarationDetails, consigneeDetails, bankDetails, ndrcDetails) =>
          MrnDetail(
            MRNNumber = Some(mrn.value),
            acceptanceDate = Some(acceptanceDate),
            declarantReferenceNumber = displayDeclaration.displayResponseDetail.declarantReferenceNumber,
            mainDeclarationReference = Some(c285Claim.movementReferenceNumber.value === mrn.value),
            procedureCode = Some(displayDeclaration.displayResponseDetail.procedureCode),
            declarantDetails = Some(declarationDetails),
            accountDetails = transformToMaybeAccountDetail(displayDeclaration.displayResponseDetail.accountDetails),
            consigneeDetails = Some(consigneeDetails),
            bankDetails = if (c285Claim.movementReferenceNumber.value === mrn.value) Some(bankDetails) else None,
            NDRCDetails = Some(ndrcDetails)
          )
        }
      }
    )

    details
      .ensuring(_.nonEmpty)
      .sequence
      .leftMap(errors =>
        NonEmptyList.one(s"there is at least one claim which has failed validation: ${errors.toList.mkString("|")}")
      )
  }

  def setDuplicateMrnDetails(
    maybeDisplayDeclaration: Option[models.claim.DisplayDeclaration],
    c285Claim: C285Claim
  ): Validation[Option[MrnDetail]] =
    maybeDisplayDeclaration match {
      case Some(displayDeclaration) =>
        (
          setDeclarantDetails(displayDeclaration.displayResponseDetail.declarantDetails),
          setConsigneeDetails(displayDeclaration.displayResponseDetail.consigneeDetails),
          setAcceptanceDate(displayDeclaration.displayResponseDetail.acceptanceDate),
          buildBankDetails(displayDeclaration.displayResponseDetail.bankDetails, c285Claim),
          makeNdrcDetails(c285Claim.claims)
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

  def setCaseType(c285Claim: C285Claim): Option[CaseType] =
    (c285Claim.reimbursementMethodAnswer, c285Claim.typeOfClaim) match {
      case (_, TypeOfClaimAnswer.Multiple)   => Some(CaseType.Bulk)
      case (_, TypeOfClaimAnswer.Scheduled)  => Some(CaseType.Bulk)
      case (CurrentMonthAdjustment, _)       => Some(CaseType.CMA)
      case _                                 => Some(CaseType.Individual)
    }

  def setReimbursementMethod(c285Claim: C285Claim): Option[ReimbursementMethod] =
    c285Claim.reimbursementMethodAnswer match {
      case CurrentMonthAdjustment => Some(ReimbursementMethod.Deferment)
      case _                      => Some(ReimbursementMethod.BankTransfer)
    }

  def setDeclarationMode(c285Claim: C285Claim): Option[DeclarationMode] =
    c285Claim.typeOfClaim match {
      case TypeOfClaimAnswer.Scheduled => Some(DeclarationMode.ParentDeclaration)
      case TypeOfClaimAnswer.Multiple  => Some(DeclarationMode.AllDeclaration)
      case _                           => Some(DeclarationMode.ParentDeclaration)
    }
}
