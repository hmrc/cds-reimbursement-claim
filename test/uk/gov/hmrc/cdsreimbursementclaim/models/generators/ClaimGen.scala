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
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{C285ClaimRequest, ClaimSubmitResponse, DisplayDeclaration, DisplayResponseDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim._

object ClaimGen {

  implicit lazy val displayResponseGen: Typeclass[DisplayResponseDetail]         = gen[DisplayResponseDetail]
  implicit lazy val displayDeclarationGen: Typeclass[DisplayDeclaration]         = gen[DisplayDeclaration]
  implicit lazy val eisRequestBGen: Typeclass[RequestDetailB]                    = gen[RequestDetailB]
  implicit lazy val eisRequestAGen: Typeclass[RequestDetailA]                    = gen[RequestDetailA]
  implicit lazy val eisRequestCommonGen: Typeclass[RequestCommon]                = gen[RequestCommon]
  implicit lazy val eisSubmitClaimResponseGen: Typeclass[EisSubmitClaimResponse] = gen[EisSubmitClaimResponse]
  implicit lazy val eisSubmitClaimRequestGen: Typeclass[EisSubmitClaimRequest]   = gen[EisSubmitClaimRequest]
  implicit lazy val submitClaimResponseGen: Typeclass[ClaimSubmitResponse]       = gen[ClaimSubmitResponse]
  implicit lazy val submitClaimRequestGen: Typeclass[C285ClaimRequest]           = gen[C285ClaimRequest]

}
