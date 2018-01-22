/*
 * Copyright 2018 Facundo Viale
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

package com.github.jarlakxen.drunk

import io.circe._

object circe {

  implicit class RichTypenameField(typename: String) {
    def decodeAs[T](implicit dec: Decoder[T]): (String, Decoder[T]) = (typename, dec)
  }

  /**
   * This a special [[io.circe.Decoder]] for decode a polymorphic JSON response using the __typename field. 
   */
  def deriveByTypenameDecoder[T](discriminators: (String, Decoder[_ <: T])*) = new Decoder[T] {
    val discriminatorsMap = discriminators.toMap
    override def apply(c: HCursor) = c.downField(ast.TypenameFieldName).as[String] match {
      case Right(tpe) if discriminatorsMap.contains(tpe) =>
        discriminatorsMap(tpe)(c)
      case Right(tpe) =>
        Left(DecodingFailure(s"Cannot deduce decoder $tpe for ${ast.TypenameFieldName}", c.history))
      case _ =>
        Left(DecodingFailure(s"Cannot deduce decoder. Missing ${ast.TypenameFieldName}", c.history))
    }
  }

}