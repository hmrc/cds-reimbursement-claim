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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi01.ndrc.NdrcClaimItem
import uk.gov.hmrc.cdsreimbursementclaim.models.tpi01.scty.SctyClaimItem

import java.time.LocalDate
import java.time.format.DateTimeFormatter

final case class ClaimsResponse(sctyClaims: Seq[SctyClaimItem], ndrcClaims: Seq[NdrcClaimItem]) {
  def ++(other: ClaimsResponse): ClaimsResponse =
    ClaimsResponse(this.sctyClaims ++ other.sctyClaims, this.ndrcClaims ++ other.ndrcClaims)
}

object ClaimsResponse {
  implicit val format: OFormat[ClaimsResponse] = Json.format[ClaimsResponse]
  val startDateFormat: DateTimeFormatter       = DateTimeFormatter.ofPattern("yyyyMMdd")
  val earliestDate                             = LocalDate.of(1900, 1, 1)

  def fromTpi01Response(
    responseDetail: ResponseDetail
  ): ClaimsResponse =
    ClaimsResponse(
      sortClaimItems(SctyClaimItem.convert(responseDetail)),
      sortClaimItems(NdrcClaimItem.convert(responseDetail))
    )

  private def sortClaimItems[T <: ClaimItem](claims: Seq[T]): Seq[T] =
    claims.sortWith((x, y) => x.submissionDate.isAfter(y.submissionDate))

}
