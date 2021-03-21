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

package uk.gov.hmrc.cdsreimbursementclaim.models.generators

import org.scalacheck.Gen
import org.scalacheck.ScalacheckShapeless._
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{DisplayDeclaration, DisplayResponseDetail, SubmitClaimRequest, SubmitClaimResponse}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._

object ClaimGen extends GenUtils {

  implicit val displayResponseGen: Gen[DisplayResponseDetail]         = gen[DisplayResponseDetail]
  implicit val displayDeclarationGen: Gen[DisplayDeclaration]         = gen[DisplayDeclaration]
  implicit val eisRequestBGen: Gen[RequestDetailB]                    = gen[RequestDetailB]
  implicit val eisRequestAGen: Gen[RequestDetailA]                    = gen[RequestDetailA]
  implicit val eisRequestCommonGen: Gen[RequestCommon]                = gen[RequestCommon]
  implicit val eisSubmitClaimResponseGen: Gen[EisSubmitClaimResponse] = gen[EisSubmitClaimResponse]
  implicit val eisSubmitClaimRequestGen: Gen[EisSubmitClaimRequest]   = gen[EisSubmitClaimRequest]
  implicit val submitClaimResponseGen: Gen[SubmitClaimResponse]       = gen[SubmitClaimResponse]
  implicit val submitClaimRequestGen: Gen[SubmitClaimRequest]         = gen[SubmitClaimRequest]

}
