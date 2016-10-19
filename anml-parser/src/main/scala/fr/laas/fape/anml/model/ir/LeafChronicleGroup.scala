package fr.laas.fape.anml.model.ir

import fr.laas.fape.anml.model.abs.AbstractChronicle
import fr.laas.fape.anml.model.abs.statements.AbstractStatement

class LeafChronicleGroup(val chronicle: AbstractChronicle) extends AbstractChronicleGroup {
  override def firsts: List[AbstractStatement] = chronicle.getStatements
  override def lasts: List[AbstractStatement] = chronicle.getStatements
  override def statements = chronicle.getStatements
}
