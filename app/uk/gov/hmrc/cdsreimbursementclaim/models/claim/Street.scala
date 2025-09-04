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

package uk.gov.hmrc.cdsreimbursementclaim.models.claim

object Street {

  def fromLines(line1: Option[String], line2: Option[String]): Option[String] =
    (line1, line2)
      .match {
        case (Some(s1), Some(s2)) if s1.trim().endsWith(s2.trim)   => Some(s1)
        case (Some(s1), Some(s2)) if s2.trim().startsWith(s1.trim) => Some(s2)
        case (Some(s1), Some(s2))                                  =>
          if (s1.length() + s2.length() <= 69) Some(s"$s1 $s2")
          else Some(s"$s1$s2")
        case (Some(s1), None)                                      => Some(s1)
        case (None, Some(s2))                                      => Some(s2)
        case _                                                     => Some("")
      }
      .map(_.take(70))

  def line1: (Option[String], Option[String]) => Option[String] = {
    case (Some(s1), Some(s2)) if s1.trim().endsWith(s2.trim) =>
      Some(s1.replace(s2, "").trim())
    case (s1, _)                                             => s1
  }

  def line2: (Option[String], Option[String]) => Option[String] = {
    case (Some(s1), Some(s2)) if s2.trim().startsWith(s1.trim) =>
      Some(s2.replace(s1, "").trim())
    case (_, s2)                                               => s2
  }
}
