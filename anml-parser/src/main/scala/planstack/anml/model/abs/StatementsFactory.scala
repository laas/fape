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
  def apply(annotatedStatement : parser.TemporalStatement, context:AbstractContext, refCounter: RefCounter, mod: Mod = DefaultMod) : AbstractChronicle = {
    val  sg = StatementsFactory(annotatedStatement.statement, context, refCounter, mod) //TODO

    annotatedStatement.annotation match {
      case None =>
        assert(sg.statements.collect({ case s:AbstractLogStatement => s}).forall(s => !s.sv.func.isConstant))
        if(sg.statements.exists(_.isInstanceOf[AbstractLogStatement]))
          println("Warning: log statement with no temporal annotation: "+sg.statements)
        EmptyAbstractChronicle.withStatementsSeq(sg.statements).withConstraintsSeq(sg.constraints)
      case Some(parsedAnnot) => {
        val annot = AbstractTemporalAnnotation(parsedAnnot)
        assert(sg.statements.nonEmpty, "Temporal annotation on something that is not a statement or a task: "+sg)
        val newConstraints = sg.getTemporalConstraints(annot)
        EmptyAbstractChronicle.withStatementsSeq(sg.statements).withConstraintsSeq(newConstraints ::: sg.constraints)
      }
    }
  }

  def apply(statement : parser.Statement, context:AbstractContext, refCounter: RefCounter, mod: Mod) : AbsStatementGroup = {
    def asSv(f:EFunction) = new AbstractParameterizedStateVariable(f.func, f.args)
    def asVar(v:EVariable) = context.getLocalVar(v.name)
    def asRef(id:String) = LStatementRef(id)
    val eGroup = context.simplifyStatement(statement, mod)
    val EMPTY = EmptyAbstractChronicle
    def trans(e:EStatement) : AbstractChronicle = e match {
      case ETriStatement(f:ETimedFunction, "==", v1:EVariable, ":->", v2:EVariable, id) =>
        EMPTY.withStatements(new AbstractTransition(asSv(f), asVar(v1), asVar(v2), asRef(id)))

      case EBiStatement(vleft: EConstantFunction, ":=", value:EVariable, id) =>
        assert(context.hasGlobalVar(asVar(value)), s"$value is not defined yet when assigned to $vleft")
        assert(context.getGlobalVar(asVar(value)).isInstanceOf[InstanceRef], s"$value is not recognied as a constant symbol when assigned to $vleft")
        EMPTY.withConstraints(new AbstractAssignmentConstraint(asSv(vleft), asVar(value), asRef(id)))
          .withConstraints(new AbstractVarEqualityConstraint(vleft, value, asRef(id)))

      case EBiStatement(vleft :EConstantFunction, ":=", ENumber(value), id) =>
        EMPTY.withConstraints(new AbstractIntAssignmentConstraint(asSv(vleft), value, asRef(id)))

      case EBiStatement(vleft:EVar, "==", vright:EVar, id) =>
        EMPTY.withConstraints(new AbstractVarEqualityConstraint(vleft, vright, asRef(id)))

      case EBiStatement(vleft:EVar, "!=", vright:EVar, id) =>
        EMPTY.withConstraints(new AbstractVarInequalityConstraint(vleft, vright, asRef(id)))

      case EBiStatement(f:ETimedFunction, ":=", value:EVariable, id) =>
        assert(!f.func.isConstant)
        EMPTY.withStatements(new AbstractAssignment(asSv(f), asVar(value), asRef(id)))

      case EBiStatement(f:ETimedFunction, "==", value:EVariable, id) =>
        EMPTY.withStatements(new AbstractPersistence(asSv(f), asVar(value), asRef(id)))

      case EBiStatement(f:ETimedFunction, "!=", value:EVariable, id) =>
        val intermediateVar = context.getNewUndefinedVar(f.func.valueType, refCounter)
        EMPTY.withStatements(new AbstractPersistence(asSv(f), intermediateVar, asRef(id)))
          .withConstraints(new AbstractVarInequalityConstraint(intermediateVar, asVar(value), LStatementRef("")))

      case EUnStatement(ETask(t, args), id) =>
        EMPTY.withStatements(new AbstractTask("t-"+t, args, LActRef(id)))

      case EBiStatement(v1:EVariable, "in", right@EVarSet(vars), id) =>
        EMPTY.withConstraints(new AbstractInConstraint(v1, vars.map(x => x), asRef(id)))

      case EBiStatement(f:ETimedFunction, "in", right@EVarSet(vars), id) =>
        val intermediateVar = context.getNewUndefinedVar(f.func.valueType, refCounter)
        EMPTY.withStatements(new AbstractPersistence(asSv(f), intermediateVar, asRef(id)))
          .withConstraints(new AbstractInConstraint(intermediateVar, vars.map(x => x), asRef(id)))

      case x => sys.error("Unmatched: "+x)
    }
    eGroup.process(trans)
  }

}
