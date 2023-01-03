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

package uk.gov.hmrc.cdsreimbursementclaim.models

object CaseStatus {

  def transformedCaseStatusNdrc(caseStatus: String): String =
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

  def caseSubStatusNdrc(caseStatus: String): Option[String] = caseStatus match {
    case "Resolved-Withdrawn"         => Some("Withdrawn")
    case "Rejected-Failed Validation" => Some("Failed Validation")
    case "Resolved-Rejected"          => Some("Rejected")
    case "Resolved-No Reply"          => Some("No Reply")
    case "Resolved-Refused"           => Some("Refused")
    case "Resolved-Approved"          => Some("Approved")
    case "Resolved-Partial Refused"   => Some("Partial Refused")
    case _                            => None
  }

  def transformedCaseStatusScty(caseStatus: String): String =
    caseStatus match {
      case "Open"                              => "In Progress"
      case "Pending-Approval"                  => "In Progress"
      case "Pending-Payment"                   => "In Progress"
      case "Partial Refund"                    => "Pending"
      case "Resolved-Refund"                   => "Closed"
      case "Pending-Query"                     => "Pending"
      case "Resolved-Manual BTA"               => "Closed"
      case "Pending-C18"                       => "Pending"
      case "Closed-C18 Raised"                 => "Closed"
      case "RTBH Letter Initiated"             => "Pending"
      case "Awaiting RTBH Letter Response"     => "Pending"
      case "Reminder Letter Initiated"         => "Pending"
      case "Awaiting Reminder Letter Response" => "Pending"
      case "Decision Letter Initiated"         => "Pending"
      case "Partial BTA"                       => "Pending"
      case "Partial BTA/Refund"                => "Pending"
      case "Resolved-Auto BTA"                 => "Closed"
      case "Resolved-Manual BTA/Refund"        => "Closed"
      case "Open-Extension Granted"            => "In Progress"
      case "Resolved-Withdrawn"                => "Closed"
    }

  def caseSubStatusScty(caseStatus: String): Option[String] = caseStatus match {
    case "Resolved-Refund"            => Some("Approved")
    case "Resolved-Manual BTA"        => Some("Refused")
    case "Closed-C18 Raised"          => Some("Underpayment")
    case "Resolved-Auto BTA"          => Some("Refused")
    case "Resolved-Manual BTA/Refund" => Some("Part approved")
    case "Resolved-Withdrawn"         => Some("Withdrawn")
    case _                            => None
  }
}
