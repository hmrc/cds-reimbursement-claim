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
import cats.syntax.apply._
import cats.syntax.either._
import com.google.inject.{ImplementedBy, Inject}
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{Address => _, NdrcDetails => _, _}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.UUIDGenerator
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error, Validation}
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
          DefaultClaimTransformerService.setGoodsDetails(claimRequest.completeClaim),
          DefaultClaimTransformerService.setBasisOfClaim(claimRequest.completeClaim),
          DefaultClaimTransformerService.setMrnDetails(
            claimRequest.completeClaim.displayDeclaration,
            claimRequest.completeClaim
          ),
          DefaultClaimTransformerService.setMrnDetails(
            claimRequest.completeClaim.duplicateDisplayDeclaration,
            claimRequest.completeClaim
          ),
          DefaultClaimTransformerService.buildEoriDetails(claimRequest.completeClaim)
        ).mapN { case (goodsDetails, basisOfClaim, mrnDetails, duplicateMrnDetails, eoriDetails) =>
          val requestDetailA = RequestDetailA(
            CDFPayservice = CDFPayservice.NDRC,
            dateReceived = Some(TimeUtils.isoLocalDateNow),
            claimType = Some(ClaimType.C285),
            caseType = Some(CaseType.Individual),
            customDeclarationType = Some(CustomDeclarationType.Entry),
            declarationMode = Some(DeclarationMode.ParentDeclaration),
            claimDate = Some(TimeUtils.isoLocalDateNow),
            claimAmountTotal = Some(claimRequest.completeClaim.claims.total.toString), //TODO: format 13,2
            disposalMethod = None,
            reimbursementMethod = Some(ReimbursementMethod.BankTransfer),
            basisOfClaim = Some(basisOfClaim),
            claimant = Some(claimRequest.completeClaim.declarantType.declarantType.toString), //TODo: checl
            payeeIndicator = Some(claimRequest.completeClaim.declarantType.declarantType.toString), //TODO: check
            newEORI = None,
            newDAN = None,
            authorityTypeProvided = None,
            ClaimantEORI = Some(claimRequest.userDetails.eori.value),
            claimantEmailAddress = Some(claimRequest.userDetails.email.value),
            goodsDetails = Some(goodsDetails),
            EORIDetails = Some(eoriDetails)
          )

          val requestDetailB = RequestDetailB(
            MRNDetails = Some(List(mrnDetails)),
            duplicateMRNDetails = Some(duplicateMrnDetails),
            entryDetails = None,
            duplicateEntryDetails = None
          )

          val postNewClaimsRequest = PostNewClaimsRequest(
            requestCommon = requestCommon,
            requestDetail = RequestDetail(requestDetailA, requestDetailB)
          )
          EisSubmitClaimRequest(postNewClaimsRequest)

        }.toEither
          .leftMap(errors => Error(s"could not create TPI05 submit claim request: ${errors.toList.mkString("; ")}"))
    }
  }

}

object DefaultClaimTransformerService {

  def buildEoriDetails(completeClaim: CompleteClaim): Validation[EoriDetails] =
    (
      setConsigneeCdsEstablishmentAddress(completeClaim.displayDeclaration),
      setDeclarantCdsEstablishmentAddress(completeClaim.displayDeclaration)
    ).mapN { case (consignee, declarant) =>
      EoriDetails(
        agentEORIDetails = EORIInformation(
          EORINumber = "None", //todo: check
          CDSFullName = None,
          legalEntityType = None,
          EORIStartDate = None,
          CDSEstablishmentAddress = consignee,
          contactInformation = None,
          VATDetails = None
        ),
        ImporterEORIDetails = EORIInformation(
          EORINumber = "None", //todo: check
          CDSFullName = None,
          legalEntityType = None,
          EORIStartDate = None,
          CDSEstablishmentAddress = declarant,
          contactInformation = None,
          VATDetails = None
        )
      )
    }

  def setDeclarantCdsEstablishmentAddress(
    maybeDisplayDeclaration: Option[DisplayDeclaration]
  ): Validation[Address] =
    maybeDisplayDeclaration match {
      case Some(displayDeclaration) =>
        Valid(
          Address(
            contactPerson = Some(displayDeclaration.displayResponseDetail.declarantDetails.legalName),
            addressLine1 =
              Some(displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.addressLine1),
            addressLine2 = displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.addressLine2,
            addressLine3 = displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.addressLine3,
            street = Some(displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.addressLine1)
              .flatMap(line1 =>
                displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.addressLine2.map(line2 =>
                  s"$line1 $line2"
                )
              ),
            city = displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.addressLine3,
            countryCode = displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.countryCode,
            postalCode = displayDeclaration.displayResponseDetail.declarantDetails.establishmentAddress.postalCode,
            telephoneNumber = None,
            emailAddress = None
          )
        )
      case None                     => Invalid(NonEmptyList.one("could not find address"))
    }

