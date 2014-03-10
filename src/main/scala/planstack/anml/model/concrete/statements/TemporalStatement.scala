package planstack.anml.model.concrete.statements

import planstack.anml.model._

class TemporalStatement(val interval:TemporalAnnotation, val statement:Statement) {

}

object TemporalStatement {

  def apply(context:Context, abs:AbstractTemporalStatement) =
    new TemporalStatement(abs.annotation, abs.statement.bind(context))
}