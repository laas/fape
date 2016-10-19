package fr.laas.fape.anml.model.ir

import fr.laas.fape.anml.model.abs.{AbstractChronicle, AbstractMinDelay, EmptyAbstractChronicle}
import fr.laas.fape.anml.model.abs.statements.AbstractStatement
import fr.laas.fape.anml.pending.IntExpression

class OrderedChronicleGroup(val parts: List[AbstractChronicleGroup]) extends AbstractChronicleGroup {
  override def firsts: List[AbstractStatement] = parts.head.firsts
  override def lasts: List[AbstractStatement] = parts.last.lasts
  override def statements: List[AbstractStatement] = parts.flatMap(_.statements)
  private def getStructuralTemporalConstraints: List[AbstractMinDelay] = {
    def constraintsBetween(p1:AbstractChronicleGroup, p2:AbstractChronicleGroup) =
      for(e <- p1.lasts ; s <- p2.firsts)
        yield AbstractMinDelay(e.end, s.start, IntExpression.lit(0))
    parts.tail.foldLeft[(AbstractChronicleGroup, List[AbstractMinDelay])]((parts.head, Nil))(
      (acc, cur) => (cur, acc._2 ::: constraintsBetween(acc._1,cur)))._2
  }
  override def chronicle : AbstractChronicle = {
    parts.foldLeft[AbstractChronicle](EmptyAbstractChronicle)((c, group) => c.union(group.chronicle))
      .withConstraintsSeq(getStructuralTemporalConstraints)
  }
}