  def setConsigneeCdsEstablishmentAddress(
    maybeDisplayDeclaration: Option[DisplayDeclaration]
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
                addressLine3 = consigneeDetails.establishmentAddress.addressLine3,
                street = Some(consigneeDetails.establishmentAddress.addressLine1).flatMap(line1 =>
                  consigneeDetails.establishmentAddress.addressLine2.map(line2 => s"$line1 $line2")
                ),
                city = consigneeDetails.establishmentAddress.addressLine3,
                countryCode = consigneeDetails.establishmentAddress.countryCode,
                postalCode = consigneeDetails.establishmentAddress.postalCode,
                telephoneNumber = None,
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
        DescOfGoods = Some(completeClaim.commodityDetails)
      )
    )

  def setBasisOfClaim(completeClaim: CompleteClaim): Validation[String] = completeClaim.basisOfClaim match {
    case Some(value) => Valid(value)
    case None        => Invalid(NonEmptyList.one("could not find basis of claim"))
  }

  def setConsigneeEstablishmentAddress(
    consigneeDetails: declaration.response.ConsigneeDetails
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
              addressLine3 = None,
              street = contactDetails.addressLine1.flatMap(line1 =>
                contactDetails.addressLine2.map(line2 => s"$line1 $line2")
              ),
              city = contactDetails.addressLine3,
              countryCode = countryCode,
              postalCode = contactDetails.postalCode,
              telephoneNumber = contactDetails.telephone,
              emailAddress = contactDetails.emailAddress
            )
          )
        )
      case None                 => Invalid(NonEmptyList.one("no contact details"))
    }

  def setDeclarantEstablishmentAddress(
    declarantDetails: declaration.response.DeclarantDetails
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
              addressLine3 = None,
              street = contactDetails.addressLine1.flatMap(line1 =>
                contactDetails.addressLine2.map(line2 => s"$line1 $line2")
              ),
              city = contactDetails.addressLine3,
              countryCode = countryCode,
              postalCode = contactDetails.postalCode,
              telephoneNumber = contactDetails.telephone,
              emailAddress = contactDetails.emailAddress
            )
          )
        )
      case None                 => Invalid(NonEmptyList.one("no contact details"))
    }

  def setDeclarantContactInformation(
    declarantDetails: declaration.response.DeclarantDetails
  ): Validation[ContactInformation] =
    declarantDetails.contactDetails match {
      case Some(contactDetails) =>
        Valid(
          ContactInformation(
            contactPerson = contactDetails.contactName,
            addressLine1 = contactDetails.addressLine1,
            addressLine2 = contactDetails.addressLine2,
            addressLine3 = contactDetails.addressLine3,
            street =
              contactDetails.addressLine1.flatMap(line1 => contactDetails.addressLine2.map(line2 => s"$line1 $line2")),
            city = contactDetails.addressLine4,
            countryCode = contactDetails.countryCode,
            postalCode = contactDetails.postalCode,
            telephoneNumber = contactDetails.telephone,
            faxNumber = None,
            emailAddress = contactDetails.emailAddress
          )
        )
      case None                 => Invalid(NonEmptyList.one("could not find contact information"))
    }

  def setConsigneeContactInformation(
    consigneeDetails: declaration.response.ConsigneeDetails
  ): Validation[ContactInformation] =
    consigneeDetails.contactDetails match {
      case Some(contactDetails) =>
        Valid(
          ContactInformation(
            contactPerson = contactDetails.contactName,
            addressLine1 = contactDetails.addressLine1,
            addressLine2 = contactDetails.addressLine2,
            addressLine3 = contactDetails.addressLine3,
            street =
              contactDetails.addressLine1.flatMap(line1 => contactDetails.addressLine2.map(line2 => s"$line1 $line2")),
            city = contactDetails.addressLine4,
            countryCode = contactDetails.countryCode,
            postalCode = contactDetails.postalCode,
            telephoneNumber = contactDetails.telephone,
            faxNumber = None,
            emailAddress = contactDetails.emailAddress
          )
        )
      case None                 => Invalid(NonEmptyList.one("could not find contact information"))
    }

  def setDeclarantDetails(declarantDetails: declaration.response.DeclarantDetails): Validation[MRNInformation] =
    (setDeclarantEstablishmentAddress(declarantDetails), setDeclarantContactInformation(declarantDetails)).mapN {
      case (establishmentAddress, contactInformation) =>
        MRNInformation(
          EORI = declarantDetails.declarantEORI,
          legalName = declarantDetails.legalName,
          establishmentAddress = establishmentAddress,
          contactDetails = contactInformation
        )
    }

  def setConsigneeDetails(
    maybeConsigneeDetails: Option[declaration.response.ConsigneeDetails]
  ): Validation[MRNInformation] =
    maybeConsigneeDetails match {
      case Some(consigneeDetails) =>
        (setConsigneeEstablishmentAddress(consigneeDetails), setConsigneeContactInformation(consigneeDetails)).mapN {
          case (establishmentAddress, contactInformation) =>
            MRNInformation(
              EORI = consigneeDetails.consigneeEORI,
              legalName = consigneeDetails.legalName,
              establishmentAddress = establishmentAddress,
              contactDetails = contactInformation
            )
        }
      case None                   => Invalid(NonEmptyList.one("could not find consignee details"))
    }

  //TODO: check if bank details have been overwrriten
  def setBankDetails(maybeBankDetails: Option[declaration.response.BankDetails]): Validation[BankDetails] =
    maybeBankDetails match {
      case Some(bankDetails) =>
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
      case None              => Invalid(NonEmptyList.one("no bank details found"))
    }

  def setNdrcDetails(claims: List[Claim]): Validation[List[NdrcDetails]] =
    Valid(claims.map { claim =>
      NdrcDetails(
        paymentMethod = claim.paymentMethod,
        paymentReference = claim.paymentReference,
        CMAEligible = None,
        taxType = claim.taxCode.description,
        amount = claim.paidAmount.toString(),
        claimantAmount = Some(claim.claimAmount.toString())
      )
    })

  def setMrnDetails(
    maybeDisplayDeclaration: Option[DisplayDeclaration],
    completeClaim: CompleteClaim
  ): Validation[MrnDetail] =
    maybeDisplayDeclaration match {
      case Some(displayDeclaration) =>
        (
          setDeclarantDetails(displayDeclaration.displayResponseDetail.declarantDetails),
          setConsigneeDetails(displayDeclaration.displayResponseDetail.consigneeDetails),
          setBankDetails(displayDeclaration.displayResponseDetail.bankDetails),
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

}
