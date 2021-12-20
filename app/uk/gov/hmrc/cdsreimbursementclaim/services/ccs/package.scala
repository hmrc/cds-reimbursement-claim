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

package object ccs {

  implicit val ce1779ClaimToDec64FilesMapper: CE1779ClaimToDec64FilesMapper = new CE1779ClaimToDec64FilesMapper
  implicit val c285ClaimToDec64FilesMapper: C285ClaimToDec64FilesMapper     = new C285ClaimToDec64FilesMapper
}
