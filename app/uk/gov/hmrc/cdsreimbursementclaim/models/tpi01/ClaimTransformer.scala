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

import cats.implicits.catsSyntaxEq

abstract class ClaimTransformer[T <: CaseDetails, S <: ClaimItem] {
  private val ENTRY_NUMBER = "^[0-9]{9}[a-zA-Z]{1}[0-9]{8}$".r
//  private val MRN          = "^[0-9]{2}[a-zA-Z]{2}[0-9a-zA-Z]{13}[0-9]{1}$".r

  final def convert(
    responseDetail: ResponseDetail,
    mapToCases: CDFPayCase => Option[Iterable[T]]
  ): List[S] = {
    val result = responseDetail.CDFPayCase
      .flatMap(mapToCases)
      .getOrElse(Seq.empty)
      .map(fromTpi01Response)
      .filter((item: ClaimItem) =>
        item.declarationID.exists(id => !ENTRY_NUMBER.pattern.matcher(id).matches()
//            && MRN.pattern.matcher(id).matches()
        )
      )
    removeDuplicates(result, _.CDFPayCaseNumber)
  }

  def fromTpi01Response(caseDetails: T): S

  final def removeDuplicates(list: Iterable[S], get: S => String): List[S] =
    list
      .foldLeft(List.empty[S]) { (acc, elem) =>
        acc.find(item => get(item) === get(elem)) match {
          case Some(_) => acc
          case None    => elem :: acc
        }
      }
      .reverse
}
