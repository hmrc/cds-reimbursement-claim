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

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{Assertion, Assertions, Ignore, OptionValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.i18n.Lang.logger
import play.api.libs.json.Json
import uk.gov.hmrc.cdsreimbursementclaim.Fake
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{C285ClaimRequest, ClaimantInformation, ClaimantType, ClaimedReimbursement, DeclarantTypeAnswer, SingleOverpaymentsClaim, Street, TypeOfClaimAnswer}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.{ContactInformation, EisSubmitClaimRequest}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.DisplayDeclaration
import uk.gov.hmrc.cdsreimbursementclaim.models.generators.C285ClaimGen._
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response.NdrcDetails

class C285MappingCrossValidationSpec
    extends AnyWordSpec
    with C285ClaimSupport
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with OptionValues
    with TypeCheckedTripleEquals {

  def toClaimantType(declarantType: DeclarantTypeAnswer): ClaimantType =
    declarantType match {
      case DeclarantTypeAnswer.Importer                            => ClaimantType.Consignee
      case DeclarantTypeAnswer.AssociatedWithImporterCompany       => ClaimantType.Declarant
      case DeclarantTypeAnswer.AssociatedWithRepresentativeCompany => ClaimantType.User
    }

  val legacyC285RequestJson =
    "{\"id\":\"d1d56496-7840-4df7-bcca-04ed2d32adcf\",\"claim\":{\"id\":\"d1d56496-7840-4df7-bcca-04ed2d32adcf\",\"typeOfClaim\":{\"Individual\":{}},\"movementReferenceNumber\":\"01AAAAAAAAAAAAAAA1\",\"declarantTypeAnswer\":{\"Importer\":{}},\"detailsRegisteredWithCdsAnswer\":{\"fullName\":\"IT Solutions LTD\",\"emailAddress\":\"someemail@mail.com\",\"contactAddress\":{\"line1\":\"19 Bricks Road\",\"line4\":\"Newcastle\",\"postcode\":\"NE12 5BT\",\"country\":{\"code\":\"GB\"}},\"addCompanyDetails\":false},\"mrnContactDetailsAnswer\":{\"fullName\":\"Online Sales LTD\",\"emailAddress\":\"someemail@mail.com\",\"phoneNumber\":\"+4420723934397\"},\"mrnContactAddressAnswer\":{\"line1\":\"11 Mount Road\",\"line4\":\"London\",\"postcode\":\"E10 7PP\",\"country\":{\"code\":\"GB\"}},\"basisOfClaimAnswer\":{\"PersonalEffects\":{}},\"documents\":[{\"checksum\":\"2ff223d3f2fe41d24dba195178f94baac4ec1a0a89853b96987493a34be8b99a\",\"downloadUrl\":\"http://localhost:9570/upscan/download/c934183d-b889-40fb-b573-469d5088f83c\",\"fileName\":\"ACC14-1.3.0-OverpaymentDeclarationDisplay.pdf\",\"fileMimeType\":\"application/binary\",\"size\":1628473,\"uploadedOn\":\"2023-02-22T13:52:17.272927\",\"documentType\":\"AirWayBill\"}],\"additionalDetailsAnswer\":{\"value\":\"the quick brown fox jumps over the lazy dog\"},\"displayDeclaration\":{\"displayResponseDetail\":{\"declarationId\":\"01AAAAAAAAAAAAAAA1\",\"acceptanceDate\":\"12 February 2021\",\"procedureCode\":\"2\",\"declarantDetails\":{\"declarantEORI\":\"GB000000000000001\",\"legalName\":\"Foxpro Central LTD\",\"establishmentAddress\":{\"addressLine1\":\"12 Skybricks Road\",\"addressLine3\":\"Coventry\",\"postalCode\":\"CV3 6EA\",\"countryCode\":\"GB\"},\"contactDetails\":{\"addressLine1\":\"45 Church Road\",\"addressLine3\":\"Leeds\",\"postalCode\":\"LS1 2HA\",\"contactName\":\"Info Tech LTD\",\"countryCode\":\"GB\"}},\"consigneeDetails\":{\"consigneeEORI\":\"GB000000000000001\",\"legalName\":\"IT Solutions LTD\",\"establishmentAddress\":{\"addressLine1\":\"19 Bricks Road\",\"addressLine3\":\"Newcastle\",\"postalCode\":\"NE12 5BT\",\"countryCode\":\"GB\"},\"contactDetails\":{\"addressLine1\":\"11 Mount Road\",\"emailAddress\":\"automation@gmail.com\",\"addressLine3\":\"London\",\"postalCode\":\"E10 7PP\",\"contactName\":\"Online Sales LTD\",\"countryCode\":\"GB\",\"telephone\":\"+4420723934397\"}},\"accountDetails\":[{\"accountType\":\"a\",\"accountNumber\":\"1\",\"eori\":\"GB000000000000001\",\"legalName\":\"a\",\"contactDetails\":{\"addressLine1\":\"a\",\"addressLine4\":\"a\",\"emailAddress\":\"a\",\"addressLine3\":\"a\",\"postalCode\":\"a\",\"contactName\":\"a\",\"countryCode\":\"GB\",\"addressLine2\":\"a\",\"telephone\":\"a\"}},{\"accountType\":\"b\",\"accountNumber\":\"2\",\"eori\":\"GB000000000000001\",\"legalName\":\"b\",\"contactDetails\":{\"addressLine1\":\"b\",\"addressLine4\":\"b\",\"emailAddress\":\"b\",\"addressLine3\":\"b\",\"postalCode\":\"b\",\"contactName\":\"b\",\"countryCode\":\"GB\",\"addressLine2\":\"b\",\"telephone\":\"b\"}}],\"bankDetails\":{\"consigneeBankDetails\":{\"accountHolderName\":\"CDS E2E To E2E Bank\",\"sortCode\":\"308844\",\"accountNumber\":\"12345678\"},\"declarantBankDetails\":{\"accountHolderName\":\"CDS E2E To E2E Bank\",\"sortCode\":\"308844\",\"accountNumber\":\"12345678\"}},\"maskedBankDetails\":{\"consigneeBankDetails\":{\"accountHolderName\":\"CDS E2E To E2E Bank\",\"sortCode\":\"Ending with 44\",\"accountNumber\":\"Ending with 5678\"},\"declarantBankDetails\":{\"accountHolderName\":\"CDS E2E To E2E Bank\",\"sortCode\":\"Ending with 44\",\"accountNumber\":\"Ending with 5678\"}},\"ndrcDetails\":[{\"taxType\":\"A80\",\"amount\":\"218.00\",\"paymentMethod\":\"001\",\"paymentReference\":\"GB201430007000\",\"cmaEligible\":\"0\"},{\"taxType\":\"A95\",\"amount\":\"211.00\",\"paymentMethod\":\"001\",\"paymentReference\":\"GB201430007000\",\"cmaEligible\":\"1\"},{\"taxType\":\"A90\",\"amount\":\"228.00\",\"paymentMethod\":\"001\",\"paymentReference\":\"GB201430007000\",\"cmaEligible\":\"1\"},{\"taxType\":\"A85\",\"amount\":\"171.00\",\"paymentMethod\":\"001\",\"paymentReference\":\"GB201430007000\",\"cmaEligible\":\"1\"}]}},\"claimedReimbursementsAnswer\":[{\"taxCode\":\"A80\",\"paidAmount\":218,\"claimAmount\":118,\"id\":\"4c78f163-d152-450f-aa58-7d83dca91c3d\",\"paymentMethod\":\"001\",\"paymentReference\":\"GB201430007000\",\"isFilled\":true},{\"taxCode\":\"A95\",\"paidAmount\":211,\"claimAmount\":61,\"id\":\"fcd9c5c2-a21b-40be-a52d-c6d344e82f79\",\"paymentMethod\":\"001\",\"paymentReference\":\"GB201430007000\",\"isFilled\":true},{\"taxCode\":\"A90\",\"paidAmount\":228,\"claimAmount\":58,\"id\":\"c2843e82-c9bd-41ef-9cad-b5738e723da0\",\"paymentMethod\":\"001\",\"paymentReference\":\"GB201430007000\",\"isFilled\":true},{\"taxCode\":\"A85\",\"paidAmount\":171,\"claimAmount\":81,\"id\":\"f7f44caa-e033-4b5d-91d2-525aeba4f774\",\"paymentMethod\":\"001\",\"paymentReference\":\"GB201430007000\",\"isFilled\":true}],\"reimbursementMethodAnswer\":\"BankAccountTransfer\"},\"signedInUserDetails\":{\"email\":\"event-agents-external-aaaadghuc4fueomsg3kpkvdmry@hmrcdigital.slack.com\",\"eori\":\"GB000000000000001\",\"verifiedEmail\":\"someemail@mail.com\",\"contactName\":\"Jayce\"}}"
  val newC285RequestJson    =
    "{\"movementReferenceNumber\":\"01AAAAAAAAAAAAAAA1\",\"claimantType\":\"Consignee\",\"claimantInformation\":{\"eori\":\"GB000000000000001\",\"fullName\":\"IT Solutions LTD\",\"establishmentAddress\":{\"contactPerson\":\"IT Solutions LTD\",\"addressLine1\":\"19 Bricks Road\",\"street\":\"19 Bricks Road\",\"city\":\"Newcastle\",\"countryCode\":\"GB\",\"postalCode\":\"NE12 5BT\",\"emailAddress\":\"automation@gmail.com\"},\"contactInformation\":{\"contactPerson\":\"Online Sales LTD\",\"addressLine1\":\"19 Bricks Road\",\"street\":\"19 Bricks Road\",\"city\":\"Newcastle\",\"countryCode\":\"GB\",\"postalCode\":\"NE12 5BT\",\"telephoneNumber\":\"+4420723934397\",\"emailAddress\":\"automation@gmail.com\"}},\"basisOfClaim\":{\"PersonalEffects\":{}},\"whetherNorthernIreland\":false,\"additionalDetails\":\"the quick brown fox jumps over the lazy dog\",\"reimbursementClaims\":{\"A80\":\"118.00\",\"A95\":\"61.00\",\"A90\":\"58.00\",\"A85\":\"81.00\"},\"reimbursementMethod\":\"BankAccountTransfer\",\"bankAccountDetails\":{\"accountName\":\"CDS E2E To E2E Bank\",\"sortCode\":\"308844\",\"accountNumber\":\"12345678\"},\"supportingEvidences\":[{\"checksum\":\"2ff223d3f2fe41d24dba195178f94baac4ec1a0a89853b96987493a34be8b99a\",\"downloadUrl\":\"http://localhost:9570/upscan/download/f21aae22-fefb-448b-9703-0ffb1df84f09\",\"fileName\":\"ACC14-1.3.0-OverpaymentDeclarationDisplay.pdf\",\"fileMimeType\":\"application/binary\",\"size\":1628473,\"uploadedOn\":\"2023-02-22T14:01:54.47396\",\"documentType\":\"AirWayBill\"}]}"
  val delcarationJson       =
    "{\"displayResponseDetail\":{\"declarationId\":\"01AAAAAAAAAAAAAAA1\",\"acceptanceDate\":\"12 February 2021\",\"procedureCode\":\"2\",\"declarantDetails\":{\"declarantEORI\":\"GB000000000000001\",\"legalName\":\"Foxpro Central LTD\",\"establishmentAddress\":{\"addressLine1\":\"12 Skybricks Road\",\"addressLine3\":\"Coventry\",\"postalCode\":\"CV3 6EA\",\"countryCode\":\"GB\"},\"contactDetails\":{\"addressLine1\":\"45 Church Road\",\"addressLine3\":\"Leeds\",\"postalCode\":\"LS1 2HA\",\"contactName\":\"Info Tech LTD\",\"countryCode\":\"GB\"}},\"consigneeDetails\":{\"consigneeEORI\":\"GB000000000000001\",\"legalName\":\"IT Solutions LTD\",\"establishmentAddress\":{\"addressLine1\":\"19 Bricks Road\",\"addressLine3\":\"Newcastle\",\"postalCode\":\"NE12 5BT\",\"countryCode\":\"GB\"},\"contactDetails\":{\"addressLine1\":\"11 Mount Road\",\"emailAddress\":\"automation@gmail.com\",\"addressLine3\":\"London\",\"postalCode\":\"E10 7PP\",\"contactName\":\"Online Sales LTD\",\"countryCode\":\"GB\",\"telephone\":\"+4420723934397\"}},\"accountDetails\":[{\"accountType\":\"a\",\"accountNumber\":\"1\",\"eori\":\"GB000000000000001\",\"legalName\":\"a\",\"contactDetails\":{\"addressLine1\":\"a\",\"addressLine4\":\"a\",\"emailAddress\":\"a\",\"addressLine3\":\"a\",\"postalCode\":\"a\",\"contactName\":\"a\",\"countryCode\":\"GB\",\"addressLine2\":\"a\",\"telephone\":\"a\"}},{\"accountType\":\"b\",\"accountNumber\":\"2\",\"eori\":\"GB000000000000001\",\"legalName\":\"b\",\"contactDetails\":{\"addressLine1\":\"b\",\"addressLine4\":\"b\",\"emailAddress\":\"b\",\"addressLine3\":\"b\",\"postalCode\":\"b\",\"contactName\":\"b\",\"countryCode\":\"GB\",\"addressLine2\":\"b\",\"telephone\":\"b\"}}],\"bankDetails\":{\"consigneeBankDetails\":{\"accountHolderName\":\"CDS E2E To E2E Bank\",\"sortCode\":\"308844\",\"accountNumber\":\"12345678\"},\"declarantBankDetails\":{\"accountHolderName\":\"CDS E2E To E2E Bank\",\"sortCode\":\"308844\",\"accountNumber\":\"12345678\"}},\"maskedBankDetails\":{\"consigneeBankDetails\":{\"accountHolderName\":\"CDS E2E To E2E Bank\",\"sortCode\":\"Ending with 44\",\"accountNumber\":\"Ending with 5678\"},\"declarantBankDetails\":{\"accountHolderName\":\"CDS E2E To E2E Bank\",\"sortCode\":\"Ending with 44\",\"accountNumber\":\"Ending with 5678\"}},\"ndrcDetails\":[{\"taxType\":\"A80\",\"amount\":\"218.00\",\"paymentMethod\":\"001\",\"paymentReference\":\"GB201430007000\",\"cmaEligible\":\"0\"},{\"taxType\":\"A95\",\"amount\":\"211.00\",\"paymentMethod\":\"001\",\"paymentReference\":\"GB201430007000\",\"cmaEligible\":\"1\"},{\"taxType\":\"A90\",\"amount\":\"228.00\",\"paymentMethod\":\"001\",\"paymentReference\":\"GB201430007000\",\"cmaEligible\":\"1\"},{\"taxType\":\"A85\",\"amount\":\"171.00\",\"paymentMethod\":\"001\",\"paymentReference\":\"GB201430007000\",\"cmaEligible\":\"1\"}]}}"

  "The C285 claim old and new mappers " should {

    "produce the same TPI05 request for a single claim" in {
      val c285ClaimRequest = Json.fromJson[C285ClaimRequest](Json.parse(legacyC285RequestJson)).get
      val declaration      = Json.fromJson[DisplayDeclaration](Json.parse(delcarationJson)).get

      val overpaymentsSingleRequest: OverpaymentsSingleClaimData = OverpaymentsSingleClaimData(
        Json.fromJson[SingleOverpaymentsClaim](Json.parse(newC285RequestJson)).get,
        declaration,
        None,
        Fake.user
      )
      val tpi05RequestOld = c285ClaimToTPI05Mapper.map(c285ClaimRequest)
      val tpi05RequestNew = overpaymentsSingleClaimToTPI05Mapper.map(overpaymentsSingleRequest)

      tpi05RequestOld.isRight shouldBe tpi05RequestNew.isRight

      val oldResult = Json.toJson(tpi05RequestOld.toOption.value.postNewClaimsRequest.requestDetail)
      val newResult = Json.toJson(tpi05RequestNew.toOption.value.postNewClaimsRequest.requestDetail)

      logger.warn(s"OLD RESULT: $oldResult")
      logger.warn(s"NEW RESULT: $newResult")

      oldResult shouldBe newResult
    }

  }
}
