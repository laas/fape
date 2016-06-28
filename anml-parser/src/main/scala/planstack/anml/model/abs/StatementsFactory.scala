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
  def apply(annotatedStatement : parser.TemporalStatement, context:AbstractContext, refCounter: RefCounter, mod: Mod) : (List[AbstractStatement],List[AbstractConstraint]) = {
    val  sg = StatementsFactory(annotatedStatement.statement, context, refCounter, mod)

    annotatedStatement.annotation match {
      case None =>
        assert(sg.statements.collect({ case s:AbstractLogStatement => s}).forall(s => !s.sv.func.isConstant))
        if(sg.statements.exists(_.isInstanceOf[AbstractLogStatement]))
          println("Warning: log statement with no temporal annotation: "+sg.statements)
        (sg.statements, sg.constraints)
      case Some(parsedAnnot) => {
        val annot = AbstractTemporalAnnotation(parsedAnnot)
        assert(sg.statements.nonEmpty, "Temporal annotation on something that is not a statement or a task: "+sg)
        val newConstraints = sg.getTemporalConstraints(annot)
        (sg.statements, newConstraints ::: sg.constraints)
      }
    }
  }

  def apply(statement : parser.Statement, context:AbstractContext, refCounter: RefCounter, mod: Mod) : AbsStatementGroup = {
    def asSv(f:EFunction) = new AbstractParameterizedStateVariable(f.func, f.args.map(a => context.getLocalVar(a.name)))
    def asVar(v:EVariable) = context.getLocalVar(v.name)
    def asRef(id:String) = LStatementRef(id)
    val eGroup = context.simplifyStatement(statement, mod)
    def trans(e:EStatement) : (List[AbstractStatement], List[AbstractConstraint]) = e match {
      case ETriStatement(f:EFunction, "==", v1:EVariable, ":->", v2:EVariable, id) =>
        (new AbstractTransition(asSv(f), asVar(v1), asVar(v2), asRef(id)) :: Nil,
          Nil)

      case EBiStatement(vleft@EVariable(_,_,Some(f)), ":=", value:EVariable, id) =>
        assert(context.hasGlobalVar(asVar(value)), s"$value is not defined yet when assigned to $f")
        assert(context.getGlobalVar(asVar(value)).isInstanceOf[InstanceRef], s"$value is not recognied as a constant symbol when assigned to $f")
        context.bindVarToConstant(asVar(vleft), context.getGlobalVar(asVar(value)).asInstanceOf[InstanceRef])
        (Nil, List(new AbstractAssignmentConstraint(asSv(f), asVar(value), asRef(id))))

      case EBiStatement(vleft@EVariable(_,_,Some(f)), ":=", ENumber(value), id) =>
        (Nil, List(new AbstractIntAssignmentConstraint(asSv(f), value, asRef(id))))

      case EBiStatement(vleft:EVariable, "==", vright:EVariable, id) =>
        (Nil, List(new AbstractVarEqualityConstraint(asVar(vleft), asVar(vright), asRef(id))))

      case EBiStatement(vleft:EVariable, "!=", vright:EVariable, id) =>
        (Nil, List(new AbstractVarInequalityConstraint(asVar(vleft), asVar(vright), asRef(id))))

      case EBiStatement(f:EFunction, ":=", value:EVariable, id) =>
        assert(!f.func.isConstant)
        (List(new AbstractAssignment(asSv(f), asVar(value), asRef(id))), Nil)

      case EBiStatement(f:EFunction, "==", value:EVariable, id) =>
        (List(new AbstractPersistence(asSv(f), asVar(value), asRef(id))), Nil)

      case EBiStatement(f:EFunction, "!=", value:EVariable, id) =>
        val intermediateVar = context.getNewUndefinedVar(f.func.valueType, refCounter)
        (List(new AbstractPersistence(asSv(f), intermediateVar, asRef(id))),
          List(new AbstractVarInequalityConstraint(intermediateVar, asVar(value), LStatementRef(""))))

      case EUnStatement(ETask(t, args), id) =>
        (List(new AbstractTask("t-"+t, args.map(arg => asVar(arg)), LActRef(id))), Nil)

      case EBiStatement(v1:EVariable, "in", right@ESet(vars), id) =>
        (Nil, List(new AbstractInConstraint(asVar(v1), vars.map(asVar), asRef(id))))

      case EBiStatement(f:EFunction, "in", right@ESet(vars), id) =>
        val intermediateVar = context.getNewUndefinedVar(f.func.valueType, refCounter)
        (List(new AbstractPersistence(asSv(f), intermediateVar, asRef(id))),
          List(new AbstractInConstraint(intermediateVar, vars.map(asVar), asRef(id))))

      case x => sys.error("Unmatched: "+x)
    }
    eGroup.process(trans)
  }

}
