package fr.laas.fape.anml.model.ir

import fr.laas.fape.anml.model.abs.AbstractChronicle

class IRSingletonGroup(val statement: IRStatement) extends IRStatementGroup {
  override def firsts: List[IRStatement] = List(statement)
  override def lasts: List[IRStatement] = List(statement)
  override def statements: List[IRStatement] = List(statement)
  override def process(f: (IRStatement) => AbstractChronicle): AbstractChronicleGroup = {
    val ac = f(statement)
    new LeafChronicleGroup(ac)
  }
}
