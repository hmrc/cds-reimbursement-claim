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

import play.api.libs.json._

import scala.util.Try

object MapFormat {

  val entryPrefix: String = "entry__"

  def apply[K, V](implicit keyFormat: Format[K], valueFormat: Format[V]): Format[Map[K, V]] =
    Format(
      Reads {
        case o: JsObject =>
          Try(
            o.fields.map {
              case (k, o2: JsObject) if k.startsWith(entryPrefix) =>
                (o2 \ "k").as[K] -> (o2 \ "v").as[V]

              case (k, valueJson)                                 =>
                JsString(k).as[K] -> valueJson.as[V]
            }.toMap
          ).fold[JsResult[Map[K, V]]](
            error => JsError(error.toString),
            mapInstance => JsSuccess(mapInstance)
          )

        case json => JsError(s"Expected json object but got ${json.getClass.getSimpleName}")
      },
      Writes.apply { mapInstance =>
        JsObject(
          mapInstance.toSeq.zipWithIndex.map { case ((k, v), i) =>
            keyFormat.writes(k) match {
              // in case key serializes to String, use it as a field key directly
              case JsString(keyString) =>
                keyString -> valueFormat.writes(v)
              // otherwise use intermediate object to handle key and value
              case keyJson             =>
                s"$entryPrefix${i.toString}" -> Json.obj("k" -> keyJson, "v" -> valueFormat.writes(v))
            }
          }
        )
      }
    )
}
