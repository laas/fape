package planstack.anml.model.abs

import planstack.anml.parser
import planstack.anml.model.{AbstractContext, AnmlProblem}
import planstack.anml.model.abs.time.AbstractTemporalAnnotation

class AbstractTemporalStatement(val annotation:AbstractTemporalAnnotation, val statement:AbstractLogStatement) {

  override def toString = "%s %s".format(annotation, statement)
}


object AbstractTemporalStatement {

  def apply(pb:AnmlProblem, context:AbstractContext, ts:parser.TemporalStatement) : AbstractTemporalStatement = {
    new AbstractTemporalStatement(
      AbstractTemporalAnnotation(ts.annotation), AbstractLogStatement(pb, context, ts.statement)
    )
  }
}