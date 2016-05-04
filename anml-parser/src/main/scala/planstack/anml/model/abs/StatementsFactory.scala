package planstack.anml.model.abs

import planstack.anml.model._
import planstack.anml.model.abs.statements._
import planstack.anml.model.abs.time.AbstractTemporalAnnotation
import planstack.anml.model.concrete.{InstanceRef, RefCounter}
import planstack.anml.parser.{FuncExpr, NumExpr, VarExpr}
import planstack.anml.pending.{IntExpression, IntLiteral}
import planstack.anml.{ANMLException, parser}

trait Mod {
  def varNameMod(str: String) : String
  def idModifier(id: String) : String
}

object DefaultMod extends Mod {
  private var nextID = 0
  def varNameMod(name: String) = name
  def idModifier(id:String) =
    if(id.isEmpty) "__id__"+{nextID+=1; nextID-1}
    else id
}

object StatementsFactory {

  def asStateVariable(expr:parser.Expr, context:AbstractContext, pb:AnmlProblem) = {
    AbstractParameterizedStateVariable(pb, context, expr)
  }

  /**
   * Transforms an annotated statement into its corresponding statement and the temporal constraints that applies
   * to its time-points (derived from the annotation).
   */
  def apply(annotatedStatement : parser.TemporalStatement, context:AbstractContext, refCounter: RefCounter, mod: Mod) : (Option[AbstractStatement],List[AbstractConstraint]) = {
    val (optStatement, constraints) = StatementsFactory(annotatedStatement.statement, context, refCounter, mod)

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

  def apply(statement : parser.Statement, context:AbstractContext, refCounter: RefCounter, mod: Mod) : (Option[AbstractStatement], List[AbstractConstraint]) = {
    def asSv(f:EFunction) = new AbstractParameterizedStateVariable(f.func, f.args.map(a => context.getLocalVar(a.name)))
    def asVar(v:EVariable) = context.getLocalVar(v.name)
    def asRef(id:String) = LStatementRef(id)
    context.simplifyStatement(statement, mod) match {

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
        val intermediateVar = context.getNewUndefinedVar(f.func.valueType, refCounter)
        (Some(new AbstractPersistence(asSv(f), intermediateVar, asRef(id))),
          List(new AbstractVarInequalityConstraint(intermediateVar, asVar(value), LStatementRef(""))))

      case EUnStatement(ETask(t, args), id) =>
        (Some(new AbstractTask("t-"+t, args.map(arg => asVar(arg)), LActRef(id))), Nil)

      case EBiStatement(v1:EVariable, "in", right@ESet(vars), id) =>
        (None, List(new AbstractInConstraint(asVar(v1), vars.map(asVar), asRef(id))))

      case EBiStatement(f:EFunction, "in", right@ESet(vars), id) =>
        val intermediateVar = context.getNewUndefinedVar(f.func.valueType, refCounter)
        (Some(new AbstractPersistence(asSv(f), intermediateVar, asRef(id))),
          List(new AbstractInConstraint(intermediateVar, vars.map(asVar), asRef(id))))

      case x => sys.error("Unmatched: "+x)
    }
  }

}
