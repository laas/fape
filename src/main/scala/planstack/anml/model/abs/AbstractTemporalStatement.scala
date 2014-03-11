package planstack.anml.model.abs

import planstack.anml.parser
import planstack.anml.model.{AbstractContext, AnmlProblem, TemporalAnnotation}

class AbstractTemporalStatement(val annotation:TemporalAnnotation, val statement:AbstractStatement) {

  override def toString = "%s %s".format(annotation, statement)
}


object AbstractTemporalStatement {

  def apply(pb:AnmlProblem, context:AbstractContext, ts:parser.TemporalStatement) : AbstractTemporalStatement = {
    new AbstractTemporalStatement(
      TemporalAnnotation(ts.annotation), AbstractStatement(pb, context, ts.statement)
    )
  }
}