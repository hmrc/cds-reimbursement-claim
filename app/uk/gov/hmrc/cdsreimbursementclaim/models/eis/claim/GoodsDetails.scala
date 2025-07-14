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

package uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim

import play.api.libs.json.{Json, OWrites, Reads}
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.enums.YesNo
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.ClaimantType
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.NewEoriAndDan

final case class GoodsDetails(
  descOfGoods: Option[String],
  isPrivateImporter: Option[YesNo] = None,
  placeOfImport: Option[String] = None,
  groundsForRepaymentApplication: Option[String] = None,
  atTheImporterOrDeclarantAddress: Option[String] = None,
  inspectionAddress: Option[InspectionAddress] = None,
  anySpecialCircumstances: Option[String] = None,
  dateOfInspection: Option[String] = None
)

object GoodsDetails {

  final def from(additionalDetails: String, newEoriAndDan: Option[NewEoriAndDan], claimantType: ClaimantType) =
    apply(
      descOfGoods = newEoriAndDan match {
        case None                => Some(additionalDetails)
        case Some(newEoriAndDan) => Some(newEoriAndDan.asAdditionalDetailsText + additionalDetails)
      },
      isPrivateImporter = Some(claimantType match {
        case ClaimantType.Consignee => YesNo.Yes
        case _                      => YesNo.No
      })
    )

  final def apply(
    descOfGoods: Option[String],
    isPrivateImporter: Option[YesNo] = None,
    placeOfImport: Option[String] = None,
    groundsForRepaymentApplication: Option[String] = None,
    atTheImporterOrDeclarantAddress: Option[String] = None,
    inspectionAddress: Option[InspectionAddress] = None,
    anySpecialCircumstances: Option[String] = None,
    dateOfInspection: Option[String] = None
  ): GoodsDetails =
    new GoodsDetails(
      descOfGoods.map(s => s.take(500)),
      isPrivateImporter,
      placeOfImport,
      groundsForRepaymentApplication,
      atTheImporterOrDeclarantAddress,
      inspectionAddress,
      anySpecialCircumstances.map(s => s.take(500)),
      dateOfInspection
    )

  implicit val reads: Reads[GoodsDetails]    = Json.reads[GoodsDetails]
  implicit val writes: OWrites[GoodsDetails] = Json.writes[GoodsDetails]

}
