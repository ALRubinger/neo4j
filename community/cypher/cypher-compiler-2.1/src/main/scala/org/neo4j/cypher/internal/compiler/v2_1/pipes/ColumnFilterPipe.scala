/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compiler.v2_1.pipes

import org.neo4j.cypher.internal.compiler.v2_1._
import commands._
import commands.expressions._
import org.neo4j.cypher.internal.compiler.v2_1.helpers.UnNamedNameGenerator.isNamed
import symbols._
import org.neo4j.cypher.internal.compiler.v2_1.PlanDescription.Arguments

class ColumnFilterPipe(source: Pipe, val returnItems: Seq[ReturnItem])
                      (implicit pipeMonitor: PipeMonitor) extends PipeWithSource(source, pipeMonitor) {
  val returnItemNames: Seq[String] = returnItems.map(_.name)
  val symbols = SymbolTable(identifiers2.toMap)

  private lazy val identifiers2: Seq[(String, CypherType)] = returnItems.
    map( ri => ri.name->ri.expression.getType(source.symbols))

  protected def internalCreateResults(input: Iterator[ExecutionContext], state: QueryState) = {
    input.map(ctx => {
      val newMap = MutableMaps.create(ctx.size)

      returnItems.foreach {
        case ReturnItem(Identifier(oldName), newName) if isNamed(newName) => newMap.put(newName, ctx(oldName))
        case ReturnItem(CachedExpression(oldName, _), newName)            => newMap.put(newName, ctx(oldName))
        case ReturnItem(_, name)                                          => newMap.put(name, ctx(name))
      }

      ctx.newFrom( newMap )
    })
  }

  def planDescription =
    new PlanDescriptionImpl(this, "ColumnFilter", SingleChild(source.planDescription), Seq(Arguments.ColumnsLeft(returnItemNames.toList)))

  def dependencies = Seq()
}
