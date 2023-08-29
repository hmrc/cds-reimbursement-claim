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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim

import cats.data.Validated.{Valid, invalidNel, validNel}
import cats.data.ValidatedNel
import cats.implicits.{catsSyntaxOption, toTraverseOps}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.Street
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.AcceptanceDate
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.{AccountDetails, BankAccountDetails, ConsigneeDetails, DeclarantDetails}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.MRN
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.Claimant

final case class MrnDetail(
  MRNNumber: Option[MRN] = None,
  acceptanceDate: Option[String] = None,
  declarantReferenceNumber: Option[String] = None,
  mainDeclarationReference: Option[Boolean] = None,
  procedureCode: Option[String] = None,
  declarantDetails: Option[MRNInformation] = None,
  accountDetails: Option[List[AccountDetail]] = None,
  consigneeDetails: Option[MRNInformation] = None,
  bankDetails: Option[BankDetails] = None,
  NDRCDetails: Option[List[NdrcDetails]] = None
)

object MrnDetail {

  def build: Builder = Builder(Valid(MrnDetail()))

  final case class Builder(validated: ValidatedNel[Error, MrnDetail]) extends AnyVal {

    def withMrnNumber(mrn: MRN): Builder =
      copy(validated.map(_.copy(MRNNumber = Some(mrn))))

    def withAcceptanceDate(acceptanceDate: String): Builder =
      copy(
        validated.andThen(mrnDetails =>
          AcceptanceDate
            .fromDisplayFormat(acceptanceDate)
            .flatMap(_.toTpi05DateString)
            .fold(
              throwable => invalidNel(Error(s"Error formatting acceptance date: ${throwable.getMessage}")),
              date => validNel(mrnDetails.copy(acceptanceDate = Some(date)))
            )
        )
      )

    def withDeclarantReferenceNumber(maybeDeclarantReferenceNumber: Option[String]): Builder =
      copy(validated.map(_.copy(declarantReferenceNumber = maybeDeclarantReferenceNumber)))

    def withWhetherMainDeclarationReference(whetherMainDeclarationReference: Boolean): Builder =
      copy(validated.map(_.copy(mainDeclarationReference = Some(whetherMainDeclarationReference))))

    def withProcedureCode(procedureCode: String): Builder =
      copy(validated.map(_.copy(procedureCode = Some(procedureCode))))

    def withDeclarantDetails(declarantDetails: DeclarantDetails): Builder =
      copy(validated.andThen { mrnDetails =>
        Valid(
          mrnDetails.copy(declarantDetails =
            Some(
              MRNInformation(
                EORI = declarantDetails.EORI,
                legalName = declarantDetails.legalName,
                establishmentAddress = Address(
                  contactPerson = None,
                  addressLine1 = Some(declarantDetails.establishmentAddress.addressLine1),
                  addressLine2 = declarantDetails.establishmentAddress.addressLine2,
                  addressLine3 = declarantDetails.establishmentAddress.addressLine3,
                  street = Street.fromLines(
                    Option(declarantDetails.establishmentAddress.addressLine1),
                    declarantDetails.establishmentAddress.addressLine2
                  ),
                  city = declarantDetails.establishmentAddress.addressLine3,
                  countryCode = declarantDetails.establishmentAddress.countryCode,
                  postalCode = declarantDetails.establishmentAddress.postalCode,
                  telephoneNumber = None,
                  emailAddress = None
                ),
                contactDetails = declarantDetails.contactDetails.map(contactDetails =>
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
              )
            )
          )
        )
      })

    def withConsigneeDetails(maybeConsigneeDetails: Option[ConsigneeDetails]): Builder =
      copy(validated.andThen { mrnDetails =>
        maybeConsigneeDetails
          .toValidNel(Error("The consignee details are missing"))
          .andThen { consigneeDetails =>
            Valid(
              mrnDetails.copy(consigneeDetails =
                Some(
                  MRNInformation(
                    EORI = consigneeDetails.EORI,
                    legalName = consigneeDetails.legalName,
                    establishmentAddress = Address(
                      contactPerson = None,
                      addressLine1 = Some(consigneeDetails.establishmentAddress.addressLine1),
                      addressLine2 = consigneeDetails.establishmentAddress.addressLine2,
                      addressLine3 = consigneeDetails.establishmentAddress.addressLine3,
                      street = Street.fromLines(
                        Option(consigneeDetails.establishmentAddress.addressLine1),
                        consigneeDetails.establishmentAddress.addressLine2
                      ),
                      city = consigneeDetails.establishmentAddress.addressLine3,
                      countryCode = consigneeDetails.establishmentAddress.countryCode,
                      postalCode = consigneeDetails.establishmentAddress.postalCode,
                      telephoneNumber = None,
                      emailAddress = None
                    ),
                    contactDetails = consigneeDetails.contactDetails.map(contactInformation =>
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
                  )
                )
              )
            )
          }
      })

    def withFirstNonEmptyBankDetailsWhen(isTrue: Boolean)(
      maybeBankDetails: Option[response.BankDetails],
      maybeBankAccountDetails: Option[BankAccountDetails],
      claimantType: Claimant
    ): Builder =
      if (isTrue) withFirstNonEmptyBankDetails(maybeBankDetails, maybeBankAccountDetails, claimantType) else this

    def withFirstNonEmptyBankDetails(
      maybeBankDetails: Option[response.BankDetails],
      maybeBankAccountDetails: Option[BankAccountDetails],
      claimantType: Claimant
    ): Builder = {

      def selectBankDetails: Option[BankDetails] =
        (maybeBankDetails, maybeBankAccountDetails) match {
          case (acc14BankDetailsOpt, Some(bankAccountDetails))   =>
            Some(
              claimantType match {
                case Claimant.Importer       =>
                  BankDetails(
                    consigneeBankDetails = Some(BankDetail.from(bankAccountDetails)),
                    declarantBankDetails = acc14BankDetailsOpt.flatMap(_.declarantBankDetails.map(BankDetail.from))
                  )
                case Claimant.Representative =>
                  BankDetails(
                    consigneeBankDetails = acc14BankDetailsOpt.flatMap(_.consigneeBankDetails.map(BankDetail.from)),
                    declarantBankDetails = Some(BankDetail.from(bankAccountDetails))
                  )
              }
            )
          case (Some(acc14BankDetails: response.BankDetails), _) =>
            Some(
              BankDetails(
                declarantBankDetails = acc14BankDetails.declarantBankDetails.map(BankDetail.from),
                consigneeBankDetails = acc14BankDetails.consigneeBankDetails.map(BankDetail.from)
              )
            )
          case _                                                 => None
        }

      copy(validated.map(_.copy(bankDetails = selectBankDetails)))
    }

    def withAccountDetails(maybeAccountDetails: Option[List[AccountDetails]]): Builder =
      maybeAccountDetails.fold(this)(accountDetails =>
        copy(
          validated.map(
            _.copy(accountDetails = Some(accountDetails.map { accountDetail =>
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
            }))
          )
        )
      )

    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    def withNdrcDetails(validatedNdrcDetails: List[ValidatedNel[Error, NdrcDetails]]): Builder =
      copy(
        validated.andThen { mrnDetails =>
          validatedNdrcDetails
            .ensuring(_.nonEmpty)
            .sequence
            .map(ndrcDetails => mrnDetails.copy(NDRCDetails = Some(ndrcDetails)))
        }
      )
  }

  implicit val format: OFormat[MrnDetail] = Json.format[MrnDetail]
}
