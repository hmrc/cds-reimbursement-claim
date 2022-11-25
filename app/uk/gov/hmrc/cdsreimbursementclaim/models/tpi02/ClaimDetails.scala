/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.cdsreimbursementclaim.models.tpi02

object ClaimDetails {

  def transformedCaseStatus(caseStatus: String): String =
    caseStatus match {
      case "Open"                              => "In Progress"
      case "Open-Analysis"                     => "In Progress"
      case "Pending-Approval"                  => "In Progress"
      case "Pending-Queried"                   => "Pending"
      case "Resolved-Withdrawn"                => "Closed"
      case "Rejected-Failed Validation"        => "Closed"
      case "Resolved-Rejected"                 => "Closed"
      case "Open-Rework"                       => "In Progress"
      case "Paused"                            => "In Progress"
      case "Resolved-No Reply"                 => "Closed"
      case "RTBH-Sent"                         => "Pending"
      case "Resolved-Refused"                  => "Closed"
      case "Pending Payment Confirmation"      => "In Progress"
      case "Resolved-Approved"                 => "Closed"
      case "Resolved-Partial Refused"          => "Closed"
      case "Pending Decision Letter"           => "In Progress"
      case "Approved"                          => "In Progress"
      case "Analysis-Rework"                   => "In Progress"
      case "Rework-Payment Details"            => "In Progress"
      case "Pending-RTBH"                      => "In Progress"
      case "Reply To RTBH"                     => "Pending"
      case "Pending-Compliance Recommendation" => "In Progress"
      case "Pending-Compliance Check Query"    => "Pending"
      case "Pending-Compliance Check"          => "In Progress"
    }

  def caseSubStatus(caseStatus: String): Option[String] = caseStatus match {
    case "Resolved-Withdrawn"         => Some("Withdrawn")
    case "Rejected-Failed Validation" => Some("Failed Validation")
    case "Resolved-Rejected"          => Some("Rejected")
    case "Resolved-No Reply"          => Some("No Reply")
    case "Resolved-Refused"           => Some("Refused")
    case "Resolved-Approved"          => Some("Approved")
    case "Resolved-Partial Refused"   => Some("Partial Refused")
    case _                            => None
  }
}
