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

import org.scalacheck.magnolia._
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{DisplayDeclaration, DisplayResponseDetail, SubmitClaimRequest, SubmitClaimResponse}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._

object ClaimGen {

  implicit val displayResponseGen: Typeclass[DisplayResponseDetail]         = gen[DisplayResponseDetail]
  implicit val displayDeclarationGen: Typeclass[DisplayDeclaration]         = gen[DisplayDeclaration]
  implicit val eisRequestBGen: Typeclass[RequestDetailB]                    = gen[RequestDetailB]
  implicit val eisRequestAGen: Typeclass[RequestDetailA]                    = gen[RequestDetailA]
  implicit val eisRequestCommonGen: Typeclass[RequestCommon]                = gen[RequestCommon]
  implicit val eisSubmitClaimResponseGen: Typeclass[EisSubmitClaimResponse] = gen[EisSubmitClaimResponse]
  implicit val eisSubmitClaimRequestGen: Typeclass[EisSubmitClaimRequest]   = gen[EisSubmitClaimRequest]
  implicit val submitClaimResponseGen: Typeclass[SubmitClaimResponse]       = gen[SubmitClaimResponse]
  implicit val submitClaimRequestGen: Typeclass[SubmitClaimRequest]         = gen[SubmitClaimRequest]

}
