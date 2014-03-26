package planstack.anml.model.abs

import planstack.anml.parser
import planstack.anml.model.{AbstractContext, AnmlProblem}
import planstack.anml.model.abs.time.AbstractTemporalAnnotation
import planstack.anml.model.abs.statements.{AbstractStatement, AbstractLogStatement}

class AbstractTemporalStatement(val annotation:AbstractTemporalAnnotation, val statement:AbstractStatement) {

  override def toString = "%s %s".format(annotation, statement)
}


object AbstractTemporalStatement {

  def apply(pb:AnmlProblem, context:AbstractContext, ts:parser.TemporalStatement) : AbstractTemporalStatement = {
    new AbstractTemporalStatement(
      AbstractTemporalAnnotation(ts.annotation), AbstractStatement(pb, context, ts.statement)
    )
  }
}