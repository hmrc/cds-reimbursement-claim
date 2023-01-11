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

package uk.gov.hmrc.cdsreimbursementclaim.utils

import com.eclipsesource.schema.drafts.Version4._
import com.eclipsesource.schema.{SchemaType, SchemaValidator}
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.libs.json.Json

trait SchemaValidation extends TestDataFromFile {
  self: Matchers =>

  final def readSchema(filename: String): SchemaType =
    Json.fromJson[SchemaType](Json.parse(contentOfFile(filename))) match {
      case JsSuccess(schema, _) => schema
      case JsError(errors)      =>
        fail(errors.mkString(s"Cannot parse json schema from $filename :", ", ", ""))
    }

  final def validateRequestBody(schema: SchemaType, body: JsValue): Unit =
    SchemaValidator().validate(schema, body) match {
      case JsSuccess(_, _) => ()
      case JsError(errors) =>
        fail(errors.mkString("Request body has failed json schema validation: ", ", ", ""))
    }
}
