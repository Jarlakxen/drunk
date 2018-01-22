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

import sangria.ast.{ Document, Field, FragmentDefinition, FragmentSpread, OperationDefinition, Selection }

package object ast {

  val TypenameFieldName = "__typename"

  val TypenameField = Field(
    None,
    TypenameFieldName,
    Vector.empty,
    Vector.empty,
    Vector.empty)

  /**
   * This methods walk through all the document and add's the __typename in every sublevel
   */
  def addTypename(doc: Document): Document = {
    val newDefinitions = doc.definitions.map {
      case op: OperationDefinition => addTypename(op)
      case frag: FragmentDefinition => addTypename(frag)
      case other => other
    }

    doc.copy(definitions = newDefinitions)
  }

  private def addTypename(op: OperationDefinition): OperationDefinition = {
    val newSelections = op.selections.map(addTypename)
    op.copy(selections = newSelections)
  }

  private def addTypename(frag: FragmentDefinition): FragmentDefinition = {
    val newSelections = frag.selections.map(addTypename)

    if (newSelections.exists(s => s.isInstanceOf[Field] && s.asInstanceOf[Field].name == TypenameFieldName)) {
      frag.copy(selections = newSelections)
    } else {
      frag.copy(selections = TypenameField +: newSelections)
    }
  }

  private def addTypename(select: Selection): Selection = {
    select match {
      case field: Field if field.selections.nonEmpty => addTypename(field)
      case other => other
    }
  }

  private def addTypename(field: Field): Field = {
    var hasTypename = false
    val newSelections = field.selections.map {
      case field: Field if field.selections.nonEmpty =>
        if (field.name == TypenameFieldName) {
          hasTypename = true
        }
        addTypename(field)
      case frag: FragmentSpread =>
        hasTypename = true
        frag
      case other => other
    }

    if (!hasTypename) {
      field.copy(selections = TypenameField +: newSelections)
    } else {
      field.copy(selections = newSelections)
    }

  }

}