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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim

import cats.data.Validated.Valid
import cats.data.{Ior, ValidatedNel}
import cats.implicits.catsSyntaxOption
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.BankAccountDetails
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.{Eori, MRN}
import uk.gov.hmrc.cdsreimbursementclaim.utils.TimeUtils

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
          TimeUtils
            .fromDisplayAcceptanceDateFormat(acceptanceDate)
            .map(date => mrnDetails.copy(acceptanceDate = Some(date)))
            .toValidNel(Error("Could not format display acceptance date"))
        )
      )

    def withDeclarantReferenceNumber(maybeDeclarantReferenceNumber: Option[String]): Builder =
      copy(validated.map(_.copy(declarantReferenceNumber = maybeDeclarantReferenceNumber)))

    def withWhetherMainDeclarationReference(whetherMainDeclarationReference: Boolean): Builder =
      copy(validated.map(_.copy(mainDeclarationReference = Some(whetherMainDeclarationReference))))

    def withProcedureCode(procedureCode: String): Builder =
      copy(validated.map(_.copy(procedureCode = Some(procedureCode))))

    def withDeclarantDetails(
      eori: Eori,
      legalName: String,
      address: Address,
      maybeContactInformation: Option[ContactInformation]
    ): Builder =
      copy(validated.andThen { detail =>
        maybeContactInformation
          .toValidNel(Error("The declarant contact information is missing"))
          .map { inf =>
            detail.copy(declarantDetails =
              Some(
                MRNInformation(
                  EORI = eori,
                  legalName = legalName,
                  establishmentAddress = address,
                  contactDetails = inf
                )
              )
            )
          }
      })

    def withConsigneeDetails(maybeDetails: Option[(Eori, String, Address, Option[ContactInformation])]): Builder =
      copy(validated.andThen { detail =>
        maybeDetails
          .toValidNel(Error("The consignee details are missing"))
          .andThen { consigneeDetails =>
            consigneeDetails._4
              .toValidNel(Error("The consignee contact information is missing"))
              .map { contactInformation =>
                detail.copy(consigneeDetails =
                  Some(
                    MRNInformation(
                      EORI = consigneeDetails._1,
                      legalName = consigneeDetails._2,
                      establishmentAddress = consigneeDetails._3,
                      contactDetails = contactInformation
                    )
                  )
                )
              }
          }
      })

    def withBankDetails(option: Option[Ior[response.BankDetails, BankAccountDetails]]): Builder = ???
  }

  implicit val format: OFormat[MrnDetail] = Json.format[MrnDetail]
}
