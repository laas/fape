package fr.laas.fape.anml.model.ir

import fr.laas.fape.anml.model.abs._
import fr.laas.fape.anml.model.abs.statements.AbstractStatement
import fr.laas.fape.anml.parser.{Action => _, Function => _}

class UnorderedChronicleGroup(val parts: List[AbstractChronicleGroup]) extends AbstractChronicleGroup {
  override def firsts: List[AbstractStatement] = parts.flatMap(_.firsts)
  override def lasts: List[AbstractStatement] = parts.flatMap(_.lasts)
  override def statements: List[AbstractStatement] = parts.flatMap(_.statements)
  override def chronicle : AbstractChronicle = {
    parts.foldLeft[AbstractChronicle](EmptyAbstractChronicle)((c, group) => c.union(group.chronicle))
  }
}




