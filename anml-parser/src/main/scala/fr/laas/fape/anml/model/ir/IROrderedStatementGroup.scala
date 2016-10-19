package fr.laas.fape.anml.model.ir

import fr.laas.fape.anml.model.abs.AbstractChronicle

class IROrderedStatementGroup(parts: List[IRStatementGroup]) extends IRStatementGroup {
  override def firsts: List[IRStatement] = parts.head.firsts
  override def lasts: List[IRStatement] = parts.last.lasts
  override def statements: List[IRStatement] = parts.flatMap(_.statements)
  override def process(f: (IRStatement) => AbstractChronicle): AbstractChronicleGroup =
    new OrderedChronicleGroup(parts.map(_.process(f)))
}
