package fr.laas.fape.anml.model.ir

import fr.laas.fape.anml.model.abs.AbstractChronicle

class IRUnorderedStatementGroup(parts: List[IRStatementGroup]) extends IRStatementGroup {
  override def firsts: List[IRStatement] = parts.flatMap(_.firsts)
  override def lasts: List[IRStatement] = parts.flatMap(_.lasts)
  override def statements: List[IRStatement] = parts.flatMap(_.statements)
  override def process(f: (IRStatement) => AbstractChronicle): AbstractChronicleGroup =
  new UnorderedChronicleGroup(parts.map(_.process(f)))
}
