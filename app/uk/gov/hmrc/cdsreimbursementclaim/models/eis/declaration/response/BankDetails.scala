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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{Format, JsPath, Reads, Writes}
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{AccountName, AccountNumber, SortCode}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.declaration.response

final case class BankDetails(
  consigneeBankDetails: Option[BankAccountDetails],
  declarantBankDetails: Option[BankAccountDetails]
)

object BankDetails {

  private val bankAccountDetailsReads: Reads[BankAccountDetails] = (
    (JsPath \ "accountHolderName").read[String].map(AccountName(_)) and
      (JsPath \ "sortCode").read[String].map(SortCode(_)) and
      (JsPath \ "accountNumber").read[String].map(AccountNumber(_))
  )(response.BankAccountDetails(_, _, _))

  private val bankAccountDetailsWrites: Writes[BankAccountDetails] = (
    (JsPath \ "accountHolderName").write[AccountName] and
      (JsPath \ "sortCode").write[SortCode] and
      (JsPath \ "accountNumber").write[AccountNumber]
  )(Tuple.fromProductTyped(_))

  implicit val maskedBankDetailsFormat: Format[BankDetails] =
    Format(
      (
        (JsPath \ "consigneeBankDetails").readNullable[BankAccountDetails](bankAccountDetailsReads) and
          (JsPath \ "declarantBankDetails").readNullable[BankAccountDetails](bankAccountDetailsReads)
      )(BankDetails(_, _)),
      (
        (JsPath \ "consigneeBankDetails").writeNullable[BankAccountDetails](bankAccountDetailsWrites) and
          (JsPath \ "declarantBankDetails").writeNullable[BankAccountDetails](bankAccountDetailsWrites)
      )(Tuple.fromProductTyped(_))
    )
}
