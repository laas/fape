package fr.laas.fape.anml.model.ir

import fr.laas.fape.anml.ANMLException
import fr.laas.fape.anml.model.abs.{AbstractChronicle, AbstractExactDelay, AbstractMinDelay}
import fr.laas.fape.anml.model.abs.statements.{AbstractAssignment, AbstractStatement, AbstractTransition}
import fr.laas.fape.anml.model.abs.time.AbstractTemporalAnnotation
import fr.laas.fape.anml.pending.IntExpression

class AnnotatedStatementGroup(annot: AbstractTemporalAnnotation, absStatementGroup: AbstractChronicleGroup) extends AbstractChronicleGroup {
  override def firsts: List[AbstractStatement] = absStatementGroup.firsts
  override def lasts: List[AbstractStatement] = absStatementGroup.lasts
  override def statements: List[AbstractStatement] = absStatementGroup.statements

  override def chronicle: AbstractChronicle = absStatementGroup.chronicle.withConstraintsSeq(getTemporalConstraintsFromAnnotation)

  /** Produces the temporal constraints by applying the temporal annotation to this statement. */
  private def getTemporalConstraintsFromAnnotation : List[AbstractMinDelay] = {
    annot match {
      case AbstractTemporalAnnotation(s, e, "is") =>
        assert(firsts.size == 1, s"Cannot apply the temporal annotation $annot on unordered statemers $firsts. " +
          s"Maybe a 'contains' is missing.")
        (firsts flatMap {
          case ass:AbstractAssignment => // assignment is a special case: any annotation is always applied to end timepoint
            assert(s == e, "Non instantaneous assignment.")
            AbstractExactDelay(s.timepoint, ass.end, IntExpression.lit(s.delta)) ++
              AbstractExactDelay(ass.start, ass.end, IntExpression.lit(1))
          case x =>
            AbstractExactDelay(s.timepoint, x.start, IntExpression.lit(s.delta))
        }) ++
          (lasts flatMap { x =>
            AbstractExactDelay(e.timepoint, x.end, IntExpression.lit(e.delta))
          })
      case AbstractTemporalAnnotation(s, e, "contains") =>
        (firsts map {
          case ass:AbstractAssignment =>
            throw new ANMLException("The 'contains' keyword is not allowed on assignments becouse it would introduce disjunctive effects: "+ass)
          case tr:AbstractTransition =>
            throw new ANMLException("The 'contains' keyword is not allowed on transitions becouse it would introduce disjunctive effects: "+tr)
          case x =>
            AbstractMinDelay(s.timepoint, x.start, IntExpression.lit(s.delta)) // start(id) >= start+delta
        }) ++
          (lasts map { x =>
            AbstractMinDelay(x.end, e.timepoint, IntExpression.lit(-e.delta)) // end(id) <= end+delta
          })
    }
  }
}
