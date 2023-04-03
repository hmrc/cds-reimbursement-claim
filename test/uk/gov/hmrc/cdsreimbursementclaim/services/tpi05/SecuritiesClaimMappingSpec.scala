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

package uk.gov.hmrc.cdsreimbursementclaim.services.tpi05

import cats.implicits.catsSyntaxOptionId
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.i18n.Lang.logger
import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform.MDTP
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.SecuritiesClaim
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.CustomDeclarationType
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.{EisSubmitClaimRequest, PostNewClaimsRequest}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.email.Email
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.SecuritiesClaimGen._

class SecuritiesClaimMappingSpec
    extends AnyWordSpec
    with SecuritiesClaimSupport
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with OptionValues
    with TypeCheckedTripleEquals {

  def isValid(
    claim: SecuritiesClaim,
    declaration: DisplayDeclaration,
    result: Either[Error, EisSubmitClaimRequest]
  ): Unit =
    result match {
      case Left(error)                                                        =>
        logger.warn(s"Error message: $error")
        assert(claim.claimantInformation.contactInformation.countryCode.isEmpty)
        ()
      case Right(EisSubmitClaimRequest(PostNewClaimsRequest(common, detail))) =>
        common.originatingSystem     should ===(MDTP)
        detail.customDeclarationType should ===(CustomDeclarationType.MRN.some)
        detail.claimType             should ===(None)
        detail.claimantEORI          should ===(claim.claimantInformation.eori)
        detail.claimantEmailAddress  should ===(
          claim.claimantInformation.contactInformation.emailAddress.map(Email(_)).value
        )
        detail.claimantName          should ===(Some(claim.claimantInformation.contactInformation.contactPerson.value))
        detail.MRNDetails.toList.flatten
          .map(_.consigneeDetails.map(_.contactDetails))
          .zip(declaration.displayResponseDetail.effectiveConsigneeDetails.flatMap(_.contactDetails) :: Nil)
          .foreach { case (Some(eisContactDetails), Some(acc14ContactDetails)) =>
            eisContactDetails.addressLine1    should ===(acc14ContactDetails.addressLine1)
            eisContactDetails.addressLine2    should ===(acc14ContactDetails.addressLine2)
            eisContactDetails.addressLine3    should ===(acc14ContactDetails.addressLine3)
            eisContactDetails.contactPerson   should ===(acc14ContactDetails.contactName)
            eisContactDetails.telephoneNumber should ===(acc14ContactDetails.telephone)
            eisContactDetails.city            should ===(acc14ContactDetails.addressLine3)
            eisContactDetails.countryCode     should ===(acc14ContactDetails.countryCode)
            eisContactDetails.emailAddress    should ===(acc14ContactDetails.emailAddress)
            eisContactDetails.faxNumber       should ===(None)
          }
        detail.security
          .flatMap(_.securityDetails)
          .toList
          .flatten
          .sortBy(_.securityDepositID)
          .zip(declaration.displayResponseDetail.securityDetails.toList.flatten.sortBy(_.securityDepositId))
          .zip(claim.securitiesReclaims.toList.sortBy(_._1))
          .foreach { case ((eisSecurityDetail, acc14SecurityDetail), (reclaimDepositId, reclaimTaxes)) =>
            eisSecurityDetail.securityDepositID should ===(acc14SecurityDetail.securityDepositId)
            eisSecurityDetail.securityDepositID should ===(reclaimDepositId)
            eisSecurityDetail.paymentReference  should ===(acc14SecurityDetail.paymentReference)
            eisSecurityDetail.totalAmount       should ===(acc14SecurityDetail.totalAmount)
            eisSecurityDetail.paymentMethod     should ===(acc14SecurityDetail.paymentMethod)
            eisSecurityDetail.amountPaid        should ===(acc14SecurityDetail.amountPaid)

            eisSecurityDetail.taxDetails
              .sortBy(_.taxType)
              .zip(
                acc14SecurityDetail.taxDetails
                  .filter(t => reclaimTaxes.keys.exists(_.value === t.taxType))
                  .sortBy(_.taxType)
              )
              .zip(reclaimTaxes.toList.sortBy(_._1.value))
              .foreach { case ((eisTax, acc14Tax), (reclaimTaxCode, reclaimAmount)) =>
                List(eisTax.taxType, acc14Tax.taxType, reclaimTaxCode.value) should ===(
                  List(eisTax.taxType, eisTax.taxType, eisTax.taxType)
                )
                eisTax.amount                                                should ===(acc14Tax.amount)
                BigDecimal(eisTax.claimAmount)                               should ===(reclaimAmount)
              }

          }
        detail.claimantAddress.map { address =>
          val contactInformation = claim.claimantInformation.contactInformation
          address.contactPerson     shouldBe contactInformation.contactPerson
          address.addressLine1      shouldBe contactInformation.addressLine1
          address.addressLine2      shouldBe contactInformation.addressLine2
          address.addressLine3      shouldBe contactInformation.addressLine3
          address.street            shouldBe contactInformation.street
          address.city              shouldBe contactInformation.city
          Some(address.countryCode) shouldBe contactInformation.countryCode
          address.postalCode        shouldBe contactInformation.postalCode
          address.telephoneNumber   shouldBe contactInformation.telephoneNumber
          address.emailAddress      shouldBe contactInformation.emailAddress
        }
    }

  "The Securities claim mapper" should {
    "map a valid Securities claim to TPI05 request" in forAll(genSecuritiesClaimAndDeclaration) {
      details: (SecuritiesClaim, DisplayDeclaration) =>
        val (claim, declaration) = details
        val tpi05Request         = securitiesClaimToTPI05Mapper.map((claim, declaration))
        isValid(claim, declaration, tpi05Request)
    }

    "map a valid temporary admission Securities claim to TPI05 request" in forAll(
      genTempAdmissionSecuritiesClaimAndDeclaration
    ) { details: (SecuritiesClaim, DisplayDeclaration) =>
      val (claim, declaration) = details
      val tpi05Request         = securitiesClaimToTPI05Mapper.map((claim, declaration))
      isValid(claim, declaration, tpi05Request)
      tpi05Request.map { case EisSubmitClaimRequest(PostNewClaimsRequest(_, detail)) =>
        detail.methodOfDisposals.isDefined                                                           should ===(true)
        detail.methodOfDisposals.flatMap(_.headOption.map(_.disposalMethod))                         should ===(
          claim.temporaryAdmissionMethodOfDisposal.map(_.eisCode)
        )
        detail.methodOfDisposals.flatMap(_.headOption.flatMap(_.exportMRNs.map(_.map(_.MRNNumber)))) should ===(
          claim.exportMovementReferenceNumber.map(List(_))
        )
      }
    }

    "fail for an invalid email in Securities claim to TPI05 request" in forAll(
      genTempAdmissionSecuritiesClaimAndDeclaration
    ) { details: (SecuritiesClaim, DisplayDeclaration) =>
      val (claim, declaration) = details
      val updatedClaim         = claim
        .copy(
          claimantInformation = claim.claimantInformation
            .copy(
              contactInformation = claim.claimantInformation.contactInformation
                .copy(emailAddress = None)
            )
        )

      val tpi05Request = securitiesClaimToTPI05Mapper.map((updatedClaim, declaration))

      tpi05Request.left.map(_.value should be("claimant email address is mandatory"))

    }

    "fail for an invalid contact person in Securities claim to TPI05 request" in forAll(
      genTempAdmissionSecuritiesClaimAndDeclaration
    ) { details: (SecuritiesClaim, DisplayDeclaration) =>
      val (claim, declaration) = details
      val updatedClaim         = claim
        .copy(
          claimantInformation = claim.claimantInformation
            .copy(
              contactInformation = claim.claimantInformation.contactInformation
                .copy(contactPerson = None)
            )
        )

      val tpi05Request = securitiesClaimToTPI05Mapper.map((updatedClaim, declaration))

      tpi05Request.left.map(_.value should be("claimant contact name is mandatory"))

    }

    "fail for an invalid claimant address in Securities claim to TPI05 request" in forAll(
      genTempAdmissionSecuritiesClaimAndDeclaration
    ) { details: (SecuritiesClaim, DisplayDeclaration) =>
      val (claim, declaration) = details
      val updatedClaim         = claim
        .copy(
          claimantInformation = claim.claimantInformation
            .copy(
              contactInformation = claim.claimantInformation.contactInformation
                .copy(countryCode = None)
            )
        )

      val tpi05Request = securitiesClaimToTPI05Mapper.map((updatedClaim, declaration))

      tpi05Request.left.map(_.value should be("Claimant Address could not be parsed: country code is mandatory"))

    }
  }
}
