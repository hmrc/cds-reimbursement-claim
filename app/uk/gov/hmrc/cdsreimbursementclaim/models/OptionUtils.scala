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

package uk.gov.hmrc.cdsreimbursementclaim.models

import play.api.libs.json._

object OptionUtils {

  implicit def optionFormat[A](implicit aFormat: Format[A]): Format[Option[A]] =
    new Format[Option[A]] {
      override def reads(json: JsValue): JsResult[Option[A]] =
        (json \ "l")
          .validateOpt[A]

      override def writes(o: Option[A]): JsValue =
        o match {
          case Some(t) ⇒ implicitly[Writes[A]].writes(t)
          case None ⇒ JsNull
        }
    }

}
