package planstack.anml.model.abs

import planstack.anml.model._
import planstack.anml.model.abs.statements._
import planstack.anml.model.abs.time.AbstractTemporalAnnotation
import planstack.anml.model.concrete.{InstanceRef, RefCounter}
import planstack.anml.parser.{FuncExpr, NumExpr, VarExpr}
import planstack.anml.pending.{IntExpression, IntLiteral}
import planstack.anml.{ANMLException, parser}

object StatementsFactory {

  def asStateVariable(expr:parser.Expr, context:AbstractContext, pb:AnmlProblem) = {
    AbstractParameterizedStateVariable(pb, context, expr)
  }

  /**
   * Transforms an annotated statement into its corresponding statement and the temporal constraints that applies
   * to its time-points (derived from the annotation).
    *
    * @param annotatedStatement Statement (with temporal annotations) to translate
   * @param context Context in which the annotated statement appears
   * @param pb Problem in which the statement appears
   * @return An equivalent list of AbstractStatement
   */
  def apply(annotatedStatement : parser.TemporalStatement, context:AbstractContext, pb:AnmlProblem, refCounter: RefCounter) : (Option[AbstractStatement],List[AbstractConstraint]) = {
    val (optStatement, constraints) = StatementsFactory(annotatedStatement.statement, context, pb, refCounter)

    annotatedStatement.annotation match {
      case None =>
        optStatement match {
          case Some(ls :AbstractLogStatement) if !ls.sv.func.isConstant =>
            println("Warning: log statement with no temporal annotation: "+ls)
          case _ =>
        }
        (optStatement, constraints)
      case Some(parsedAnnot) => {
        val annot = AbstractTemporalAnnotation(parsedAnnot)
        assert(optStatement.nonEmpty, "Temporal annotation on something that is not a statement or a task: "+constraints)
        (optStatement, optStatement.get.getTemporalConstraints(annot) ::: constraints)
      }
    }
  }

  def apply(statement : parser.Statement, context:AbstractContext, pb : AnmlProblem, refCounter: RefCounter) : (Option[AbstractStatement], List[AbstractConstraint]) = {
    def asSv(f:EFunction) = new AbstractParameterizedStateVariable(f.func, f.args.map(a => context.getLocalVar(a.name)))
    def asVar(v:EVariable) = context.getLocalVar(v.name)
    def asRef(id:String) = LStatementRef(id)
    context.simplifyStatement(statement, pb) match {

      case ETriStatement(f:EFunction, "==", v1:EVariable, ":->", v2:EVariable, id) =>
        (Some(new AbstractTransition(asSv(f), asVar(v1), asVar(v2), asRef(id))), Nil)
      case EBiStatement(vleft@EVariable(_,_,Some(f)), ":=", value:EVariable, id) =>
        assert(context.hasGlobalVar(asVar(value)), s"$value is not defined yet when assigned to $f")
        assert(context.getGlobalVar(asVar(value)).isInstanceOf[InstanceRef], s"$value is not recognied as a constant symbol when assigned to $f")
        context.bindVarToConstant(asVar(vleft), context.getGlobalVar(asVar(value)).asInstanceOf[InstanceRef])
        (None, List(new AbstractAssignmentConstraint(asSv(f), asVar(value), asRef(id))))
      case EBiStatement(vleft@EVariable(_,_,Some(f)), ":=", ENumber(value), id) =>
        (None, List(new AbstractIntAssignmentConstraint(asSv(f), value, asRef(id))))
      case EBiStatement(vleft:EVariable, "==", vright:EVariable, id) =>
        (None, List(new AbstractVarEqualityConstraint(asVar(vleft), asVar(vright), asRef(id))))
      case EBiStatement(vleft:EVariable, "!=", vright:EVariable, id) =>
        (None, List(new AbstractVarInequalityConstraint(asVar(vleft), asVar(vright), asRef(id))))
      case EBiStatement(f:EFunction, ":=", value:EVariable, id) =>
        assert(!f.func.isConstant)
        (Some(new AbstractAssignment(asSv(f), asVar(value), asRef(id))), Nil)
      case EBiStatement(f:EFunction, "==", value:EVariable, id) =>
        (Some(new AbstractPersistence(asSv(f), asVar(value), asRef(id))), Nil)
      case EBiStatement(f:EFunction, "!=", value:EVariable, id) =>
        val intermediateVar = context.getNewUndefinedVar(f.func.valueType, pb.refCounter)
        (Some(new AbstractPersistence(asSv(f), intermediateVar, asRef(id))),
          List(new AbstractVarInequalityConstraint(intermediateVar, asVar(value), LStatementRef(""))))
      case EUnStatement(ETask(t, args), id) =>
        (Some(new AbstractTask("t-"+t, args.map(arg => asVar(arg)), LActRef(id))), Nil)
      case x => sys.error("Unmatched: "+x)
    }
  }

}
