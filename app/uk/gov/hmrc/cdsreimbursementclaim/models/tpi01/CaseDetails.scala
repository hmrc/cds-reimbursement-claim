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

package uk.gov.hmrc.cdsreimbursementclaim.models.tpi01

trait CaseDetails {
  def CDFPayCaseNumber: String
  def declarationID: Option[String]
  def closedDate: Option[String]
  def caseStatus: String
  def declarantEORI: String
  def claimantEORI: Option[String]
  def totalCustomsClaimAmount: Option[String]
  def totalVATClaimAmount: Option[String]
  def declarantReferenceNumber: Option[String]

  def total: BigDecimal

  final val ZERO: BigDecimal = BigDecimal.apply("0.00")
}
