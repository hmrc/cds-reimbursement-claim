/*
 * Copyright 2024 HM Revenue & Customs
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

//import scala.NamedTuple.AnyNamedTuple
//import scala.compiletime.*

trait Lens[R, V] {
  // def get(root: R): V
  def set(root: R, value: V): R
  // def update(root: R, update: V => V): R
}

// object Lens {

//   final class LensMount[R <: Product] extends Selectable {

//     type Fields = NamedTuple.Map[NamedTuple.From[R], [X] =>> LensOf[R, R, X]]

//     transparent inline def selectDynamic(name: String & Singleton) =
//       type X = TypeByName[NamedTuple.From[R], name.type]
//       inline erasedValue[X] match {
//         case _: Unit    => error(s"$name is not a valid property name")
//         case _: Product =>
//           LensMacro.createLens[R, R, X & Product, AdjustableLens[R, R, X & Product]](name, identity[R], (_, r) => r)
//         case _          =>
//           LensMacro.createLens[R, R, X, FixedLens[R, R, X]](name, identity[R], (_, r) => r)
//       }
//   }

//   abstract class AdjustableLens[R, T <: Product, V <: Product] extends Lens[R, V] with Selectable {

//     type Fields = NamedTuple.Map[NamedTuple.From[V], [X] =>> LensOf[R, V, X]]

//     transparent inline def selectDynamic(name: String & Singleton) =
//       type X = TypeByName[NamedTuple.From[V], name.type]
//       inline erasedValue[X] match {
//         case _: Unit    => error(s"$name is not a valid property name")
//         case _: Product =>
//           LensMacro.createLens[R, V, X & Product, AdjustableLens[R, V, X & Product]](name, get, set)
//         case _          =>
//           LensMacro.createLens[R, V, X, FixedLens[R, V, X]](name, get, set)
//       }
//   }

//   abstract class FixedLens[R, T <: Product, V] extends Lens[R, V]

//   abstract class AdjustableOptionalLens[R, T <: Product, V] extends Lens[R, Option[V]]

//   type LensOf[R, V, X] =
//     X match {
//       case Product => AdjustableLens[R, V, X]
//       case _       => FixedLens[R, V, X]
//     }

//   type TypeByName[T <: AnyNamedTuple, Label <: String & Singleton] =
//     Find[Tuple.Zip[NamedTuple.Names[T], NamedTuple.DropNames[T]], Label]

//   type Find[T <: Tuple, Label <: String & Singleton] =
//     T match {
//       case EmptyTuple      => Unit
//       case (Label, v) *: _ => v
//       case _ *: t          => Find[t, Label]
//     }

//   inline def apply[R <: Product]: LensMount[R] = new LensMount[R]

// }
